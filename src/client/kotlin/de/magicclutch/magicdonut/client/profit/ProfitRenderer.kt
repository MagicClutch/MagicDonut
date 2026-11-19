package de.magicclutch.magicdonut.client.profit

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.util.AmountParser
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import kotlin.math.abs

/** Small HUD above the hotbar: current balance and today's profit or loss. */
object ProfitRenderer {
    private const val COLOR_TEXT = 0xFFFFFFFF.toInt()
    private const val COLOR_UP = 0xFF55FF55.toInt()
    private const val COLOR_DOWN = 0xFFFF5555.toInt()
    private const val COLOR_NEUTRAL = 0xFFAAAAAA.toInt()

    fun register() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.HOTBAR,
            Identifier.fromNamespaceAndPath("magicdonut", "profit_hud")
        ) { context, _ -> render(context) }
    }

    private fun render(context: GuiGraphicsExtractor) {
        val config = MagicDonutConfig.instance.profitConfig
        if (!config.enabled) return

        val mc = Minecraft.getInstance()
        if (mc.level == null) return

        val balance = ProfitTracker.currentBalance ?: return
        val profit = ProfitTracker.profitToday

        val lines = buildList {
            if (config.showBalance) add("Balance: $${AmountParser.formatShort(balance)}" to COLOR_TEXT)
            if (config.showProfit && profit != null) {
                val sign = if (profit >= 0) "+" else "-"
                val color = when {
                    profit > 0 -> COLOR_UP
                    profit < 0 -> COLOR_DOWN
                    else -> COLOR_NEUTRAL
                }
                add("Today: $sign$${AmountParser.formatShort(abs(profit))}" to color)
            }
        }
        if (lines.isEmpty()) return

        val font = mc.font
        val centerX = mc.window.guiScaledWidth / 2
        var y = mc.window.guiScaledHeight - config.hudBottomOffset.coerceIn(20, 200) - lines.size * 10
        for ((text, color) in lines) {
            context.centeredText(font, text, centerX, y, color)
            y += 10
        }
    }
}
