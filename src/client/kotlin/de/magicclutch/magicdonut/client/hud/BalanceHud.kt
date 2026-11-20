package de.magicclutch.magicdonut.client.hud

import de.magicclutch.magicdonut.client.profit.ProfitTracker
import de.magicclutch.magicdonut.client.util.AmountParser
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.TextHud

/** Your current balance, read from the tab list footer. */
class BalanceHud : TextHud("magicdonut_balance", "Balance", Hud.Category.INFO, "Balance: ", "") {
    override fun getText(): String {
        val balance = ProfitTracker.currentBalance ?: return "-"
        return "$${AmountParser.formatShort(balance)}"
    }
}
