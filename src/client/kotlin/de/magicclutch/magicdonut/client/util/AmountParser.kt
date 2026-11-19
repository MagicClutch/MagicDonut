package de.magicclutch.magicdonut.client.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/** Parsing and formatting of DonutSMP-style money amounts (`1500`, `2.5k`, `3m`, `1.2b`). */
object AmountParser {
    private val amountPattern = Regex("""(\d+(?:\.\d+)?)([kmb])?""", RegexOption.IGNORE_CASE)

    /** Returns the amount, or `null` for anything that is not a positive, in-range amount. */
    fun parse(input: String): Long? {
        val match = amountPattern.matchEntire(input.trim()) ?: return null
        val number = BigDecimal(match.groupValues[1])
        val multiplier = when (match.groupValues[2].lowercase()) {
            "k" -> 1_000L
            "m" -> 1_000_000L
            "b" -> 1_000_000_000L
            else -> 1L
        }
        return try {
            number.multiply(BigDecimal.valueOf(multiplier))
                .setScale(0, RoundingMode.DOWN)
                .longValueExact()
                .takeIf { it > 0 }
        } catch (_: ArithmeticException) {
            null
        }
    }

    /** `1500000` -> `1.5m`, `999` -> `999`. */
    fun formatShort(amount: Double): String {
        fun scaled(divisor: Double, suffix: String) =
            "%.2f".format(Locale.ROOT, amount / divisor).trimEnd('0').trimEnd('.') + suffix

        return when {
            amount >= 1_000_000_000 -> scaled(1_000_000_000.0, "b")
            amount >= 1_000_000 -> scaled(1_000_000.0, "m")
            amount >= 1_000 -> scaled(1_000.0, "k")
            else -> "%.0f".format(Locale.ROOT, amount)
        }
    }

    fun formatShort(amount: Long): String = formatShort(amount.toDouble())
}
