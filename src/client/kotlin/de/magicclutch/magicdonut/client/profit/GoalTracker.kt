package de.magicclutch.magicdonut.client.profit

import de.magicclutch.magicdonut.client.hud.GoalHud
import de.magicclutch.magicdonut.client.util.Money
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import de.magicclutch.magicdonut.client.util.AmountParser
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

/** Balance goal: shows the progress and announces in chat once when the balance crosses it. */
object GoalTracker {
    private var previous: Double? = null

    private val goal: Double? get() = parse(GoalHud.live?.goalAmount ?: "100m")

    private fun parse(text: String): Double? = AmountParser.parse(text)?.toDouble()

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register {
            val balance = ProfitTracker.currentBalance
            val target = goal
            val before = previous
            previous = balance
            if (balance == null || target == null || before == null) return@register
            if (before < target && balance >= target) {
                MinecraftClientService.sendChatMessage(
                    Component.literal("[MagicDonutUtils] Goal reached: ${Money.format(target)}!").withStyle(ChatFormatting.GOLD)
                )
            }
        }
    }

    fun progressText(goalText: String): String {
        val target = parse(goalText) ?: return "not set"
        val balance = ProfitTracker.currentBalance ?: return "-"
        val percent = (balance / target * 100).coerceAtLeast(0.0)
        return "${Money.format(balance)} / ${Money.format(target)} (${"%.0f".format(java.util.Locale.ROOT, percent)}%)"
    }
}
