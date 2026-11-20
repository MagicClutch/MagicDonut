package de.magicclutch.magicdonut.client.config

import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files

/**
 * Small values the mod remembers between sessions (highest donation, start of the day's balance).
 * They live in their own file so they never show up in the OneConfig settings screen.
 */
object DataStore {
    class Data {
        var highestDonationPlayer: String? = null
        var highestDonationAmount: Double = 0.0
        var profitStartBalance: Double = -1.0
        var profitStartDate: String = ""
    }

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file = FabricLoader.getInstance().configDir.resolve("magicdonututils-data.json")

    val data: Data = load()

    private fun load(): Data = try {
        if (Files.exists(file)) Files.newBufferedReader(file).use { gson.fromJson(it, Data::class.java) } ?: Data() else Data()
    } catch (_: Exception) {
        Data()
    }

    @Synchronized
    fun save() {
        try {
            Files.newBufferedWriter(file).use { gson.toJson(data, it) }
        } catch (_: Exception) {
            // Losing this is harmless: the next donation or balance refresh writes it again.
        }
    }
}
