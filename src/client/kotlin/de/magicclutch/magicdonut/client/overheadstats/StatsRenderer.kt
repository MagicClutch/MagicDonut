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
import java.util.concurrent.Executors

class StatsRenderer {
    private val executor = Executors.newFixedThreadPool(2)

    fun init() {
        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            val minecraft = MinecraftClientService.client
            val camera = minecraft.gameRenderer.mainCamera
            val matrices = context.matrices()
            val consumers = context.consumers()

            for (player in minecraft.level?.players() ?: return@register) {
                if (player == minecraft.player) continue

                render(player, matrices, consumers, camera.position)
            }
        }
    }

    private fun render(
        player: Player,
        matrices: PoseStack,
        consumers: MultiBufferSource,
        cameraPos: Vec3
    ) {
        val minecraft = MinecraftClientService.client

        val name = player.gameProfile.name
        val entry = StatsCache[name]

        if (entry.data == null && !entry.loading) {
            StatsCache[name].loading = true
            executor.submit {
                val stats = APIService.getStats(name)
                StatsCache[name].data = stats
                StatsCache[name].loading = false
            }
        }

        val text = extractTextFromData(entry.data)

        matrices.pushPose()
        matrices.translate(
            player.x - cameraPos.x,
            player.y - cameraPos.y + player.bbHeight + 0.75,
            player.z - cameraPos.z
        )

        matrices.mulPose(Axis.YP.rotationDegrees(-minecraft.entityRenderDispatcher.camera!!.yRot))
        matrices.mulPose(Axis.XP.rotationDegrees(minecraft.entityRenderDispatcher.camera!!.xRot))

        matrices.scale(-0.025f, -0.025f, 0.025f)

        val textRenderer = minecraft.font
        val width = textRenderer.width(text) / 2

        textRenderer.drawInBatch(
            text,
            (-width).toFloat(),
            0f,
            0xFFFFFFFF.toInt(),
            false,
            matrices.last().pose(),
            consumers,
            DisplayMode.NORMAL,
            0,
            15728880
        )

        matrices.popPose()
    }

    private fun extractTextFromData(data: JsonObject?): String {
        return data?.getAsJsonPrimitive("money")?.asString?.let { "§a$it" } ?: "Loading..."
    }
}