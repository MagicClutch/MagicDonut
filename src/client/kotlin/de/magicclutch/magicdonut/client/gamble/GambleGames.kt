package de.magicclutch.magicdonut.client.gamble

import kotlin.random.Random

/** Pure game logic of the offline slot machine (virtual money only), free of any Minecraft types. */
object GambleGames {
    /** Reel symbols with what three of a kind pays (as a multiple of the bet). */
    enum class Symbol(val displayName: String, val tripleMultiplier: Int) {
        STAR("Nether Star", 20),
        DIAMOND("Diamond", 10),
        EMERALD("Emerald", 6),
        GOLD("Gold Ingot", 4),
        REDSTONE("Redstone", 3),
    }

    /** Two matching symbols pay the bet back. */
    const val PAIR_MULTIPLIER = 1

    data class SpinResult(
        val reels: List<Symbol>,
        /** Total paid back for the bet: 0 on a loss. */
        val payout: Long,
        val description: String,
    ) {
        val won: Boolean get() = payout > 0
    }

    fun spin(bet: Long, random: Random = Random.Default): SpinResult {
        val reels = List(3) { Symbol.entries.random(random) }
        val counts = reels.groupingBy { it }.eachCount()
        return when {
            counts.size == 1 -> {
                val symbol = reels[0]
                SpinResult(reels, bet * symbol.tripleMultiplier, "Three ${symbol.displayName}s! ${symbol.tripleMultiplier}x")
            }
            counts.size == 2 -> SpinResult(reels, bet * PAIR_MULTIPLIER, "A pair: your bet back.")
            else -> SpinResult(reels, 0, "No match.")
        }
    }
}
