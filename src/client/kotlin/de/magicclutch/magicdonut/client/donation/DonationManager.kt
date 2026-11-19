package de.magicclutch.magicdonut.client.donation

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import net.minecraft.network.chat.Component

object DonationManager {
    private val config get() = MagicDonutConfig.instance.donationConfig

    // "<name> paid you $<number>[k|m|b]". The name token may start with '.' (Bedrock players),
    // the number may contain thousands separators ("1,000").
    private val regex = Regex("""(\S+) paid you \$(\d[\d,]*(?:\.\d+)?)\s*([kmbKMB]?)""")

    fun handleChatMessage(message: Component) {
        if (!MinecraftClientService.isOnDonut) return

        val match = regex.find(message.string) ?: return

        val playerName = match.groupValues[1]
        val base = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: return
        val multiplier = when (match.groupValues[3].lowercase()) {
            "k" -> 1_000.0
            "m" -> 1_000_000.0
            "b" -> 1_000_000_000.0
            else -> 1.0
        }

        val newDonation = Donation(playerName, base * multiplier)
        val currentHighest = config.getHighestDonation()
        if (currentHighest == null || newDonation.amount > currentHighest.amount) {
            config.setHighestDonation(newDonation)
        }
    }
}
