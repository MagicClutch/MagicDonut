package de.magicclutch.magicdonut.client.config.configuration

import dev.isxander.yacl3.config.v2.api.SerialEntry

class ProfitConfig {
    @SerialEntry
    var enabled: Boolean = true

    @SerialEntry
    var showBalance: Boolean = true

    @SerialEntry
    var showProfit: Boolean = true

    // Seconds between balance refreshes (each refresh is one stats request for yourself).
    @SerialEntry
    var refreshSeconds: Int = 30

    // Distance of the HUD from the bottom of the screen, in GUI pixels.
    @SerialEntry
    var hudBottomOffset: Int = 72

    // Balance at the start of the tracked day, and the local date it belongs to (ISO, e.g. 2026-11-19).
    // A negative balance means "not set yet".
    @SerialEntry
    var startBalance: Double = -1.0

    @SerialEntry
    var startDate: String = ""
}
