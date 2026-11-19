package de.magicclutch.magicdonut.client.config.configuration

import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch

class OverheadStatsConfig {
    @Switch(title = "Show stats", description = "Stats above player name tags. Needs an API key or a stats proxy.")
    var enabled: Boolean = true

    // Global cap for overhead stats API usage.
    // This is used by the auto-refresh logic to scale refresh rate based on nearby player count.
    @Slider(title = "Requests per minute", description = "Global cap for API requests. Lower it if you hit rate limits.", min = 1f, max = 250f, step = 1f)
    var maxRequestsPerMinute: Int = 200

    @Dropdown(title = "Display mode", description = "One fixed stat, or cycle through all of them.")
    var displayMode: DisplayMode = DisplayMode.FIXED

    // Only used when displayMode == CYCLE.
    @Slider(title = "Cycle interval", description = "Seconds per stat in cycle mode.", min = 1f, max = 30f, step = 1f)
    var cycleSeconds: Int = 5

    @Dropdown(title = "Stat", description = "The stat shown in fixed mode.")
    var stat: StatField = StatField.MONEY

    // Relative to the vanilla name tag attachment position.
    // Negative values render below the username, positive values above.
    @Slider(title = "Height", description = "Distance above the name tag.", min = -2f, max = 4f, step = 0.05f)
    var statYOffset: Float = 1.85f

    // Scale factor for the stat label text (1.0 = vanilla size).
    @Slider(title = "Scale", description = "Text size relative to the vanilla name tag.", min = 0.2f, max = 2f, step = 0.05f)
    var statScale: Float = 0.5f

    init {
        live = this
    }

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

    companion object {
        /** OneConfig builds its own instance of this class; the last one created is the one bound to the config file. */
        @JvmStatic
        var live: OverheadStatsConfig = OverheadStatsConfig()
            private set
    }
}
