package de.magicclutch.magicdonut.client.auction

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.config.configuration.AuctionConfig
import de.magicclutch.magicdonut.client.util.AmountParser
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import kotlin.math.cos
import kotlin.math.sin

/**
 * The auction hologram: the item floats and slowly spins in the world in front of where the auction was started.
 * Above it, from the top: the time left, the item name and one subtitle (top bid, or the minimum bid).
 * Only you see it; it is drawn client-side.
 */
object AuctionHologram {
    private const val FULL_BRIGHT = 15728880

    private val itemState = ItemStackRenderState()

    private const val BONUS_TEXT_MS = 1800L
    private const val RING_POINTS = 8
    private const val RING_RADIUS = 0.62

    fun register() {
        LevelRenderEvents.COLLECT_SUBMITS.register { render(it) }
        ClientTickEvents.END_CLIENT_TICK.register { tickRing(it) }
    }

    /** RGB of a chosen color; `null` means the plain white end-rod sparkle. Rainbow cycles through hues per particle. */
    private fun particleFor(color: AuctionConfig.ParticleColor, index: Int, phase: Double): ParticleOptions {
        val rgb = when (color) {
            AuctionConfig.ParticleColor.WHITE -> return ParticleTypes.END_ROD
            AuctionConfig.ParticleColor.GOLD -> 0xFFC800
            AuctionConfig.ParticleColor.ORANGE -> 0xFF7A18
            AuctionConfig.ParticleColor.RED -> 0xFF3030
            AuctionConfig.ParticleColor.PINK -> 0xFF60C0
            AuctionConfig.ParticleColor.PURPLE -> 0xB040FF
            AuctionConfig.ParticleColor.BLUE -> 0x3050FF
            AuctionConfig.ParticleColor.CYAN -> 0x30E0FF
            AuctionConfig.ParticleColor.GREEN -> 0x30FF50
            AuctionConfig.ParticleColor.RAINBOW -> java.awt.Color.HSBtoRGB(((phase * 0.25 + index * 0.07) % 1.0).toFloat(), 0.85f, 1f) and 0xFFFFFF
        }
        return DustParticleOptions(rgb, 0.9f)
    }

    /**
     * Glowing particles in the chosen style and color. Everything happens underneath the item, which hovers on top
     * of it like a pedestal effect.
     */
    private fun tickRing(mc: Minecraft) {
        val level = mc.level ?: return
        val config = MagicDonutConfig.instance.auctionConfig
        if (!config.enabled || !config.glowRing) return
        val anchor = AuctionManager.current?.anchor ?: return

        val scale = config.scale.coerceIn(0.5f, 3f).toDouble()
        val radius = RING_RADIUS * scale
        val phase = System.currentTimeMillis() / 1000.0
        val cx = anchor.x
        val cz = anchor.z
        val base = anchor.y - 0.45 * scale      // the floor of the effect, below the item
        var n = 0
        fun emit(x: Double, y: Double, z: Double, vx: Double = 0.0, vy: Double = 0.0, vz: Double = 0.0) {
            level.addParticle(particleFor(config.particleColor, n++, phase), x, y, z, vx, vy, vz)
        }

        when (config.particleStyle) {
            // Two flat rings turning in opposite directions.
            AuctionConfig.ParticleStyle.RING -> for (i in 0 until RING_POINTS) {
                val angle = phase * 2.2 + i * (2 * Math.PI / RING_POINTS)
                emit(cx + cos(angle) * radius, base, cz + sin(angle) * radius)
                val inner = -phase * 3.0 + i * (2 * Math.PI / RING_POINTS)
                if (i % 2 == 0) emit(cx + cos(inner) * radius * 0.55, base + 0.03, cz + sin(inner) * radius * 0.55)
            }

            // Two strands winding upwards, stopping right under the item.
            AuctionConfig.ParticleStyle.HELIX -> for (strand in 0..1) for (i in 0 until 6) {
                val t = i / 6.0
                val angle = phase * 2.0 + t * 3 * Math.PI + strand * Math.PI
                emit(cx + cos(angle) * radius * 0.8, base + t * 0.4 * scale, cz + sin(angle) * radius * 0.8)
            }

            // Three flat tilted orbits crossing each other, like an atom.
            AuctionConfig.ParticleStyle.ATOM -> for (orbit in 0..2) for (k in 0..1) {
                val theta = phase * 2.6 + k * Math.PI + orbit * 0.9
                val px = cos(theta) * radius * 1.05
                val py = sin(theta) * 0.14 * scale
                val tilt = orbit * Math.PI / 3
                emit(cx + px * cos(tilt), base + 0.14 * scale + py, cz + px * sin(tilt))
            }

            // Twinkling stars on a low dome under the item.
            AuctionConfig.ParticleStyle.SPARKLES -> repeat(3) {
                val yaw = Math.random() * 2 * Math.PI
                val r = radius * Math.sqrt(Math.random()) * 1.1
                emit(cx + cos(yaw) * r, base + Math.random() * 0.35 * scale, cz + sin(yaw) * r)
            }

            // Sparks bubbling up from the floor and arcing outwards.
            AuctionConfig.ParticleStyle.FOUNTAIN -> for (i in 0..2) {
                val angle = phase * 5.0 + i * (2 * Math.PI / 3)
                emit(cx, base, cz, cos(angle) * 0.03, 0.05 + Math.random() * 0.02, sin(angle) * 0.03)
            }

            // A narrowing whirl rising up towards the item.
            AuctionConfig.ParticleStyle.VORTEX -> for (i in 0 until 10) {
                val t = (phase * 0.6 + i / 10.0) % 1.0
                val angle = phase * 3.4 + i * 0.9
                val r = radius * (1.3 - t * 1.0)
                emit(cx + cos(angle) * r, base + t * 0.4 * scale, cz + sin(angle) * r)
            }
        }
    }

    /** One-off burst when a bid comes in: sparks fly outwards from the item. */
    fun burst(auction: AuctionManager.Auction) {
        val anchor = auction.anchor ?: return
        val mc = Minecraft.getInstance()
        mc.execute {
            val level = mc.level ?: return@execute
            for (i in 0 until 36) {
                val angle = i * (2 * Math.PI / 36)
                level.addParticle(
                    ParticleTypes.FIREWORK,
                    anchor.x, anchor.y + 0.05, anchor.z,
                    cos(angle) * 0.18, 0.03, sin(angle) * 0.18
                )
            }
            for (i in 0 until 10) {
                level.addParticle(ParticleTypes.END_ROD, anchor.x, anchor.y, anchor.z, 0.0, 0.12 + i * 0.01, 0.0)
            }
        }
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
                pose.mulPose(Matrix4f().rotationY((((seconds * 0.9) % (2 * Math.PI)).toFloat()) + AuctionSounds.spinAngle(auction.lastBidAtMs)))
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
            val delayLine = Component.literal("Stream Delay +${auction.streamDelaySeconds}s").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
            val name = Component.literal("${auction.item.count}x ").append(auction.item.hoverName).withStyle(ChatFormatting.WHITE)

            val camState = camera
            collector.submitNameTag(pose, Vec3(0.0, 0.62, 0.0), 0, subtitle, true, FULL_BRIGHT, camState)
            collector.submitNameTag(pose, Vec3(0.0, 0.88, 0.0), 0, name, true, FULL_BRIGHT, camState)
            // Under 10 seconds the time shakes, harder and harder towards zero.
            val (shakeX, shakeY) = AuctionSounds.shake(running)
            collector.submitNameTag(pose, Vec3(shakeX * 0.05, 1.14 + shakeY * 0.04, 0.0), 0, timeLine, true, FULL_BRIGHT, camState)

            // "Stream Delay +5s" rises from the timer the moment the stream delay restarts it, like the "+3s" of a bid.
            val sinceDelay = System.currentTimeMillis() - auction.lastDelayAtMs
            if (running != null && auction.lastDelayAtMs > 0L && sinceDelay in 0..BONUS_TEXT_MS) {
                val rise = sinceDelay.toDouble() / BONUS_TEXT_MS
                collector.submitNameTag(pose, Vec3(0.0, 1.4 + rise * 0.5, 0.0), 0, delayLine, true, FULL_BRIGHT, camState)
            }

            // "+3s" floats up above the timer for a moment after every bid.
            val sinceBonus = System.currentTimeMillis() - auction.lastBonusAtMs
            if (running != null && config.bidEffect && auction.lastBonusSeconds > 0 && sinceBonus in 0..BONUS_TEXT_MS) {
                val rise = sinceBonus.toDouble() / BONUS_TEXT_MS
                val bonusLine = Component.literal("+${auction.lastBonusSeconds}s").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                collector.submitNameTag(pose, Vec3(0.0, 1.7 + rise * 0.5, 0.0), 0, bonusLine, true, FULL_BRIGHT, camState)
            }
            pose.popPose()
        } finally {
            pose.popPose()
        }
    }
}
