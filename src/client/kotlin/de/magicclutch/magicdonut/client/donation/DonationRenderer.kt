package de.magicclutch.magicdonut.client.donation

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen

object DonationRenderer {
    fun register() {
        HudRenderCallback.EVENT.register { context, _ ->
            val mc = Minecraft.getInstance()
            val config = MagicDonutConfig.instance.donationConfig

            // 1. Check: Ist das Modul überhaupt an?
            if (!config.enabled) return@register

            // 2. Check: Sind wir in einer Welt?
            if (mc.level == null) return@register

            val screen = mc.screen
            if (screen == null || screen is ChatScreen || screen is InventoryScreen || screen is PositionEditScreen) {
                render(context)
            }
        }
    }

    fun render(context: GuiGraphics) {
        val mc = Minecraft.getInstance()
        val config = MagicDonutConfig.instance.donationConfig
        val donation = config.getHighestDonation()
        val font = mc.font

        val contentText = if (donation != null) {
            "${donation.playerName} : ${formatMoney(donation.amount)}"
        } else {
            config.noDonationText
        }

        val text = "${config.customTitle}: $contentText"
        val textWidth = font.width(text).toFloat()
        val screenWidth = mc.window.guiScaledWidth.toFloat()
        val screenHeight = mc.window.guiScaledHeight.toFloat()
        val centerX = screenWidth * config.x.coerceIn(0f, 1f)
        val centerY = screenHeight * config.y.coerceIn(0f, 1f)
        val pose = context.pose()
        pose.pushMatrix()
        pose.translate(centerX, centerY)
        pose.scale(config.size, config.size)

        if (config.showBox) {
            val paddingX = 6
            val paddingY = 2

            drawRoundedRect(
                context,
                -(textWidth / 2).toInt() - paddingX,
                -paddingY,
                (textWidth + paddingX * 2).toInt(),
                9 + (paddingY * 2),
                3,
                config.boxColor
            )
        }

        context.drawString(font, text, -(textWidth / 2).toInt(), 0, config.color, true)

        pose.popMatrix()
    }

    private fun formatMoney(value: Float): String {
        return when {
            value >= 1_000_000_000 -> String.format("%.2fB$", value / 1_000_000_000.0)
            value >= 1_000_000 -> String.format("%.2fM$", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.2fK$", value / 1_000.0)
            else -> String.format("%.2f$", value)
        }
    }

    private fun drawRoundedRect(context: GuiGraphics, x: Int, y: Int, width: Int, height: Int, radius: Int, color: Int) {
        context.fill(x + radius, y, x + width - radius, y + height, color)
        context.fill(x, y + radius, x + radius, y + height - radius, color)
        context.fill(x + width - radius, y + radius, x + width, y + height - radius, color)

        context.fill(x + 1, y + 1, x + radius, y + radius, color)
        context.fill(x + width - radius, y + 1, x + width - 1, y + radius, color)
        context.fill(x + 1, y + height - radius, x + radius, y + height - 1, color)
        context.fill(x + width - radius, y + height - radius, x + width - 1, y + height - 1, color)
    }
}
