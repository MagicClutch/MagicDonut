package de.magicclutch.magicdonut.client.service

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.util.PlayerIdentityUtil
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.time.Duration
import java.util.regex.Pattern

object APIService {
    private val logger = LoggerFactory.getLogger("MagicDonutUtils/API")

    private const val STATS_ENDPOINT = "https://api.donutsmp.net/v1/stats/"

    val apiTokenPattern: Pattern = Pattern.compile("API (Key|Token is): ([a-z0-9]{32})")

    // One shared client: HttpClient owns a selector thread and connection pool, so creating one per request leaks resources.
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    fun hasApiKey(): Boolean =
        getApiKey().isNotBlank()

    fun getConfigPath(): Path =
        FabricLoader.getInstance().configDir.resolve("magicdonut.json5")

    fun getApiKey(): String =
        MagicDonutConfig.instance.developmentConfig.apiKey.trim()

    /**
     * Fetches the stats of [username]. Blocking, so only call it off the render/main thread.
     * Returns `null` on any failure; the cause is logged.
     */
    fun getStats(username: String): JsonObject? {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            logger.debug("Stats request skipped: no API key configured (config={})", getConfigPath())
            return null
        }

        val normalizedUsername = PlayerIdentityUtil.normalizeUsernameForApi(username)
        if (normalizedUsername.isNullOrBlank()) {
            logger.debug("Stats request skipped: invalid username '{}'", username)
            return null
        }

        val request = HttpRequest.newBuilder()
            .uri(URI.create(STATS_ENDPOINT + normalizedUsername))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .GET()
            .build()

        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            when (val status = response.statusCode()) {
                200 -> parseResult(response.body())
                401 -> {
                    logger.warn("Stats request rejected with 401 Unauthorized; check the API key {} (config={})", maskApiKey(apiKey), getConfigPath())
                    null
                }
                else -> {
                    logger.debug("Stats request for '{}' failed with status {}", normalizedUsername, status)
                    null
                }
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (e: Exception) {
            logger.debug("Stats request for '{}' failed", normalizedUsername, e)
            null
        }
    }

    private fun parseResult(body: String): JsonObject? {
        val root = JsonParser.parseString(body)
        if (!root.isJsonObject) return null
        val result = root.asJsonObject.get("result")
        return if (result != null && result.isJsonObject) result.asJsonObject else null
    }

    private fun maskApiKey(apiKey: String): String {
        if (apiKey.length <= 8) return "<len=${apiKey.length}>"
        return "${apiKey.take(4)}...${apiKey.takeLast(4)}"
    }
}
