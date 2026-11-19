package de.magicclutch.magicdonut.client.profit

import net.minecraft.network.chat.Component

/**
 * The tab list footer on DonutSMP reads like `$ 12.5M · 66 ms`. It is captured when the server sends it, so the
 * tab list never has to be opened. The shown amount is rounded by the server (12.5M), so it is only as exact as that.
 */
object TabListFooter {
    private val moneyPattern = Regex("""\$\s*(\d[\d,]*(?:\.\d+)?)\s*([kKmMbBtT])?""")

    @Volatile
    var text: String = ""
        private set

    @Volatile
    var balance: Double? = null
        private set

    fun update(footer: Component?) {
        val raw = footer?.string.orEmpty()
        text = raw
        balance = parse(raw)
    }

    fun clear() {
        text = ""
        balance = null
    }

    fun parse(raw: String): Double? {
        val match = moneyPattern.find(raw) ?: return null
        val number = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        val factor = when (match.groupValues[2].lowercase()) {
            "k" -> 1e3
            "m" -> 1e6
            "b" -> 1e9
            "t" -> 1e12
            else -> 1.0
        }
        return number * factor
    }
}
