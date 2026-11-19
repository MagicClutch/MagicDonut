package de.magicclutch.magicdonut.client.auction

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.util.AmountParser
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import kotlin.math.sin

/**
 * The auction hologram: the item floats and slowly spins in the world in front of where the auction was started.
 * Above it, from the top: the time left, the item name and one subtitle (top bid, or the minimum bid).
 * Only you see it; it is drawn client-side.
 */
object AuctionHologram {
    private const val FULL_BRIGHT = 15728880

    private val itemState = ItemStackRenderState()

    fun register() {
        LevelRenderEvents.COLLECT_SUBMITS.register { render(it) }
    }

    private fun render(context: LevelRenderContext) {
        val config = MagicDonutConfig.instance.auctionConfig
        if (!config.enabled) return

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        if (mc.level == null) return

        val running = AuctionManager.current
        val auction = running ?: AuctionManager.visibleFinished() ?: return
        val anchor = auction.anchor ?: return
        val camera = context.levelState().cameraRenderState ?: return
        val cameraPos = camera.pos

        val seconds = System.currentTimeMillis() / 1000.0
        val bob = if (config.bobbingItem && running != null) sin(seconds * 2.0) * 0.06 else 0.0
        val scale = config.scale.coerceIn(0.5f, 3f)

        val pose = context.poseStack()
        val collector = context.submitNodeCollector()

        pose.pushPose()
        try {
            pose.translate(anchor.x - cameraPos.x, anchor.y - cameraPos.y + bob, anchor.z - cameraPos.z)

            // The item, spinning around its vertical axis.
            itemState.clear()
            mc.itemModelResolver.updateForNonLiving(itemState, auction.item, ItemDisplayContext.GROUND, player)
            if (!itemState.isEmpty) {
                pose.pushPose()
                pose.scale(1.4f * scale, 1.4f * scale, 1.4f * scale)
                pose.mulPose(Matrix4f().rotationY(((seconds * 0.9) % (2 * Math.PI)).toFloat()))
                itemState.submit(pose, collector, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0)
                pose.popPose()
            }

            // Text above the item, drawn as name tags so it always faces you.
            pose.pushPose()
            pose.scale(scale, scale, scale)
            val top = auction.highest
            val subtitle = when {
                running == null && top != null -> Component.literal("Sold to ${top.player} for $${AmountParser.formatShort(top.amount)}").withStyle(ChatFormatting.GREEN)
                running == null -> Component.literal("No bids").withStyle(ChatFormatting.GRAY)
                top != null -> Component.literal("Top bid $${AmountParser.formatShort(top.amount)} by ${top.player}").withStyle(ChatFormatting.GREEN)
                else -> Component.literal("Min bid $${AmountParser.formatShort(auction.minimumBid)}").withStyle(ChatFormatting.GRAY)
            }
            val timeLine = if (running != null) {
                val left = auction.secondsLeft()
                val color = if (left <= 10) ChatFormatting.RED else ChatFormatting.GOLD
                Component.literal("%d:%02d".format(left / 60, left % 60)).withStyle(color, ChatFormatting.BOLD)
            } else {
                Component.literal("SOLD").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            }
            val name = Component.literal("${auction.item.count}x ").append(auction.item.hoverName).withStyle(ChatFormatting.WHITE)

            val camState = camera
            collector.submitNameTag(pose, Vec3(0.0, 0.62, 0.0), 0, subtitle, true, FULL_BRIGHT, camState)
            collector.submitNameTag(pose, Vec3(0.0, 0.88, 0.0), 0, name, true, FULL_BRIGHT, camState)
            collector.submitNameTag(pose, Vec3(0.0, 1.14, 0.0), 0, timeLine, true, FULL_BRIGHT, camState)
            pose.popPose()
        } finally {
            pose.popPose()
        }
    }
}
