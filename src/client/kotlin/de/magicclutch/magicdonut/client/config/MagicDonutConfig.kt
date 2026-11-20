package de.magicclutch.magicdonut.client.config

import de.magicclutch.magicdonut.client.config.configuration.AuctionConfig
import de.magicclutch.magicdonut.client.config.configuration.DevelopmentConfig
import de.magicclutch.magicdonut.client.config.configuration.OverheadStatsConfig
import de.magicclutch.magicdonut.client.config.configuration.PayConfirmConfig
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.ConfigEntrypoint
import org.polyfrost.oneconfig.api.config.v1.annotations.Accordion
import de.magicclutch.magicdonut.client.donation.Donation

/**
 * All settings of the mod, managed by OneConfig (config file and settings screen).
 *
 * Each `@Accordion` field below only tells OneConfig which section classes to show. OneConfig creates its own
 * instance of every section; the sections remember the instance that is bound to the file (`live`), and the
 * accessors below hand that one out.
 */
class MagicDonutConfig private constructor() :
    Config("magicdonututils", "/assets/magicdonut/icon.png", "MagicDonutUtils", Category.QOL) {

    @Accordion(title = "Pay Confirm", category = "Pay Confirm", description = "Confirm big payments")
    @Suppress("unused")
    private val payConfirm = PayConfirmConfig()

    @Accordion(title = "Overhead Stats", category = "Overhead Stats", description = "Stats above name tags")
    @Suppress("unused")
    private val overheadStats = OverheadStatsConfig()

    @Accordion(title = "Auction", category = "Auction", description = "Hologram auctions. /bid start while holding an item.")
    @Suppress("unused")
    private val auction = AuctionConfig()

    @Accordion(title = "Developer", category = "Developer", description = "API key and proxy")
    @Suppress("unused")
    private val development = DevelopmentConfig()

    val payConfirmConfig: PayConfirmConfig get() = PayConfirmConfig.live
    val overheadStatsConfig: OverheadStatsConfig get() = OverheadStatsConfig.live
    val auctionConfig: AuctionConfig get() = AuctionConfig.live
    val developmentConfig: DevelopmentConfig get() = DevelopmentConfig.live

    // Stored data of the HUDs. Kept in a separate file (see DataStore) so nothing extra shows up in the settings screen.
    var profitStartBalance: Double
        get() = DataStore.data.profitStartBalance
        set(value) {
            DataStore.data.profitStartBalance = value
            DataStore.save()
        }

    var profitStartDate: String
        get() = DataStore.data.profitStartDate
        set(value) {
            DataStore.data.profitStartDate = value
            DataStore.save()
        }

    fun highestDonation(): Donation? {
        val player = DataStore.data.highestDonationPlayer ?: return null
        return Donation(player, DataStore.data.highestDonationAmount)
    }

    fun recordDonation(donation: Donation) {
        DataStore.data.highestDonationPlayer = donation.playerName
        DataStore.data.highestDonationAmount = donation.amount
        DataStore.save()
    }

    fun resetDonation() {
        DataStore.data.highestDonationPlayer = null
        DataStore.data.highestDonationAmount = 0.0
        DataStore.save()
    }

    companion object {
        val instance: MagicDonutConfig by lazy { MagicDonutConfig() }

        fun save() = instance.save()
    }
}

/** Registered as the `oneconfig:config` entrypoint in fabric.mod.json. */
class MagicDonutConfigEntrypoint : ConfigEntrypoint {
    override fun registerConfigs(context: ConfigEntrypoint.Context) {
        context.register(MagicDonutConfig.instance)
    }
}
