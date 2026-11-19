package de.magicclutch.magicdonut.client.config.configuration


class OverheadStatsConfig {
    var enabled: Boolean = true

    // Global cap for overhead stats API usage.
    // This is used by the auto-refresh logic to scale refresh rate based on nearby player count.
    var maxRequestsPerMinute: Int = 200

    var displayMode: DisplayMode = DisplayMode.FIXED

    // Only used when displayMode == CYCLE.
    var cycleSeconds: Int = 5

    var stat: StatField = StatField.MONEY

    // Relative to the vanilla name tag attachment position.
    // Negative values render below the username, positive values above.
    var statYOffset: Float = 1.85f

    // Scale factor for the stat label text (1.0 = vanilla size).
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
