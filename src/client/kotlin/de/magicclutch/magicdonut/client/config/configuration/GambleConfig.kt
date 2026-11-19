package de.magicclutch.magicdonut.client.config.configuration

import dev.isxander.yacl3.config.v2.api.SerialEntry

/** Settings and saved state of the offline gamble simulator. Nothing here touches the server. */
class GambleConfig {
    @SerialEntry
    var startingBalance: Long = 10_000L

    @SerialEntry
    var balance: Long = 10_000L

    @SerialEntry
    var soundEnabled: Boolean = true

    @SerialEntry
    var gamesPlayed: Int = 0

    @SerialEntry
    var gamesWon: Int = 0

    @SerialEntry
    var biggestWin: Long = 0L

    fun resetStats() {
        balance = startingBalance
        gamesPlayed = 0
        gamesWon = 0
        biggestWin = 0L
    }
}
