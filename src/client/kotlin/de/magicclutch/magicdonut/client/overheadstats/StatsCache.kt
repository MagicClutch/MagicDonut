package de.magicclutch.magicdonut.client.overheadstats

import com.google.gson.JsonObject
import java.util.concurrent.ConcurrentHashMap

object StatsCache {
    data class Entry(
        var data: JsonObject? = null,
        var loading: Boolean = false
    )

    private val cache = ConcurrentHashMap<String, Entry>()

    operator fun get(name: String): Entry =
        cache.computeIfAbsent(normalize(name)) { Entry() }

    fun normalize(name: String): String =
        name.trim().lowercase()
}