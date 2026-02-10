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
            val screen = mc.screen

            if (screen == null || screen is ChatScreen || screen is InventoryScreen || screen is PositionEditScreen) {
                render(context)
            }
        }
    }

    fun render(context: GuiGraphics) {
        val config = MagicDonutConfig.instance.donationConfig
        val donation = config.getHighestDonation()
        val font = Minecraft.getInstance().font

        val contentText = if (donation != null) {
            "${donation.playerName} : ${StatsRenderer.formatMoney(donation.amount)}"
        } else {
            config.noDonationText
        }

        val text = "${config.customTitle}: $contentText"
        val textWidth = font.width(text)
        val textHeight = 9

        context.pose().pushPose()

        context.pose().translate(config.x.toFloat(), config.y.toFloat(), 300f)


        context.pose().scale(config.size, config.size, 1f)

        if (config.showBox) {
            val paddingX = 8
            val paddingY = 2

            val x = -paddingX
            val y = -paddingY
            val w = textWidth + paddingX * 2
            val h = textHeight + paddingY * 2


            drawRoundedRect(context, x, y, w, h, 3, config.boxColor)
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