package de.magicclutch.magicdonut.client.donation

import java.util.Locale

data class Donation(
    val playerName: String,
    val amount: Double,
) {
    companion object {
        /** Compact money format shared by the HUD and the position editor, e.g. `1.50M$`. */
        fun formatMoney(value: Double): String = when {
            value >= 1_000_000_000 -> "%.2fB$".format(Locale.ROOT, value / 1_000_000_000.0)
            value >= 1_000_000 -> "%.2fM$".format(Locale.ROOT, value / 1_000_000.0)
            value >= 1_000 -> "%.2fK$".format(Locale.ROOT, value / 1_000.0)
            else -> "%.2f$".format(Locale.ROOT, value)
        }
    }
}
