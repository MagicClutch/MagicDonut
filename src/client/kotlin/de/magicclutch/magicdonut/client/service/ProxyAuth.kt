package de.magicclutch.magicdonut.client.service

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Logs into the stats proxy with the player's own Minecraft account.
 *
 * The proxy hands out a short-lived nonce, we "join" it with Mojang's session server (the same call the
 * game makes when logging into a server), and the proxy verifies that with `hasJoined`. No API key and no
 * access token ever leaves the machine except to Mojang, which is who it belongs to.
 */
class ProxyAuth(private val http: HttpClient) {
    private val logger = LoggerFactory.getLogger("MagicDonutUtils/ProxyAuth")

    private var token: String? = null
    private var tokenExpiresAtMs = 0L
    private var retryAfterMs = 0L

    /** A valid proxy token, logging in first if needed. Blocking; call it off the main thread. */
    @Synchronized
    fun getToken(baseUrl: String, client: net.minecraft.client.Minecraft): String? {
        val now = System.currentTimeMillis()
        token?.takeIf { now < tokenExpiresAtMs - REFRESH_MARGIN_MS }?.let { return it }
        if (now < retryAfterMs) return null

        val fresh = try {
            login(baseUrl, client)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (e: Exception) {
            logger.debug("Proxy login failed", e)
            null
        }

        if (fresh == null) {
            // Don't hammer the proxy or Mojang when something is wrong.
            retryAfterMs = now + FAILURE_BACKOFF_MS
            return null
        }
        token = fresh.first
        tokenExpiresAtMs = now + fresh.second * 1000L
        return fresh.first
    }

    @Synchronized
    fun invalidate() {
        token = null
        tokenExpiresAtMs = 0L
    }

    private fun login(baseUrl: String, client: net.minecraft.client.Minecraft): Pair<String, Long>? {
        val challenge = get("$baseUrl/challenge")?.get("challenge")?.asString ?: return null

        val user = client.user
        client.services().sessionService().joinServer(user.profileId, user.accessToken, challenge)

        val body = JsonObject().apply {
            addProperty("username", user.name)
            addProperty("challenge", challenge)
        }
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/auth"))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) return null

        val json = JsonParser.parseString(response.body()).asJsonObject
        val newToken = json.get("token")?.asString ?: return null
        val ttl = json.get("expiresIn")?.asLong ?: 600L
        return newToken to ttl
    }

    private fun get(url: String): JsonObject? {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) return null
        return JsonParser.parseString(response.body()).asJsonObject
    }

    private companion object {
        const val REFRESH_MARGIN_MS = 60_000L
        const val FAILURE_BACKOFF_MS = 30_000L
    }
}
