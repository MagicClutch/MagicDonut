package de.magicclutch.magicdonut.client.config.configuration

import dev.isxander.yacl3.config.v2.api.SerialEntry

class AuctionConfig {
    @SerialEntry
    var enabled: Boolean = true

    // Position of the hologram panel as a fraction of the screen (center of the panel).
    @SerialEntry
    var x: Float = 0.85f

    @SerialEntry
    var y: Float = 0.30f

    @SerialEntry
    var scale: Float = 1.0f

    @SerialEntry
    var defaultDurationSeconds: Int = 120

    @SerialEntry
    var defaultMinimumBid: Long = 1_000L

    @SerialEntry
    var bobbingItem: Boolean = true

    @SerialEntry
    var playBidSound: Boolean = true
}
