# MagicDonutUtils stats proxy

A tiny Cloudflare Worker that lets the mod read DonutSMP stats **without any API key inside the mod**.
The key lives only on the Worker as a secret, so it never reaches a player's machine.

## How it works

1. The mod asks `GET /v1/challenge` for a signed, 60-second nonce.
2. The mod calls Mojang's `joinServer` with that nonce (the same mechanism Minecraft uses to log into servers).
3. The mod sends `POST /v1/auth {username, challenge}`. The Worker checks Mojang's `hasJoined`, which proves the
   player really owns that account, and returns a signed token valid for 30 minutes.
4. The mod calls `GET /v1/stats/{user}` with that token. The Worker adds the real `Authorization` header itself.

Protections: endpoint whitelist (only the three routes above), username allowlist, per-account and global rate
limits, a 20 s response cache, a ban list, and upstream auth errors are never forwarded to clients.

## Setup

```sh
cd proxy
npx wrangler secret put DONUT_API_KEY      # your approved DonutSMP API key
npx wrangler secret put SESSION_SECRET     # e.g. the output of: openssl rand -hex 32
npx wrangler deploy
```

Then, in the mod's config (Dev tab) or `magicdonut.json5`, set `apiBaseUrl` to
`https://magicdonut-proxy.<your-subdomain>.workers.dev/v1` and leave the API key empty.

## Operating it

- **Rotate the key:** `wrangler secret put DONUT_API_KEY` again. Players notice nothing.
- **Ban an account:** `wrangler secret put BANNED_UUIDS` with a comma separated list of uuids (no dashes).
- **Tune limits:** edit `limit` in `wrangler.toml`, keep `GLOBAL_LIMITER` under your key's own limit.
- Only run this with a key you are allowed to use this way. Check Donut's API rules before deploying.
