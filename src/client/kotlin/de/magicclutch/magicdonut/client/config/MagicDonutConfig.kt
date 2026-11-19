package de.magicclutch.magicdonut.client.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import de.magicclutch.magicdonut.client.config.configuration.AuctionConfig
import de.magicclutch.magicdonut.client.config.configuration.DevelopmentConfig
import de.magicclutch.magicdonut.client.config.configuration.DonationConfig
import de.magicclutch.magicdonut.client.config.configuration.GambleConfig
import de.magicclutch.magicdonut.client.config.configuration.OverheadStatsConfig
import de.magicclutch.magicdonut.client.config.configuration.PayConfirmConfig
import de.magicclutch.magicdonut.client.config.configuration.ProfitConfig
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * All settings of the mod, stored as one JSON file (`config/magicdonut.json5`). Missing fields keep their defaults,
 * so new settings never break an existing file.
 */
class MagicDonutConfig {
    var developmentConfig: DevelopmentConfig = DevelopmentConfig()
    var payConfirmConfig: PayConfirmConfig = PayConfirmConfig()
    var donationConfig: DonationConfig = DonationConfig()
    var overheadStatsConfig: OverheadStatsConfig = OverheadStatsConfig()
    var auctionConfig: AuctionConfig = AuctionConfig()
    var gambleConfig: GambleConfig = GambleConfig()
    var profitConfig: ProfitConfig = ProfitConfig()

    companion object {
        private val logger = LoggerFactory.getLogger("MagicDonutUtils/Config")
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        private val path: Path
            get() = FabricLoader.getInstance().configDir.resolve("magicdonut.json5")

        @Volatile
        var instance: MagicDonutConfig = load()
            private set

        private fun load(): MagicDonutConfig {
            val file = path
            if (!Files.exists(file)) return MagicDonutConfig().also { write(it) }
            return try {
                Files.newBufferedReader(file).use { reader ->
                    // Lenient: also reads the JSON5 style files written by older versions of the mod.
                    val json = JsonReader(reader).apply { isLenient = true }
                    gson.fromJson<MagicDonutConfig>(json, MagicDonutConfig::class.java) ?: MagicDonutConfig()
                }.also { write(it) } // re-save so new settings show up in the file
            } catch (e: Exception) {
                logger.error("Could not read {}, using defaults (the broken file is kept as .broken)", file, e)
                runCatching { Files.move(file, file.resolveSibling("magicdonut.json5.broken"), StandardCopyOption.REPLACE_EXISTING) }
                MagicDonutConfig()
            }
        }

        /** Re-reads the file, for example after the user edited it by hand. */
        fun reload() {
            instance = load()
        }

        @Synchronized
        fun save() = write(instance)

        @Synchronized
        private fun write(config: MagicDonutConfig) {
            val file = path
            try {
                Files.createDirectories(file.parent)
                val tmp = file.resolveSibling("magicdonut.json5.tmp")
                Files.writeString(tmp, gson.toJson(config))
                try {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING)
                }
            } catch (e: Exception) {
                logger.error("Could not save {}", file, e)
            }
        }
    }
}
