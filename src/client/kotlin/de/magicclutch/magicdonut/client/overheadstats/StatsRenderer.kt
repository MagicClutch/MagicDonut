package de.magicclutch.magicdonut.client.overheadstats


import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import de.magicclutch.magicdonut.client.service.APIService
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.gui.Font.DisplayMode
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class StatsRenderer {
    private val executor = Executors.newFixedThreadPool(2)

    private val lastUpdateTimes = ConcurrentHashMap<String, Long>()
    private val normalUpdateInterval = 10_000L // 10s
    private val fastUpdateInterval = 2_000L // 2s
    private val maxRequestsPerMinute = 200
    private val requestCounter = AtomicInteger(0)
    private var lastCounterReset = System.currentTimeMillis()

    fun init() {
        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            if(!MinecraftClientService.isOnDonut) return@register

            val minecraft = MinecraftClientService.client
            val camera = minecraft.gameRenderer.mainCamera
            val players = minecraft.level?.players() ?: return@register
            val tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true)

            for (player in players) {
                handlePlayer(player, context.matrixStack()!!, context.consumers()!!, camera.position, tickDelta, players)
            }
        }
    }

    private fun handlePlayer(
        player: Player,
        matrices: PoseStack,
        consumers: MultiBufferSource,
        cameraPos: Vec3,
        tickDelta: Float,
        players: List<AbstractClientPlayer>
    ) {
        val name = player.gameProfile.name

        if(!checkRenderAllowed(player, players)) return

        checkApiCache(player, players)

        val text = extractTextFromData(name)

        renderStatsDisplay(
            player,
            matrices,
            consumers,
            cameraPos,
            tickDelta,
            text
        )
    }

    private fun checkRenderAllowed(player: Player, players: List<AbstractClientPlayer>): Boolean {
        val minecraft = MinecraftClientService.client
        if (player == minecraft.player) return false
        if (player.isCrouching) return false
        if (player.isInvisibleTo(minecraft.player!!)) return false

        val name = player.gameProfile.name
        if (name.length < 3) return false

        val playersOnZero = players.count { it.x.toInt() == 0 && it.z.toInt() == 0 }
        if (playersOnZero > 2 && player.x.toInt() == 0 && player.z.toInt() == 0) return false

        return true
    }

    private fun checkApiCache(player: Player, players: List<AbstractClientPlayer>) {
        val name = player.gameProfile.name
        val cacheEntry = StatsCache[name]

        val lastUpdate = lastUpdateTimes.getOrDefault(name, 0L)
        val interval = if (players.size < 5) fastUpdateInterval else normalUpdateInterval

        val now = System.currentTimeMillis()
        if (now - lastCounterReset >= 60_000) {
            requestCounter.set(0)
            lastCounterReset = now
        }

        if (!cacheEntry.loading && now - lastUpdate > interval && requestCounter.get() < maxRequestsPerMinute) {
            lastUpdateTimes[name] = now
            requestCounter.incrementAndGet()

            executor.submit {
                cacheEntry.loading = true
                try {
                    val newData = APIService.getStats(name)
                    if (newData != null) {
                        cacheEntry.data = newData
                    }
                } finally {
                    cacheEntry.loading = false
                }
            }
        }
    }

    private fun renderStatsDisplay(
        player: Player,
        matrices: PoseStack,
        consumers: MultiBufferSource,
        cameraPos: Vec3,
        tickDelta: Float,
        text: String
    ) {
        val minecraft = MinecraftClientService.client
        val camera = minecraft.entityRenderDispatcher.camera ?: return
        val font = minecraft.font

        val x = player.xOld + (player.x - player.xOld) * tickDelta
        val y = player.yOld + (player.y - player.yOld) * tickDelta
        val z = player.zOld + (player.z - player.zOld) * tickDelta

        val renderY = y + player.bbHeight + 0.52

        matrices.pushPose()
        matrices.translate(x - cameraPos.x, renderY - cameraPos.y, z - cameraPos.z)
        matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot))
        matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot))
        matrices.scale(-0.025f, -0.025f, 0.025f)

        matrices.pushPose()
        matrices.translate(0.0, 9.0, 0.0)
        matrices.scale(0.5f, 0.5f, 0.5f)

        val width = font.width(text) / 2f
        val color = 0xFF00FC00.toInt()

        font.drawInBatch(
            text, -width, 0f, color, false,
            matrices.last().pose(), consumers, DisplayMode.NORMAL, 0, 15728880
        )

        matrices.popPose()
        matrices.popPose()
    }

    private fun extractTextFromData(name: String): String {
        val entry = StatsCache[name]
        val data = entry.data

        if (data == null && entry.loading) return "§7Loading..."


        if (data == null) return "§cNo Data"

        val money = data.getAsJsonPrimitive("money")
            ?.runCatching { asFloat }
            ?.getOrDefault(0f) ?: 0f

        return "§a${formatMoney(money)}"
    }

companion object{
    fun formatMoney(value: Float): String {
        return when {
            value >= 1_000_000_000 -> String.format("%.2fB$", value / 1_000_000_000.0)
            value >= 1_000_000 -> String.format("%.2fM$", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.2fK$", value / 1_000.0)
            else -> String.format("%.2f$", value)
        }
    }
}
}