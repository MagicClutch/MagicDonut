package de.magicclutch.magicdonut.client.profit

import com.google.gson.JsonElement
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.service.APIService
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import java.time.LocalDate
import java.util.concurrent.Executors

/**
 * Tracks how much money you gained or lost today. Your own balance is read from the stats API at a low
 * rate; the balance at the start of the day is stored in the config.
 */
object ProfitTracker {
    // Unbound by default; players pick their own key in the controls screen.
    val toggleBinding: KeyMapping = KeyMappingHelper.registerKeyMapping(
        KeyMapping("magicdonut.toggle_profit", -1, KeyMapping.Category.MISC)
    )

    private val config get() = MagicDonutConfig.instance.profitConfig

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "MagicDonutUtils-Profit").apply { isDaemon = true }
    }

    @Volatile
    var currentBalance: Double? = null
        private set

    @Volatile
    private var fetching = false
    private var lastFetchMs = 0L

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
                MagicDonutConfig.HANDLER.save()
            }

            if (client.level == null) {
                currentBalance = null
                return@register
            }
            if (!config.enabled || !MinecraftClientService.isOnDonut || !APIService.isAvailable()) return@register

            val now = System.currentTimeMillis()
            if (fetching || now - lastFetchMs < config.refreshSeconds.coerceIn(10, 600) * 1000L) return@register
            fetching = true
            lastFetchMs = now

            val name = client.user.name
            executor.execute {
                try {
                    val balance = APIService.getStats(name)?.get("money")?.let(::parseDouble)
                    if (balance != null) client.execute { onBalance(balance) }
                } finally {
                    fetching = false
                }
            }
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
        MagicDonutConfig.HANDLER.save()
    }

    private fun onBalance(balance: Double) {
        currentBalance = balance
        // First balance of a new day (or first ever): that is the day's starting point.
        if (config.startBalance < 0 || config.startDate != LocalDate.now().toString()) {
            setBaseline(balance)
        }
    }

    private fun setBaseline(balance: Double) {
        config.startBalance = balance
        config.startDate = LocalDate.now().toString()
        MagicDonutConfig.HANDLER.save()
    }

    private fun parseDouble(element: JsonElement): Double? {
        if (!element.isJsonPrimitive) return null
        val primitive = element.asJsonPrimitive
        return when {
            primitive.isNumber -> primitive.asDouble
            primitive.isString -> primitive.asString.trim().toDoubleOrNull()
            else -> null
        }
    }
}
