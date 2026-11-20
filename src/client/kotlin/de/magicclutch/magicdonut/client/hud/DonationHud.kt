package de.magicclutch.magicdonut.client.hud

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.donation.Donation
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.TextHud

/** Your highest received donation. Position, size, color and background come from the OneConfig HUD editor. */
class DonationHud : TextHud("magicdonut_donation", "Donation", Hud.Category.INFO, "Highest Donation: ", "") {
    override fun getText(): String {
        val donation = MagicDonutConfig.instance.highestDonation() ?: return "No Donation"
        return "${donation.playerName} : ${Donation.formatMoney(donation.amount)}"
    }
}
