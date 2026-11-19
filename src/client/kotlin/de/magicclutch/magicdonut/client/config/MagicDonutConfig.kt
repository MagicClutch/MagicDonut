package de.magicclutch.magicdonut.client.config

import com.google.gson.GsonBuilder
import de.magicclutch.magicdonut.client.config.category.DevelopmentCategory
import de.magicclutch.magicdonut.client.config.category.DonationCategory
import de.magicclutch.magicdonut.client.config.category.OverheadStatsCategory
import de.magicclutch.magicdonut.client.config.category.PayConfirmCategory
import de.magicclutch.magicdonut.client.config.configuration.DevelopmentConfig
import de.magicclutch.magicdonut.client.config.configuration.DonationConfig
import de.magicclutch.magicdonut.client.config.configuration.OverheadStatsConfig
import de.magicclutch.magicdonut.client.config.configuration.PayConfirmConfig
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler
import dev.isxander.yacl3.config.v2.api.SerialEntry
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class MagicDonutConfig {
    companion object {
        val HANDLER: ConfigClassHandler<MagicDonutConfig> =
            ConfigClassHandler.createBuilder<MagicDonutConfig>(MagicDonutConfig::class.java)
                .serializer { config ->
                    GsonConfigSerializerBuilder.create<MagicDonutConfig>(config)
                        .setPath(FabricLoader.getInstance().configDir.resolve("magicdonut.json5"))
                        .appendGsonBuilder(GsonBuilder::setPrettyPrinting)
                        .setJson5(true)
                        .build()
                }
                .build()

        val instance: MagicDonutConfig
            get() = HANDLER.instance()

        init {
            HANDLER.load()
        }

        fun createGUI(parent: Screen?): Screen {
            return YetAnotherConfigLib.create<MagicDonutConfig>(
                HANDLER
            ) { defaults: MagicDonutConfig, config: MagicDonutConfig, builder: YetAnotherConfigLib.Builder ->
                builder.title(Component.literal("MagicDonutUtils Config"))
                    .category(PayConfirmCategory.create(defaults, config))
                    .category(DonationCategory.create(defaults, config))
                    .category(OverheadStatsCategory.create(defaults, config))
                    .category(DevelopmentCategory.create(defaults, config))


            }.generateScreen(parent)
        }
    }

    @SerialEntry
    var developmentConfig: DevelopmentConfig = DevelopmentConfig()

    @SerialEntry
    var payConfirmConfig: PayConfirmConfig = PayConfirmConfig()

    @SerialEntry
    var donationConfig: DonationConfig = DonationConfig()

    @SerialEntry
    var overheadStatsConfig: OverheadStatsConfig = OverheadStatsConfig()
}
