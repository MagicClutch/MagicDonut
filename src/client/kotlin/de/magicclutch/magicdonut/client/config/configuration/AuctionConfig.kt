package de.magicclutch.magicdonut.client.config.configuration


class AuctionConfig {
    var enabled: Boolean = true

    // Position of the hologram panel as a fraction of the screen (center of the panel).
    var x: Float = 0.85f

    var y: Float = 0.30f

    var scale: Float = 1.0f

    var defaultDurationSeconds: Int = 120

    var defaultMinimumBid: Long = 1_000L

    var bobbingItem: Boolean = true

    var playBidSound: Boolean = true

    // The flat panel on the HUD, in addition to the hologram in the world.
    var showHudPanel: Boolean = false
}
