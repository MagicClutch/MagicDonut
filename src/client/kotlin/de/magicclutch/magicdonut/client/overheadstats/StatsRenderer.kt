package de.magicclutch.magicdonut.client.overheadstats

import com.google.gson.JsonObject
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import de.magicclutch.magicdonut.client.service.APIService
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.gui.Font.DisplayMode
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
    private val maxRequestsPerMinute = 100
    private val requestCounter = AtomicInteger(0)
    private var lastCounterReset = System.currentTimeMillis()

    fun init() {
        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            val mc = MinecraftClientService.client
            val camera = mc.gameRenderer.mainCamera
            val matrices = context.matrices()
            val consumers = context.consumers()
            val players = mc.level?.players() ?: return@register
            val tick = mc.deltaTracker.gameTimeDeltaTicks

            // Reset rate limit jede Minute
            val now = System.currentTimeMillis()
            if (now - lastCounterReset >= 60_000) {
                requestCounter.set(0)
                lastCounterReset = now
            }

            // Zähle Spieler auf 0,0 Koordinaten
            val playersOnZero = players.count { it.x.toInt() == 0 && it.z.toInt() == 0 }

            for (player in players) {
                if (player == mc.player) continue
                val name = player.gameProfile.name

                // Skip wenn >2 Spieler auf 0,0 und der Spieler auch dort ist
                if (playersOnZero > 2 && player.x.toInt() == 0 && player.z.toInt() == 0) continue

                val lastUpdate = lastUpdateTimes.getOrDefault(name, 0L)
                val interval = if (players.size < 5) fastUpdateInterval else normalUpdateInterval

                // Rate-Limit: max 100 Anfragen / Minute
                if (now - lastUpdate > interval && requestCounter.get() < maxRequestsPerMinute) {
                    lastUpdateTimes[name] = now
                    requestCounter.incrementAndGet()

                    executor.submit {
                        StatsCache[name].apply {
                            loading = true
                            data = APIService.getStats(name)
                            loading = false
                        }
                    }
                }

                render(player, matrices, consumers, camera.position, tick)
            }
        }
    }


    private fun render(
        player: Player,
        matrices: PoseStack,
        consumers: MultiBufferSource,
        cameraPos: Vec3,
        tickDelta: Float
    ) {
        val mc = MinecraftClientService.client
        val camera = mc.entityRenderDispatcher.camera ?: return
        val font = mc.font

        //like vanilla nametag
        if (player.isCrouching) return
        if (player.isInvisibleTo(mc.player)) return

        val name = player.gameProfile.name
        if (name.length < 3) return

        val entry = StatsCache[name]
        if (entry.data == null && !entry.loading) {
            entry.loading = true
            executor.submit {
                entry.data = APIService.getStats(name)
                entry.loading = false
            }
        }

        val moneyText = extractTextFromData(entry.data)

        val tick = tickDelta

        val x = player.xo + (player.x - player.xo) * tick
        val y = player.yo + (player.y - player.yo) * tick
        val z = player.zo + (player.z - player.zo) * tick

        val renderY = y + player.bbHeight + 0.48

        matrices.pushPose()
        matrices.translate(
            x - cameraPos.x,
            renderY - cameraPos.y,
            z - cameraPos.z
        )


        matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot))
        matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot))
        matrices.scale(-0.025f, -0.025f, 0.025f)

        // ---------- MONEY ONLY ----------
        matrices.pushPose()
        matrices.translate(0.0, 9.0, 0.0)
        matrices.scale(0.5f, 0.5f, 0.5f)

        val moneyWidth = font.width(moneyText) / 2f
        val moneyColor = 0xFF00FC00.toInt()

        font.drawInBatch(
            moneyText,
            -moneyWidth,
            0f,
            moneyColor,
            false,
            matrices.last().pose(),
            consumers,
            DisplayMode.NORMAL,
            0,
            15728880
        )



        matrices.popPose()
        matrices.popPose()
    }

    private fun extractTextFromData(data: JsonObject?): String {
        val money = data?.getAsJsonPrimitive("money")
            ?.runCatching { asFloat }
            ?.getOrDefault(0f)
            ?: return "§7Loading..."

        return "§a${formatMoney(money)}"
    }

    private fun formatMoney(value: Float): String {
        return when {
            value >= 1_000_000_000 -> String.format("%.2fB$", value / 1_000_000_000.0)
            value >= 1_000_000 -> String.format("%.2fM$", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.2fK$", value / 1_000.0)
            else -> String.format("%.2f$", value)
        }
    }
}