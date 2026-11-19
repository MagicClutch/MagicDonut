package de.magicclutch.magicdonut.client.config.configuration


class ProfitConfig {
    var enabled: Boolean = true

    var showBalance: Boolean = true

    var showProfit: Boolean = true

    // Seconds between balance refreshes (each refresh is one stats request for yourself).
    var refreshSeconds: Int = 30

    // Distance of the HUD from the bottom of the screen, in GUI pixels.
    var hudBottomOffset: Int = 72

    // Balance at the start of the tracked day, and the local date it belongs to (ISO, e.g. 2026-11-19).
    // A negative balance means "not set yet".
    var startBalance: Double = -1.0

    var startDate: String = ""
}
