package de.magicclutch.magicdonut.client.config.configuration

import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider

class AuctionConfig {
    @Slider(title = "Duration", description = "Seconds, when /bid start has no duration.", min = 10f, max = 900f, step = 10f)
    var defaultDurationSeconds: Int = 120

    @Slider(title = "Extra time per top bid", description = "Seconds added to the timer for every new top bid. 0 turns it off.", min = 0f, max = 30f, step = 1f)
    var bidTimeBonusSeconds: Int = 3

    @Slider(title = "Stream delay", description = "Seconds the timer restarts at when it hits 0 (usually 4-5). Also: /bid delay <seconds>.", min = 0f, max = 60f, step = 1f)
    var streamDelaySeconds: Int = 0

    @Slider(title = "Sound volume", description = "Volume of all auction sounds (tick, countdown, bid bell, winner jingle). 0 mutes them.", min = 0f, max = 1f, step = 0.05f)
    var soundVolume: Float = 1.0f

    @Dropdown(title = "Particle style", description = "Ring, helix, atom, sparkles, fountain or vortex.")
    var particleStyle: ParticleStyle = ParticleStyle.RING

    @Dropdown(title = "Particle color", description = "Rainbow cycles through all colors.")
    var particleColor: ParticleColor = ParticleColor.WHITE

    @Slider(title = "Size", description = "Size of the hologram.", min = 0.5f, max = 3f, step = 0.1f)
    var scale: Float = 1.0f

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
