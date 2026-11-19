package de.magicclutch.magicdonut.client.config.configuration

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.donation.Donation
import java.awt.Color

class DonationConfig {
    var enabled: Boolean = true // Der neue An/Aus Schalter

    var highestDonationPlayer: String? = null
    var highestDonationAmount: Double = 0.0

    var x: Float = 0.5f
    var y: Float = 0.05f

    var size: Float = 1.0F

    var customTitle: String = "Highest Donation"

    var noDonationText: String = "No Donation"

    var color: Int = Color.WHITE.rgb

    var showBox: Boolean = true

    var boxColor: Int = Color(192, 192, 192, 150).rgb


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
}
