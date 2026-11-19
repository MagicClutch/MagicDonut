package de.magicclutch.magicdonut.client.config.configuration

import org.polyfrost.oneconfig.api.config.v1.annotations.Number
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch

class AuctionConfig {
    @Switch(title = "Show hologram", description = "The item floats in the world in front of you while an auction runs.")
    var enabled: Boolean = true

    // Position of the hologram panel as a fraction of the screen (center of the panel).
    @Slider(title = "HUD panel: horizontal", description = "0 is the left edge, 1 the right edge.", min = 0f, max = 1f, step = 0.01f)
    var x: Float = 0.85f

    @Slider(title = "HUD panel: vertical", description = "0 is the top edge, 1 the bottom edge.", min = 0f, max = 1f, step = 0.01f)
    var y: Float = 0.30f

    @Slider(title = "Scale", description = "Size of the hologram and the panel.", min = 0.5f, max = 3f, step = 0.1f)
    var scale: Float = 1.0f

    @Slider(title = "Default duration", description = "Seconds, used when /bid start has no duration.", min = 10f, max = 900f, step = 10f)
    var defaultDurationSeconds: Int = 120

    @Number(title = "Default minimum bid", description = "Payments below this are ignored.", min = 0f, max = 1_000_000_000f)
    var defaultMinimumBid: Long = 1_000L

    @Switch(title = "Floating animation", description = "The item gently bobs up and down.")
    var bobbingItem: Boolean = true

    @Switch(title = "Sound on new top bid", description = "A short sound when someone takes the lead.")
    var playBidSound: Boolean = true

    // The flat panel on the HUD, in addition to the hologram in the world.
    @Switch(title = "Also show HUD panel", description = "A flat panel on screen in addition to the hologram.")
    var showHudPanel: Boolean = false

    init {
        live = this
    }

    companion object {
        /** OneConfig builds its own instance of this class; the last one created is the one bound to the config file. */
        @JvmStatic
        var live: AuctionConfig = AuctionConfig()
            private set
    }
}
