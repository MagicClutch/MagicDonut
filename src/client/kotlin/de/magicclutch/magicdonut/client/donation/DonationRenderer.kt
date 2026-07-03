package de.magicclutch.magicdonut.client.donation

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.resources.Identifier
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

object DonationRenderer {
    fun register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, Identifier.fromNamespaceAndPath("magicdonut", "donation_hud")) { context, _ ->
            val mc = Minecraft.getInstance()
            val config = MagicDonutConfig.instance.donationConfig

            // 1. Check: Ist das Modul überhaupt an?
            if (!config.enabled) return@attachElementAfter

            // 2. Check: Sind wir in einer Welt?
            if (mc.level == null) return@attachElementAfter

            val screen = mc.screen
            if (screen == null || screen is ChatScreen || screen is InventoryScreen || screen is PositionEditScreen) {
                render(context)
            }
        }
    }

    fun render(context: GuiGraphicsExtractor) {
        val mc = Minecraft.getInstance()
        val config = MagicDonutConfig.instance.donationConfig
        val donation = config.getHighestDonation()
        val font = mc.font

        // During aggressive window resizing (or when minimized), scaled dimensions can hit 0.
        // Rendering with a 0x0 GUI size can lead to NaN transforms and pose stack corruption if an exception occurs.
        val screenWidthInt = mc.window.guiScaledWidth
        val screenHeightInt = mc.window.guiScaledHeight
        if (screenWidthInt <= 0 || screenHeightInt <= 0) return

        val contentText = if (donation != null) {
            "${donation.playerName} : ${formatMoney(donation.amount)}"
        } else {
            config.noDonationText
        }

        val text = "${config.customTitle}: $contentText"
        val textWidth = font.width(text).toFloat()
        val screenWidth = screenWidthInt.toFloat()
        val screenHeight = screenHeightInt.toFloat()

        val xNorm = config.x.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f
        val yNorm = config.y.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.05f
        val scale = config.size.takeIf { it.isFinite() }?.coerceIn(0.25f, 8.0f) ?: 1.0f

        val centerX = screenWidth * xNorm
        val centerY = screenHeight * yNorm

        val pose = context.pose()
        pose.pushMatrix()
        try {
            pose.translate(centerX, centerY)
            pose.scale(scale, scale)

            if (config.showBox) {
                val paddingX = 6f
                val paddingY = 2f

                val boxX = floor(-(textWidth / 2f) - paddingX).toInt()
                val boxY = floor(-paddingY).toInt()
                val boxW = ceil(textWidth + paddingX * 2f).toInt()
                val boxH = ceil(9f + paddingY * 2f).toInt()

                // No rounded-corners option: always render a simple rectangle.
                safeFill(context, boxX, boxY, boxX + boxW, boxY + boxH, config.boxColor)
            }

            context.text(font, text, -(textWidth / 2f).toInt(), 0, config.color, true)
        } finally {
            // Ensure we never leave the pose stack unbalanced (can crash later frames).
            pose.popMatrix()
        }
    }

    private fun formatMoney(value: Float): String {
        return when {
            value >= 1_000_000_000 -> String.format("%.2fB$", value / 1_000_000_000.0)
            value >= 1_000_000 -> String.format("%.2fM$", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.2fK$", value / 1_000.0)
            else -> String.format("%.2f$", value)
        }
    }


    private fun safeFill(context: GuiGraphicsExtractor, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        val left = min(x1, x2)
        val right = maxOf(x1, x2)
        val top = min(y1, y2)
        val bottom = maxOf(y1, y2)
        if (left == right || top == bottom) return
        context.fill(left, top, right, bottom, color)
    }
}
