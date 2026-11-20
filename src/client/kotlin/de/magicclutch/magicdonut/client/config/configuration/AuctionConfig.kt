package de.magicclutch.magicdonut.client.config.configuration

import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.config.v1.annotations.Text

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

    @Text(title = "Default minimum bid", description = "Payments below this are ignored. Short forms work: 500, 10k, 2.5m, 1b.")
    var defaultMinimumBid: String = "1k"

    @Slider(title = "Time added per bid", description = "Seconds added to the timer for every payment (bid). 0 turns it off.", min = 0f, max = 30f, step = 1f)
    var bidTimeBonusSeconds: Int = 3

    @Switch(title = "Bid effect", description = "A burst of particles and a floating +time text whenever a bid comes in.")
    var bidEffect: Boolean = true

    @Switch(title = "Glow ring", description = "A glowing ring of light circles the item while the auction runs.")
    var glowRing: Boolean = true

    @Switch(title = "Floating animation", description = "The item gently bobs up and down.")
    var bobbingItem: Boolean = true

    @Switch(title = "Sound on new top bid", description = "A short sound when someone takes the lead.")
    var playBidSound: Boolean = true

    @Switch(title = "Sound when auction ends", description = "The level-up jingle when the timer runs out.")
    var playEndSound: Boolean = true

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
