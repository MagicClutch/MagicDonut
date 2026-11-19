package de.magicclutch.magicdonut.client.overheadstats

import com.google.gson.JsonElement
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.config.configuration.OverheadStatsConfig.DisplayMode
import de.magicclutch.magicdonut.client.config.configuration.OverheadStatsConfig.StatField
import de.magicclutch.magicdonut.client.service.APIService
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import de.magicclutch.magicdonut.client.util.PlayerIdentityUtil
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

object StatsRenderer {
    private val allowedStats = listOf(
        StatField.MONEY,
        StatField.DEATHS,
        StatField.KILLS,
        StatField.SHARDS,
        StatField.PLAYTIME
    )

    data class RenderLabel(
        val stat: StatField,
        val text: String,
        val color: Int
    )

    private const val LOADING_COLOR = 0x808080

    // Daemon threads so pending API calls never keep the JVM alive on shutdown.
    private val executor = Executors.newFixedThreadPool(2) { runnable ->
        Thread(runnable, "MagicDonutUtils-Stats").apply { isDaemon = true }
    }
    private val lastUpdateTimes = ConcurrentHashMap<String, Long>()
    private val requestCounter = AtomicInteger(0)
    private var lastCounterReset = System.currentTimeMillis()

    @Volatile
    private var allowedPlayers: Set<String> = emptySet()
    @Volatile
    private var allowedPlayerCount: Int = 0

    private const val MAX_PLAYERS_PER_BLOCK = 3
    private const val EXCLUDED_EPS = 1e-6
    private const val MAX_REQUESTS_PER_MINUTE_MIN = 1
    private const val MAX_REQUESTS_PER_MINUTE_MAX = 250

    private var missingKeyHintShown = false

    private fun showMissingKeyHintOnce() {
        if (missingKeyHintShown) return
        missingKeyHintShown = true
        MinecraftClientService.sendChatMessage(
            Component.literal("[MagicDonutUtils] Overhead stats need a DonutSMP API key (ranked players or approved projects). Everything else works without one.")
                .withStyle(ChatFormatting.YELLOW)
        )
    }

    private fun clearTracking() {
        allowedPlayers = emptySet()
        allowedPlayerCount = 0
    }

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register {
            val level = Minecraft.getInstance().level
            if (level == null) {
                // Left the world: drop everything so the caches don't grow across sessions.
                clearTracking()
                missingKeyHintShown = false
                StatsCache.clear()
                lastUpdateTimes.clear()
                return@register
            }
            if (!MinecraftClientService.isOnDonut || !MagicDonutConfig.instance.overheadStatsConfig.enabled) {
                clearTracking()
                return@register
            }
            if (!isApiAvailable()) {
                clearTracking()
                showMissingKeyHintOnce()
                return@register
            }

            // Build a snapshot of players we allow for overhead stats this tick.
            // Rules:
            // - If >3 players occupy the same block: exclude all players on that block.
            // - Exclude players at exact X/Z coords: (0,0) and (-0.5, 0.5).
            val tracked = level.players()
                .asSequence()
                .filter { canTrack(it) }
                .filterNot { isExcludedXZ(it) }
                .mapNotNull { p -> PlayerIdentityUtil.getCanonicalUsername(p)?.let { StatsCache.normalize(it) }?.let { it to p.blockPosition() } }
                .toList()

            val perBlock = HashMap<BlockPos, Int>(tracked.size)
            for ((_, pos) in tracked) {
                perBlock[pos] = (perBlock[pos] ?: 0) + 1
            }

            val overcrowded = perBlock.asSequence()
                .filter { it.value > MAX_PLAYERS_PER_BLOCK }
                .map { it.key }
                .toHashSet()

            val allowed = tracked.asSequence()
                .filterNot { (_, pos) -> overcrowded.contains(pos) }
                .map { it.first }
                .toHashSet()

            allowedPlayers = allowed
            allowedPlayerCount = allowed.size

            for (name in allowed) {
                requestStatsIfNeeded(name)
            }
        }
    }

    private fun getMaxRequestsPerMinute(): Int {
        return MagicDonutConfig.instance.overheadStatsConfig.maxRequestsPerMinute
            .coerceIn(MAX_REQUESTS_PER_MINUTE_MIN, MAX_REQUESTS_PER_MINUTE_MAX)
    }

    private fun computeAutoRefreshMs(validPlayerCount: Int): Long {
        if (validPlayerCount <= 0) return Long.MAX_VALUE

        // Goal:
        // - Low players => high refresh (up to 60 requests/min per player => 1s interval)
        // - More players => lower refresh while staying within global cap.
        // Using the global cap we enforce via requestCounter (<maxRequestsPerMinute/min).
        val maxRequestsPerMinute = getMaxRequestsPerMinute()
        val perPlayerRpm = minOf(60, maxRequestsPerMinute / validPlayerCount) // floor division
        if (perPlayerRpm <= 0) {
            // Fractional RPM per player, avoid division by zero.
            return ceil((60_000.0 * validPlayerCount) / maxRequestsPerMinute.toDouble()).toLong().coerceAtLeast(1L)
        }
        return ceil(60_000.0 / perPlayerRpm.toDouble()).toLong().coerceAtLeast(1L)
    }

    fun requestStatsIfNeeded(name: String?) {
        val normalizedName = name?.takeIf { it.isNotBlank() }?.let(StatsCache::normalize) ?: return
        // If we know this player is excluded this tick, skip API checks entirely.
        if (allowedPlayers.isNotEmpty() && !allowedPlayers.contains(normalizedName)) return
        if (!isApiAvailable()) return

        val cache = StatsCache[normalizedName]
        val refreshMs = computeAutoRefreshMs(allowedPlayerCount)
        val now = System.currentTimeMillis()

        if (now - lastCounterReset >= 60_000) {
            requestCounter.set(0)
            lastCounterReset = now
        }

        val last = lastUpdateTimes.getOrDefault(normalizedName, 0L)
        if (!cache.loading && now - last > refreshMs && requestCounter.get() < getMaxRequestsPerMinute()) {
            lastUpdateTimes[normalizedName] = now
            requestCounter.incrementAndGet()
            cache.loading = true
            executor.submit {
                try {
                    // Keep the previous value when a refresh fails instead of flashing back to "???".
                    APIService.getStats(normalizedName)?.let { cache.data = it }
                } finally {
                    cache.loading = false
                }
            }
        }
    }

    fun getRenderLabel(name: String?): RenderLabel? {
        val normalizedName = name?.takeIf { it.isNotBlank() }?.let(StatsCache::normalize) ?: return null
        // Hard render exclusion: don't show overhead stats for excluded players.
        if (allowedPlayers.isNotEmpty() && !allowedPlayers.contains(normalizedName)) return null
        val entry = StatsCache[normalizedName]
        val data = entry.data
        val stat = currentStat()

        if (data == null) {
            return if (entry.loading || isApiAvailable()) {
                RenderLabel(stat, withIcon(stat, "???"), LOADING_COLOR)
            } else {
                null
            }
        }

        val value = parseDoubleSafe(data.get(stat.apiKey)) ?: 0.0
        val text = when (stat) {
            StatField.PLAYTIME -> formatPlaytime(value)
            else -> {
                val prefix = if (stat == StatField.MONEY) "$" else ""
                "$prefix${formatNumber(value)}"
            }
        }

        return RenderLabel(stat, withIcon(stat, text), statColor(stat))
    }

    private fun canTrack(player: Player): Boolean {
        val self = MinecraftClientService.client.player
        if (player == self) return false
        if (self != null && player.isInvisibleTo(self)) return false
        if (player.gameProfile.name.length < 3) return false
        return true
    }

    private fun isExcludedXZ(player: Player): Boolean {
        val x = player.x
        val z = player.z
        val atZero = abs(x - 0.0) <= EXCLUDED_EPS && abs(z - 0.0) <= EXCLUDED_EPS
        val atNegHalf = abs(x - (-0.5)) <= EXCLUDED_EPS && abs(z - 0.5) <= EXCLUDED_EPS
        return atZero || atNegHalf
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
            absVal >= 1_000_000_000 -> "%.2fB".format(java.util.Locale.ROOT, value / 1_000_000_000.0)
            absVal >= 1_000_000 -> "%.2fM".format(java.util.Locale.ROOT, value / 1_000_000.0)
            absVal >= 1_000 -> "%.2fK".format(java.util.Locale.ROOT, value / 1_000.0)
            else -> "%.0f".format(java.util.Locale.ROOT, value)
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

    private fun currentStat(): StatField {
        val cfg = MagicDonutConfig.instance.overheadStatsConfig
        val fallback = StatField.MONEY

        val fixed = cfg.stat.takeIf { it in allowedStats } ?: fallback
        if (cfg.displayMode != DisplayMode.CYCLE) {
            return fixed
        }

        val seconds = cfg.cycleSeconds.coerceIn(1, 30)
        val idx = ((System.currentTimeMillis() / (seconds * 1000L)) % allowedStats.size).toInt()
        return allowedStats.getOrElse(idx) { fixed }
    }

    private fun statColor(stat: StatField): Int {
        return when (stat) {
            StatField.MONEY -> 0x55FF55 // green
            StatField.DEATHS -> 0xFFAA00 // orange
            StatField.KILLS -> 0xFF5555 // red
            StatField.SHARDS -> 0xAA00AA // purple
            StatField.PLAYTIME -> 0xFFFF55 // yellow
        }
    }
}
