package de.magicclutch.magicdonut.client.donation

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.overheadstats.StatsRenderer
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
            "${donation.playerName} : ${StatsRenderer.formatMoney(donation.amount)}"
        } else {
            config.noDonationText
        }

        val text = "${config.customTitle}: $contentText"

        // --- ZENTRIERUNGS LOGIK (RESIZE-SAFE) ---
        val textWidth = font.width(text)
        val screenWidth = mc.window.guiScaledWidth
        val centerX = screenWidth / 2f

        // Berechne die Breite inklusive Skalierung
        val totalScaledWidth = textWidth * config.size

        // Berechne Start-X so, dass die Mitte des Textes auf der Mitte des Screens liegt
        val xPos = centerX - (totalScaledWidth / 2f)

        context.pose().pushPose()

        // Translation nutzt berechnetes xPos statt config.x
        context.pose().translate(xPos, config.y.toFloat(), 300f)
        context.pose().scale(config.size, config.size, 1f)

        if (config.showBox) {
            val paddingX = 6
            val paddingY = 2

            // Zeichne Box relativ zu 0,0 (da Pose bereits verschoben ist)
            drawRoundedRect(
                context,
                -paddingX,
                -paddingY,
                textWidth + (paddingX * 2),
                9 + (paddingY * 2),
                3,
                config.boxColor
            )
        }

        context.drawString(font, text, 0, 0, config.color, true)

        context.pose().popPose()
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