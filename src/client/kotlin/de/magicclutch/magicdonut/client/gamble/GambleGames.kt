package de.magicclutch.magicdonut.client.gamble

import kotlin.random.Random

/** Pure game logic of the offline simulator (virtual money only), kept free of any Minecraft types. */
object GambleGames {
    data class Outcome(
        /** Total paid back for the bet: 0 on a loss, `bet * multiplier` on a win. */
        val payout: Long,
        val description: String,
    ) {
        val won: Boolean get() = payout > 0
    }

    enum class CoinSide { HEADS, TAILS }

    private val slotSymbols = listOf("7", "$", "#", "@", "*")

    fun coinflip(bet: Long, pick: CoinSide, random: Random = Random.Default): Outcome {
        val result = if (random.nextBoolean()) CoinSide.HEADS else CoinSide.TAILS
        return if (result == pick) {
            Outcome(bet * 2, "The coin landed on ${result.name.lowercase()}. You win!")
        } else {
            Outcome(0, "The coin landed on ${result.name.lowercase()}. You lose.")
        }
    }

    /** Roll 1-100; rolling 51 or higher wins 1.98x (a 1% house edge, like most real dice games). */
    fun dice(bet: Long, random: Random = Random.Default): Outcome {
        val roll = random.nextInt(1, 101)
        return if (roll > 50) {
            Outcome((bet * 1.98).toLong(), "You rolled $roll (needs 51+). You win!")
        } else {
            Outcome(0, "You rolled $roll (needs 51+). You lose.")
        }
    }

    data class SlotsResult(val reels: List<String>, val outcome: Outcome)

    /** Three reels: three of a kind pays 8x (7s pay 20x), any pair pays 1.5x. */
    fun slots(bet: Long, random: Random = Random.Default): SlotsResult {
        val reels = List(3) { slotSymbols.random(random) }
        val counts = reels.groupingBy { it }.eachCount()
        val outcome = when {
            counts.size == 1 -> {
                val multiplier = if (reels[0] == "7") 20 else 8
                Outcome(bet * multiplier, "Jackpot! Three of a kind pays ${multiplier}x.")
            }
            counts.size == 2 -> Outcome((bet * 1.5).toLong(), "A pair pays 1.5x.")
            else -> Outcome(0, "No match. You lose.")
        }
        return SlotsResult(reels, outcome)
    }
}
