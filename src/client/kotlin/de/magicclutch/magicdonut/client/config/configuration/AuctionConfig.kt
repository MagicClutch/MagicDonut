package de.magicclutch.magicdonut.client.config.configuration

import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.config.v1.annotations.Text

class AuctionConfig {
    @Slider(title = "Duration", description = "Seconds, when /bid start has no duration.", min = 10f, max = 900f, step = 10f)
    var defaultDurationSeconds: Int = 120

    @Text(title = "Minimum bid", description = "Payments below this are ignored. Short forms work: 500, 10k, 2.5m, 1b.")
    var defaultMinimumBid: String = "1k"

    @Slider(title = "Time per bid", description = "Seconds added for every new top bid. 0 turns it off.", min = 0f, max = 30f, step = 1f)
    var bidTimeBonusSeconds: Int = 3

    @Slider(title = "Stream delay", description = "Seconds the timer restarts at when it hits 0 (usually 4-5). Also: /bid delay <seconds>.", min = 0f, max = 60f, step = 1f)
    var streamDelaySeconds: Int = 0

    @Switch(title = "Countdown sounds", description = "A tick every second, and a rising sound for each of the last 10 seconds.")
    var tickSounds: Boolean = true

    @Slider(title = "Tick volume", description = "Volume of the tick while more than 10 seconds are left. 0 mutes it.", min = 0f, max = 1f, step = 0.05f)
    var tickVolume: Float = 0.5f

    @Switch(title = "Bid bell", description = "A little bell when a bid beats the top bid.")
    var playBidSound: Boolean = true

    @Switch(title = "Winner jingle", description = "The level-up sound when someone wins.")
    var playEndSound: Boolean = true

    @Switch(title = "Hologram", description = "The item floats in the world in front of you while an auction runs.")
    var enabled: Boolean = true

    @Slider(title = "Size", description = "Size of the hologram.", min = 0.5f, max = 3f, step = 0.1f)
    var scale: Float = 1.0f

    @Switch(title = "Bobbing", description = "The item gently floats up and down.")
    var bobbingItem: Boolean = true

    @Switch(title = "Bid effect", description = "A burst of particles and a floating +time text for every new top bid.")
    var bidEffect: Boolean = true

    @Switch(title = "Glow particles", description = "Glowing particles under the item while the auction runs.")
    var glowRing: Boolean = true

    @Dropdown(title = "Particle style", description = "Ring, helix, atom, sparkles, fountain or vortex.")
    var particleStyle: ParticleStyle = ParticleStyle.RING

    @Dropdown(title = "Particle color", description = "Rainbow cycles through all colors.")
    var particleColor: ParticleColor = ParticleColor.WHITE

    init {
        live = this
    }

    enum class ParticleColor { WHITE, GOLD, ORANGE, RED, PINK, PURPLE, BLUE, CYAN, GREEN, RAINBOW }

    enum class ParticleStyle { RING, HELIX, ATOM, SPARKLES, FOUNTAIN, VORTEX }

    companion object {
        /** OneConfig builds its own instance of this class; the last one created is the one bound to the config file. */
        @JvmStatic
        var live: AuctionConfig = AuctionConfig()
            private set
    }
}
