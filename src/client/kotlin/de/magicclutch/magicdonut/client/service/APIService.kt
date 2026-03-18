package de.magicclutch.magicdonut.client.service

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.regex.Pattern

object APIService {
    val apiTokenPattern: Pattern = Pattern.compile("API (Key|Token is): ([a-z0-9]{32})")

    fun getStats(username: String): JsonObject? {
        val apiUrl = "https://api.donutsmp.net/v1/stats/$username"
        val request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("Authorization", "Bearer ${MagicDonutConfig.instance.developmentConfig.apiKey}")
            .header("Accept", "application/json")
            .GET()
            .build()

        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
        if(response.statusCode() != 200) {
            println("DonutSMP API stats request for user $username failed with status ${response.statusCode()}: ${response.body()}")
            return null
        }

        val responseBody = response.body()
        println("DonutSMP API stats request for user $username successful:\n${responseBody}")
        return Gson().fromJson(responseBody, JsonElement::class.java)?.asJsonObject?.getAsJsonObject("result")
    }
}
