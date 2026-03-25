package de.magicclutch.magicdonut.client.donation

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import net.minecraft.network.chat.Component

object DonationManager {
    private val config get() = MagicDonutConfig.instance.donationConfig

    // Neuer Regex: Sucht nach Zahl + optionalem Suffix (k, m, b)
    // Capture the full name token (Bedrock players can have a leading '.', e.g. ".name").
    private val regex = Regex("([^\\s]+) paid you \\$(\\d+(?:\\.\\d+)?)\\s*([kmbKMB]?)")

    fun handleChatMessage(message: Component) {
        val textContent = message.string
        val match = regex.find(textContent) ?: return

        val playerName = match.groupValues[1]
        var amount = match.groupValues[2].toFloat()
        val suffix = match.groupValues[3].lowercase()

        amount *= when (suffix) {
            "k" -> 1_000.0
            "m" -> 1_000_000.0
            "b" -> 1_000_000_000.0
            else -> 1.0
        }.toFloat()

        val newDonation = Donation(playerName, amount)
        val currentHighest = config.getHighestDonation()
        if (currentHighest == null || newDonation.amount > currentHighest.amount) {
            config.setHighestDonation(newDonation)
        }
    }
}
