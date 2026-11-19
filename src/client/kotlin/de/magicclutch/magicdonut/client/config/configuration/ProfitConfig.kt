package de.magicclutch.magicdonut.client.config.configuration

import org.polyfrost.oneconfig.api.config.v1.annotations.Include
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch

class ProfitConfig {
    @Switch(title = "Show HUD", description = "Balance and today's profit above the hotbar, read from the tab list footer.")
    var enabled: Boolean = true

    @Switch(title = "Show balance", description = "Your current balance.")
    var showBalance: Boolean = true

    @Switch(title = "Show today's profit", description = "Gain or loss since the starting point.")
    var showProfit: Boolean = true


    // Distance of the HUD from the bottom of the screen, in GUI pixels.
    @Slider(title = "Height above screen bottom", description = "Move the HUD up or down.", min = 20f, max = 200f, step = 2f)
    var hudBottomOffset: Int = 72

    // Balance at the start of the tracked day, and the local date it belongs to (ISO, e.g. 2026-11-19).
    // A negative balance means "not set yet".
    @Include
    var startBalance: Double = -1.0

    @Include
    var startDate: String = ""

    init {
        live = this
    }

    companion object {
        /** OneConfig builds its own instance of this class; the last one created is the one bound to the config file. */
        @JvmStatic
        var live: ProfitConfig = ProfitConfig()
            private set
    }
}
