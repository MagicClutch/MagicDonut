package de.magicclutch.magicdonut.client.hud

import de.magicclutch.magicdonut.client.earnings.EarningsTracker
import de.magicclutch.magicdonut.client.util.Money
import de.magicclutch.magicdonut.client.profit.GoalTracker
import org.polyfrost.oneconfig.api.config.v1.annotations.Text
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.TextHud

/** What you earned today and the top payers. */
class EarningsHud : TextHud("magicdonut_earnings", "Earnings", Hud.Category.INFO, "Today: ", "") {
    override fun getText(): String {
        val top = EarningsTracker.topPayers(3)
        val today = Money.format(EarningsTracker.today())
        return if (top.isEmpty()) today else today + "  |  " + top.joinToString("  ") { "${it.first} ${Money.format(it.second)}" }
    }
}

/** Progress of your balance towards the goal. The goal is set in this HUD's own settings in the HUD editor. */
class GoalHud : TextHud("magicdonut_goal", "Goal", Hud.Category.INFO, "Goal: ", "") {
    @Text(title = "Goal", description = "Balance goal. Short forms work: 50m, 1b.")
    var goalAmount: String = "100m"

    init {
        live = this
    }

    override fun getText(): String = GoalTracker.progressText(goalAmount)

    companion object {
        /** The goal text of the HUD that is shown (the last one created). */
        @JvmStatic
        var live: GoalHud? = null
            private set
    }
}

/** A short custom line, for example what you are streaming. The text is set in this HUD's own settings. */
class InfoHud : TextHud("magicdonut_info", "Info", Hud.Category.INFO, "", "") {
    @Text(title = "Text", description = "The line this HUD shows, for example what you stream.")
    var infoText: String = ""

    override fun getText(): String = infoText.ifBlank { "Set the text in this HUD's settings" }
}
