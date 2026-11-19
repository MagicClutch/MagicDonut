package de.magicclutch.magicdonut.client.overheadstats

import com.google.gson.JsonObject
import java.util.concurrent.ConcurrentHashMap

object StatsCache {
    // Written by the API worker threads, read by the render thread, hence @Volatile.
    class Entry {
        @Volatile
        var data: JsonObject? = null

        @Volatile
        var loading: Boolean = false
    }

    private val cache = ConcurrentHashMap<String, Entry>()

    operator fun get(name: String): Entry =
        cache.computeIfAbsent(normalize(name)) { Entry() }

    fun normalize(name: String): String =
        name.trim().lowercase()

    fun clear() = cache.clear()
}
