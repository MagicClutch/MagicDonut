package de.magicclutch.magicdonut.client.profit

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import java.time.LocalDate

/**
 * Tracks how much money you gained or lost today. The balance comes from the tab list footer ([TabListFooter]),
 * so no API key is needed. The first balance seen on a new day becomes that day's starting point; the starting
 * point is stored in the config.
 */
object ProfitTracker {
    private val config get() = MagicDonutConfig.instance

    @Volatile
    var currentBalance: Double? = null
        private set

    /** Gain (positive) or loss (negative) since the start of the tracked day, or `null` if unknown. */
    val profitToday: Double?
        get() {
            val current = currentBalance ?: return null
            if (config.profitStartBalance < 0) return null
            return current - config.profitStartBalance
        }

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.level == null || !MinecraftClientService.isOnDonut) {
                currentBalance = null
                if (client.level == null) TabListFooter.clear()
                return@register
            }

            val balance = TabListFooter.balance
            currentBalance = balance
            if (balance != null) onBalance(balance)
        }
    }

    /** Saves the current balance as the start of today. Returns `false` if the balance isn't known yet. */
    fun startToday(): Boolean {
        val balance = currentBalance ?: return false
        setBaseline(balance)
        return true
    }

    fun resetBaseline() {
        config.profitStartBalance = -1.0
        config.profitStartDate = ""
        MagicDonutConfig.save()
    }

    private fun onBalance(balance: Double) {
        // First balance of a new day (or first ever): that is the day's starting point, so profit resets to 0.
        if (config.profitStartBalance < 0 || config.profitStartDate != LocalDate.now().toString()) {
            setBaseline(balance)
        }
    }

    private fun setBaseline(balance: Double) {
        config.profitStartBalance = balance
        config.profitStartDate = LocalDate.now().toString()
        MagicDonutConfig.save()
    }
}
