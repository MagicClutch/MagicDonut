package de.magicclutch.magicdonut.client.config.configuration

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.donation.Donation
import dev.isxander.yacl3.config.v2.api.SerialEntry
import java.awt.Color

class DonationConfig {
    @SerialEntry
    var enabled: Boolean = true // Der neue An/Aus Schalter

    @SerialEntry
    var highestDonationPlayer: String? = null
    @SerialEntry
    var highestDonationAmount: Float = 0.0F

    @SerialEntry
    var x: Float = 0.5f
    @SerialEntry
    var y: Float = 0.05f

    @SerialEntry
    var size: Float = 1.0F

    @SerialEntry
    var customTitle: String = "Highest Donation"

    @SerialEntry
    var noDonationText: String = "No Donation"

    @SerialEntry
    var roundedCorners: Boolean = true

    @SerialEntry
    var color: Int = Color.WHITE.rgb

    @SerialEntry
    var showBox: Boolean = true

    @SerialEntry
    var boxColor: Int = Color(192, 192, 192, 150).rgb


    fun getHighestDonation(): Donation? {
        val player = highestDonationPlayer ?: return null
        return Donation(player, highestDonationAmount)
    }

    fun setHighestDonation(donation: Donation) {
        highestDonationPlayer = donation.playerName
        highestDonationAmount = donation.amount
        MagicDonutConfig.HANDLER.save()
    }

    fun reset() {
        highestDonationPlayer = null
        highestDonationAmount = 0.0F
        MagicDonutConfig.HANDLER.save()
    }
}
