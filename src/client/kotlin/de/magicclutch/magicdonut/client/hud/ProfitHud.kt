package de.magicclutch.magicdonut.client.hud

import de.magicclutch.magicdonut.client.profit.ProfitTracker
import de.magicclutch.magicdonut.client.util.AmountParser
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.TextHud
import kotlin.math.abs

/** Gain or loss since the start of today. */
class ProfitHud : TextHud("magicdonut_profit", "Daily Profit", Hud.Category.INFO, "Today: ", "") {
    override fun getText(): String {
        val profit = ProfitTracker.profitToday ?: return "-"
        val sign = if (profit >= 0) "+" else "-"
        return "$sign$${AmountParser.formatShort(abs(profit))}"
    }
}
