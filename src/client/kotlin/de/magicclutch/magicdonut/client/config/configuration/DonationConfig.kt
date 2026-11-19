package de.magicclutch.magicdonut.client.config.configuration

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.donation.Donation
import org.polyfrost.oneconfig.api.config.v1.annotations.Color
import org.polyfrost.oneconfig.api.config.v1.annotations.Include
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.config.v1.annotations.Text

class DonationConfig {
    @Switch(title = "Show HUD", description = "Your highest received donation, on screen.")
    var enabled: Boolean = true

    @Include
    var highestDonationPlayer: String? = null

    @Include
    var highestDonationAmount: Double = 0.0

    @Slider(title = "Horizontal position", description = "0 is the left edge, 1 the right edge.", min = 0f, max = 1f, step = 0.01f)
    var x: Float = 0.5f

    @Slider(title = "Vertical position", description = "0 is the top edge, 1 the bottom edge.", min = 0f, max = 1f, step = 0.01f)
    var y: Float = 0.05f

    @Slider(title = "Size", description = "Scale of the whole HUD.", min = 0.5f, max = 3f, step = 0.1f)
    var size: Float = 1.0F

    @Text(title = "Prefix", description = "Text shown before the donation.")
    var customTitle: String = "Highest Donation"

    @Text(title = "No-donation text", description = "Shown until the first donation arrives.")
    var noDonationText: String = "No Donation"

    @Color(title = "Text color", description = "Color of the text.", alpha = false)
    var color: java.awt.Color = java.awt.Color.WHITE

    @Switch(title = "Background box", description = "Draw a box behind the text.")
    var showBox: Boolean = true

    @Color(title = "Box color", description = "Color of the background box.", alpha = true)
    var boxColor: java.awt.Color = java.awt.Color(192, 192, 192, 150)

    init {
        live = this
    }

    fun getHighestDonation(): Donation? {
        val player = highestDonationPlayer ?: return null
        return Donation(player, highestDonationAmount)
    }

    fun setHighestDonation(donation: Donation) {
        highestDonationPlayer = donation.playerName
        highestDonationAmount = donation.amount
        MagicDonutConfig.save()
    }

    fun reset() {
        highestDonationPlayer = null
        highestDonationAmount = 0.0
        MagicDonutConfig.save()
    }

    companion object {
        /** OneConfig builds its own instance of this class; the last one created is the one bound to the config file. */
        @JvmStatic
        var live: DonationConfig = DonationConfig()
            private set
    }
}
