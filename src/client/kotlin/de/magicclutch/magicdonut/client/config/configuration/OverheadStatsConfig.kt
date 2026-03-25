package de.magicclutch.magicdonut.client.config.configuration

import dev.isxander.yacl3.config.v2.api.SerialEntry

class OverheadStatsConfig {
    @SerialEntry
    var enabled: Boolean = true

    @SerialEntry
    var displayMode: DisplayMode = DisplayMode.FIXED

    // Only used when displayMode == CYCLE.
    @SerialEntry
    var cycleSeconds: Int = 5

    @SerialEntry
    var stat: StatField = StatField.MONEY

    // Relative to the vanilla name tag attachment position.
    // Negative values render below the username, positive values above.
    @SerialEntry
    var statYOffset: Float = 1.85f

    // Scale factor for the stat label text (1.0 = vanilla size).
    @SerialEntry
    var statScale: Float = 0.5f

    enum class DisplayMode {
        FIXED,
        CYCLE
    }

    enum class StatField(
        val apiKey: String,
        val displayName: String,
        val iconGlyph: String? = null
    ) {
        MONEY("money", "Money"),
        SHARDS("shards", "Shards"),
        PLAYTIME("playtime", "Playtime"),
        KILLS("kills", "Kills"),
        DEATHS("deaths", "Deaths", "☠");

        override fun toString(): String = displayName
    }
}
