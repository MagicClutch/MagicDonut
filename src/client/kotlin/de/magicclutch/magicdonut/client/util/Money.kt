package de.magicclutch.magicdonut.client.util

import kotlin.math.abs

/** Shared money formatting and colors for the HUD, screens and toasts. */
object Money {
    const val GOOD = 0xFF55FF55.toInt()
    const val BAD = 0xFFFF5555.toInt()
    const val MUTED = 0xFFAAAAAA.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()

    /** `$1.5k` */
    fun format(amount: Double): String = "$" + AmountParser.formatShort(abs(amount))

    /** `+$300`, `-$2m`, `$0` */
    fun signed(amount: Double): String = when {
        amount > 0 -> "+" + format(amount)
        amount < 0 -> "-" + format(amount)
        else -> format(0.0)
    }

    fun percent(value: Double): String = "%+.1f%%".format(java.util.Locale.ROOT, value)

    fun color(value: Double): Int = when {
        value > 0 -> GOOD
        value < 0 -> BAD
        else -> MUTED
    }

    fun duration(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return when {
            s >= 3600 -> "%dh %02dm".format(s / 3600, s % 3600 / 60)
            s >= 60 -> "%dm %02ds".format(s / 60, s % 60)
            else -> "${s}s"
        }
    }
}
