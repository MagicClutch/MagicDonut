package de.magicclutch.magicdonut.client.overheadstats

import com.google.gson.JsonElement
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.config.configuration.OverheadStatsConfig.StatField
import de.magicclutch.magicdonut.client.service.APIService
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Player
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

object StatsRenderer {
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

            val players = level.players()
            for (p in players) {
                if (!canRender(p)) continue
                updateCacheIfNeeded(p.gameProfile.name)
                val label = getFormattedStat(p.gameProfile.name, cfg.stat) ?: continue
                p.setCustomName(label)
                p.isCustomNameVisible = true
            }
        }
    }

    private fun canRender(player: Player): Boolean {
        val self = MinecraftClientService.client.player
        if (player == self) return false
        if (player.isCrouching) return false
        if (self != null && player.isInvisibleTo(self)) return false
        if (player.gameProfile.name.length < 3) return false
        return true
    }

    private fun updateCacheIfNeeded(name: String) {
        val cache = StatsCache[name]
        val refreshMs = MagicDonutConfig.instance.developmentConfig.overheadStatsRefreshRate * 1000L
        val now = System.currentTimeMillis()

        if (now - lastCounterReset >= 60_000) {
            requestCounter.set(0)
            lastCounterReset = now
        }

        val last = lastUpdateTimes.getOrDefault(name, 0L)
        if (!cache.loading && now - last > refreshMs && requestCounter.get() < 200) {
            lastUpdateTimes[name] = now
            requestCounter.incrementAndGet()
            cache.loading = true
            executor.submit {
                try {
                    cache.data = APIService.getStats(name)
                } finally {
                    cache.loading = false
                }
            }
        }
    }

    private fun getFormattedStat(name: String, stat: StatField): net.minecraft.network.chat.Component? {
        val entry = StatsCache[name]
        val data = entry.data
        val text = when {
            data == null && entry.loading -> "§7Loading..."
            data == null -> "§cNo data"
            else -> {
                val value = parseDoubleSafe(data.get(stat.apiKey)) ?: 0.0
                val prefix = if (stat.name.lowercase().contains("money")) "$" else ""
                "§a$prefix${formatNumber(value)}"
            }
        }
        return net.minecraft.network.chat.Component.literal("${name} | $text")
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
}
