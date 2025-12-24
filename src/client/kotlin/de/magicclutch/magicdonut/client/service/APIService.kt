package de.magicclutch.magicdonut.client.service

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

object APIService {
    val apiTokenPattern: Pattern = Pattern.compile("API (Key|Token is): ([a-z0-9]{32})")

    fun getStats(username: String): JsonObject? {
        val apiUrl = "https://api.donutsmp.net/v1/stats/$username"

        val request = HttpGet(apiUrl)
        request.addHeader("Authorization", "Bearer ${MagicDonutConfig.instance.developmentConfig.apiKey}")
        request.addHeader("Accept", "application/json")

        val httpClient = HttpClients.createDefault()

        val response = httpClient.execute(request)
        if(response.statusLine.statusCode != 200) {
            println("DonutSMP API stats request for user $username failed with status ${response.statusLine.statusCode}: ${response.statusLine.reasonPhrase}")
            return null
        }

        val responseBody = String(response.entity.content.readAllBytes(), StandardCharsets.UTF_8)
        println("DonutSMP API stats request for user $username successful:\n${responseBody}")
        return Gson().fromJson(responseBody, JsonElement::class.java)?.asJsonObject?.getAsJsonObject("result")
    }
}