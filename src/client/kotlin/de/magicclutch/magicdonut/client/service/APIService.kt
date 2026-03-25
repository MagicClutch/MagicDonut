package de.magicclutch.magicdonut.client.service

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import net.fabricmc.loader.api.FabricLoader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.util.regex.Pattern

object APIService {
    val apiTokenPattern: Pattern = Pattern.compile("API (Key|Token is): ([a-z0-9]{32})")

    fun hasApiKey(): Boolean =
        getApiKey().isNotBlank()

    fun getConfigPath(): Path =
        FabricLoader.getInstance().configDir.resolve("magicdonut.json5")

    fun getApiKey(): String =
        MagicDonutConfig.instance.developmentConfig.apiKey.trim()

    fun getStats(username: String): JsonObject? {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            println("DonutSMP API stats request skipped: no API key configured (config=${getConfigPath()})")
            return null
        }

        val apiUrl = "https://api.donutsmp.net/v1/stats/$username"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .GET()
            .build()

        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
        if(response.statusCode() != 200) {
            if (response.statusCode() == 401) {
                println("DonutSMP API returned 401 Unauthorized. The configured key is ${maskApiKey(apiKey)} (config=${getConfigPath()})")
            }
            println("DonutSMP API stats request for user $username failed with status ${response.statusCode()}: ${response.body()}")
            return null
        }

        val responseBody = response.body()
        println("DonutSMP API stats request for user $username successful:\n${responseBody}")
        return Gson().fromJson(responseBody, JsonElement::class.java)?.asJsonObject?.getAsJsonObject("result")
    }

    private fun maskApiKey(apiKey: String): String {
        if (apiKey.length <= 8) return "<len=${apiKey.length}>"
        return "${apiKey.take(4)}...${apiKey.takeLast(4)} (len=${apiKey.length})"
    }
}
