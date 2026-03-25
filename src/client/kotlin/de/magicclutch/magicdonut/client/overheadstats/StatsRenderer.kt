package de.magicclutch.magicdonut.client.overheadstats

import com.google.gson.JsonElement
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.config.configuration.OverheadStatsConfig.StatField
import de.magicclutch.magicdonut.client.service.APIService
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import de.magicclutch.magicdonut.client.util.PlayerIdentityUtil
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Player
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.max

object StatsRenderer {
    data class RenderLabel(
        val text: String,
        val color: Int
    )

    private const val LOADING_COLOR = 0x808080

    private val executor = Executors.newFixedThreadPool(2)
    private val lastUpdateTimes = ConcurrentHashMap<String, Long>()
    private val requestCounter = AtomicInteger(0)
    private var lastCounterReset = System.currentTimeMillis()

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register {
            if (!MinecraftClientService.isOnDonut) return@register
            val mc = Minecraft.getInstance()
            val level = mc.level ?: return@register
            val cfg = MagicDonutConfig.instance.overheadStatsConfig
            if (!cfg.enabled) return@register

            for (player in level.players()) {
                if (!canTrack(player)) continue
                requestStatsIfNeeded(PlayerIdentityUtil.getCanonicalUsername(player))
            }
        }
    }

    fun requestStatsIfNeeded(name: String?) {
        val normalizedName = name?.takeIf { it.isNotBlank() }?.let(StatsCache::normalize) ?: return
        if (!isApiAvailable()) return

        val cache = StatsCache[normalizedName]
        val refreshMs = MagicDonutConfig.instance.developmentConfig.overheadStatsRefreshRate * 1000L
        val now = System.currentTimeMillis()

        if (now - lastCounterReset >= 60_000) {
            requestCounter.set(0)
            lastCounterReset = now
        }

        val last = lastUpdateTimes.getOrDefault(normalizedName, 0L)
        if (!cache.loading && now - last > refreshMs && requestCounter.get() < 200) {
            lastUpdateTimes[normalizedName] = now
            requestCounter.incrementAndGet()
            cache.loading = true
            executor.submit {
                try {
                    cache.data = APIService.getStats(normalizedName)
                } finally {
                    cache.loading = false
                }
            }
        }
    }

    fun getRenderLabel(name: String?): RenderLabel? {
        val normalizedName = name?.takeIf { it.isNotBlank() }?.let(StatsCache::normalize) ?: return null
        val entry = StatsCache[normalizedName]
        val data = entry.data
        val stat = MagicDonutConfig.instance.overheadStatsConfig.stat.let { configured ->
            when (configured) {
                StatField.MONEY, StatField.DEATHS, StatField.KILLS, StatField.SHARDS, StatField.PLAYTIME -> configured
                else -> StatField.MONEY
            }
        }

        if (data == null) {
            return if (entry.loading || isApiAvailable()) {
                RenderLabel(withIcon(stat, "???"), LOADING_COLOR)
            } else {
                null
            }
        }

        val value = parseDoubleSafe(data.get(stat.apiKey)) ?: 0.0
        val text = when (stat) {
            StatField.PLAYTIME -> formatPlaytime(value)
            else -> {
                val prefix = if (stat.name.lowercase().contains("money")) "$" else ""
                "$prefix${formatNumber(value)}"
            }
        }

        return RenderLabel(withIcon(stat, text), statColor(stat))
    }

    private fun canTrack(player: Player): Boolean {
        val self = MinecraftClientService.client.player
        if (player == self) return false
        if (self != null && player.isInvisibleTo(self)) return false
        if (player.gameProfile.name.length < 3) return false
        return true
    }

    private fun parseDoubleSafe(element: JsonElement?): Double? {
        if (element == null || element.isJsonNull) return null
        if (!element.isJsonPrimitive) return null

        val primitive = element.asJsonPrimitive
        return when {
            primitive.isNumber -> primitive.asDouble
            primitive.isString -> primitive.asString.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
            else -> null
        }
    }

    private fun formatNumber(value: Double): String {
        val absVal = abs(value)
        return when {
            absVal >= 1_000_000_000 -> "%.2fB".format(value / 1_000_000_000.0)
            absVal >= 1_000_000 -> "%.2fM".format(value / 1_000_000.0)
            absVal >= 1_000 -> "%.2fK".format(value / 1_000.0)
            else -> "%.0f".format(value)
        }
    }

    // DonutSMP API returns playtime in milliseconds.
    // Display rules:
    // - days: "Xd Yh"
    // - hours: "Xh Ym"
    // - minutes: "Xm"
    private fun formatPlaytime(value: Double): String {
        val totalSeconds = max(0L, (value / 1000.0).toLong())
        val totalMinutes = totalSeconds / 60L

        val days = totalMinutes / (24L * 60L)
        val hours = (totalMinutes % (24L * 60L)) / 60L
        val minutes = totalMinutes % 60L

        return when {
            days > 0L -> "${days}d ${hours}h"
            hours > 0L -> "${hours}h ${minutes}m"
            else -> "${totalMinutes}m"
        }
    }

    private fun withIcon(stat: StatField, text: String): String =
        when (stat) {
            // Rendered as an amethyst shard bitmap glyph in the renderer itself.
            StatField.SHARDS -> text
            else -> stat.iconGlyph?.takeIf { it.isNotBlank() }?.let { "$it $text" } ?: text
        }

    private fun isApiAvailable(): Boolean =
        APIService.hasApiKey()

    private fun statColor(stat: StatField): Int {
        return when (stat) {
            StatField.MONEY -> 0x55FF55 // green
            StatField.DEATHS -> 0xFFAA00 // orange
            StatField.KILLS -> 0xFF5555 // red
            StatField.SHARDS -> 0xAA00AA // purple
            StatField.PLAYTIME -> 0xFFFF55 // yellow
            else -> 0xFFFFFF
        }
    }
}
