package de.magicclutch.magicdonut.client.profit

import com.mojang.blaze3d.platform.InputConstants
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import java.time.LocalDate

/**
 * Tracks how much money you gained or lost today. The balance comes from the tab list footer ([TabListFooter]),
 * so no API key is needed. The first balance seen on a new day becomes that day's starting point; the starting
 * point is stored in the config.
 */
object ProfitTracker {
    // Unbound by default; players pick their own key in the controls screen.
    val toggleBinding: KeyMapping = KeyMappingHelper.registerKeyMapping(
        KeyMapping("magicdonut.toggle_profit", InputConstants.UNKNOWN.value, KeyMapping.Category.MISC)
    )

    private val config get() = MagicDonutConfig.instance.profitConfig

    @Volatile
    var currentBalance: Double? = null
        private set

    /** Gain (positive) or loss (negative) since the start of the tracked day, or `null` if unknown. */
    val profitToday: Double?
        get() {
            val current = currentBalance ?: return null
            if (config.startBalance < 0) return null
            return current - config.startBalance
        }

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (toggleBinding.consumeClick()) {
                config.enabled = !config.enabled
                MagicDonutConfig.save()
            }

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
        config.startBalance = -1.0
        config.startDate = ""
        MagicDonutConfig.save()
    }

    private fun onBalance(balance: Double) {
        // First balance of a new day (or first ever): that is the day's starting point, so profit resets to 0.
        if (config.startBalance < 0 || config.startDate != LocalDate.now().toString()) {
            setBaseline(balance)
        }
    }

    private fun setBaseline(balance: Double) {
        config.startBalance = balance
        config.startDate = LocalDate.now().toString()
        MagicDonutConfig.save()
    }
}
