// MagicDonutUtils stats proxy (Cloudflare Worker).
//
// Keeps the DonutSMP API key on the server. Players prove they own a real Minecraft account via
// Mojang's session server, receive a short-lived signed token, and can then read stats through the
// proxy. See proxy/README.md for setup.
//
// Secrets (wrangler secret put):  DONUT_API_KEY, SESSION_SECRET
// Rate limit bindings (wrangler.toml): USER_LIMITER (per account), GLOBAL_LIMITER (everyone)

const UPSTREAM = "https://api.donutsmp.net/v1";
const MOJANG_HAS_JOINED = "https://sessionserver.mojang.com/session/minecraft/hasJoined";

const CHALLENGE_MAX_AGE_S = 60;
const TOKEN_TTL_S = 30 * 60;
const STATS_CACHE_S = 20;

// Same allowlist the mod uses: letters, digits, '_' and '.' (Bedrock names start with a dot).
const USERNAME_RE = /^[A-Za-z0-9_.]{1,20}$/;
const MC_NAME_RE = /^[A-Za-z0-9_]{1,16}$/;

const encoder = new TextEncoder();

export default {
  async fetch(request, env) {
    try {
      return await route(request, env);
    } catch (err) {
      console.error("unhandled", err);
      return json({ error: "internal_error" }, 500);
    }
  },
};

async function route(request, env) {
  if (!env.DONUT_API_KEY || !env.SESSION_SECRET) return json({ error: "not_configured" }, 503);

  const url = new URL(request.url);
  const path = url.pathname.replace(/\/+$/, "");

  if (request.method === "GET" && path === "/v1/challenge") return challenge(env);
  if (request.method === "POST" && path === "/v1/auth") return auth(request, env);

  const statsMatch = path.match(/^\/v1\/stats\/([^/]+)$/);
  if (request.method === "GET" && statsMatch) return stats(request, env, decodeURIComponent(statsMatch[1]));

  // Everything else is intentionally not forwarded.
  return json({ error: "not_found" }, 404);
}

// ---- 1) challenge: a stateless, signed, short-lived nonce used as the Mojang "serverId" ----------

async function challenge(env) {
  const ts = Math.floor(Date.now() / 1000).toString(16).padStart(8, "0");
  const rand = randomHex(8);
  const sig = (await hmacHex(env.SESSION_SECRET, `c.${ts}.${rand}`)).slice(0, 16);
  return json({ challenge: `${ts}${rand}${sig}` });
}

async function verifyChallenge(env, value) {
  if (typeof value !== "string" || !/^[0-9a-f]{40}$/.test(value)) return false;
  const ts = value.slice(0, 8);
  const rand = value.slice(8, 24);
  const sig = value.slice(24);
  const expected = (await hmacHex(env.SESSION_SECRET, `c.${ts}.${rand}`)).slice(0, 16);
  if (!timingSafeEqual(sig, expected)) return false;
  const age = Math.floor(Date.now() / 1000) - parseInt(ts, 16);
  return age >= 0 && age <= CHALLENGE_MAX_AGE_S;
}

// ---- 2) auth: verify the account with Mojang, hand out a signed token ----------------------------

async function auth(request, env) {
  let body;
  try {
    body = await request.json();
  } catch {
    return json({ error: "bad_request" }, 400);
  }
  const username = body && body.username;
  const challengeValue = body && body.challenge;
  if (typeof username !== "string" || !MC_NAME_RE.test(username)) return json({ error: "bad_request" }, 400);
  if (!(await verifyChallenge(env, challengeValue))) return json({ error: "bad_challenge" }, 400);

  // Slow down brute forcing of the auth endpoint itself.
  const ip = request.headers.get("CF-Connecting-IP") || "unknown";
  if (!(await allow(env.USER_LIMITER, `auth:${ip}`))) return json({ error: "rate_limited" }, 429);

  const check = await fetch(
    `${MOJANG_HAS_JOINED}?username=${encodeURIComponent(username)}&serverId=${challengeValue}`,
  );
  if (check.status !== 200) return json({ error: "not_verified" }, 401);
  const profile = await check.json();
  if (!profile || typeof profile.id !== "string" || !/^[0-9a-f]{32}$/i.test(profile.id)) {
    return json({ error: "not_verified" }, 401);
  }

  const exp = Math.floor(Date.now() / 1000) + TOKEN_TTL_S;
  const uuid = profile.id.toLowerCase();
  const sig = await hmacHex(env.SESSION_SECRET, `t.${uuid}.${exp}`);
  return json({ token: `${uuid}.${exp}.${sig}`, expiresIn: TOKEN_TTL_S });
}

async function verifyToken(env, request) {
  const header = request.headers.get("Authorization") || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : "";
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  const [uuid, exp, sig] = parts;
  if (!/^[0-9a-f]{32}$/.test(uuid) || !/^\d{1,12}$/.test(exp)) return null;
  const expected = await hmacHex(env.SESSION_SECRET, `t.${uuid}.${exp}`);
  if (!timingSafeEqual(sig, expected)) return null;
  if (parseInt(exp, 10) < Math.floor(Date.now() / 1000)) return null;
  return uuid;
}

// ---- 3) stats: whitelisted, rate limited, cached ------------------------------------------------

async function stats(request, env, user) {
  const uuid = await verifyToken(env, request);
  if (!uuid) return json({ error: "unauthorized" }, 401);

  // Ban switch: BANNED_UUIDS is a comma separated list of uuids (no dashes).
  if ((env.BANNED_UUIDS || "").toLowerCase().split(",").map((s) => s.trim()).includes(uuid)) {
    return json({ error: "forbidden" }, 403);
  }

  if (!USERNAME_RE.test(user)) return json({ error: "bad_username" }, 400);
  const name = user.toLowerCase();

  // Cache first: many players looking at the same people become one upstream call.
  const cache = caches.default;
  const cacheKey = new Request(`https://cache.internal/stats/${encodeURIComponent(name)}`);
  const cached = await cache.match(cacheKey);
  if (cached) return cached;

  if (!(await allow(env.USER_LIMITER, `user:${uuid}`))) return json({ error: "rate_limited" }, 429);
  // Keeps total traffic below Donut's limit so one abuser can't burn the key.
  if (!(await allow(env.GLOBAL_LIMITER, "global"))) return json({ error: "busy" }, 503);

  const upstream = await fetch(`${UPSTREAM}/stats/${encodeURIComponent(name)}`, {
    headers: { Authorization: `Bearer ${env.DONUT_API_KEY}`, Accept: "application/json" },
  });

  // Never leak upstream auth problems to clients.
  if (upstream.status === 401 || upstream.status === 403) {
    console.error("upstream rejected the API key");
    return json({ error: "upstream_unavailable" }, 502);
  }
  if (upstream.status !== 200) return json({ error: "upstream_error" }, upstream.status === 500 ? 404 : 502);

  const response = new Response(upstream.body, {
    status: 200,
    headers: {
      "Content-Type": "application/json",
      "Cache-Control": `public, max-age=${STATS_CACHE_S}`,
    },
  });
  await cache.put(cacheKey, response.clone());
  return response;
}

// ---- helpers ------------------------------------------------------------------------------------

async function allow(limiter, key) {
  if (!limiter) return true;
  const { success } = await limiter.limit({ key });
  return success;
}

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function randomHex(bytes) {
  const buf = new Uint8Array(bytes);
  crypto.getRandomValues(buf);
  return [...buf].map((b) => b.toString(16).padStart(2, "0")).join("");
}

async function hmacHex(secret, message) {
  const key = await crypto.subtle.importKey("raw", encoder.encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  const sig = await crypto.subtle.sign("HMAC", key, encoder.encode(message));
  return [...new Uint8Array(sig)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

function timingSafeEqual(a, b) {
  if (typeof a !== "string" || typeof b !== "string" || a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}
