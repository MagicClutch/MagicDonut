package de.magicclutch.magicdonut.client.config.configuration


/** Settings and saved state of the offline gamble simulator. Nothing here touches the server. */
class GambleConfig {
    var startingBalance: Long = 10_000L

    var balance: Long = 10_000L

    var soundEnabled: Boolean = true

    var gamesPlayed: Int = 0

    var gamesWon: Int = 0

    var biggestWin: Long = 0L

    fun resetStats() {
        balance = startingBalance
        gamesPlayed = 0
        gamesWon = 0
        biggestWin = 0L
    }
}
