package de.magicclutch.magicdonut.client.donation

import de.magicclutch.magicdonut.client.auction.AuctionManager
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.payment.Payment
import de.magicclutch.magicdonut.client.payment.PaymentListener

object DonationManager {
    private val config get() = MagicDonutConfig.instance

    fun init() {
        PaymentListener.onReceived { handlePayment(it) }
    }

    private fun handlePayment(payment: Payment) {
        AuctionManager.onPayment(payment.player, payment.amount)

        val newDonation = Donation(payment.player, payment.amount)
        val currentHighest = config.highestDonation()
        if (currentHighest == null || newDonation.amount > currentHighest.amount) {
            config.recordDonation(newDonation)
        }
    }
}
