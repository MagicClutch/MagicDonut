package de.magicclutch.magicdonut.client.config.category

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.LongFieldControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import net.minecraft.network.chat.Component

class PayConfirmCategory {
    companion object {
        fun create(defaults: MagicDonutConfig, config: MagicDonutConfig): ConfigCategory {
            return ConfigCategory.createBuilder()
                .name(Component.literal("Pay Confirm"))
                .tooltip(Component.literal("Payment Savety"))

                // Toggle Switch
                .option(Option.createBuilder<Boolean>()
                    .name(Component.literal("Active"))
                    .binding(defaults.payConfirmConfig.enabled, { config.payConfirmConfig.enabled }, { config.payConfirmConfig.enabled = it })
                    .controller { TickBoxControllerBuilder.create(it) }
                    .build())

                // Eingabefeld für den Betrag (Long)
                .option(Option.createBuilder<Long>()
                    .name(Component.literal("Pay amount"))
                    .description(OptionDescription.of(Component.literal("You must confirm if you pass this amount.")))
                    .binding(defaults.payConfirmConfig.confirmThreshold, { config.payConfirmConfig.confirmThreshold }, { config.payConfirmConfig.confirmThreshold = it })
                    .controller { LongFieldControllerBuilder.create(it) }
                    .build())

                // Slider für das Zeitlimit
                .option(Option.createBuilder<Int>()
                    .name(Component.literal("Timeout (sec)"))
                    .binding(defaults.payConfirmConfig.timeoutSeconds, { config.payConfirmConfig.timeoutSeconds }, { config.payConfirmConfig.timeoutSeconds = it })
                    .controller { IntegerSliderControllerBuilder.create(it).range(10, 300).step(5) }
                    .build())
                .build()
        }
    }
}