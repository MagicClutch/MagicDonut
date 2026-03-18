package de.magicclutch.magicdonut.client.config.category

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.StringControllerBuilder
import net.minecraft.network.chat.Component

class DevelopmentCategory {
    companion object {
        fun create(defaults: MagicDonutConfig, config: MagicDonutConfig): ConfigCategory {
            return ConfigCategory.createBuilder()
                .name(Component.literal("Dev"))
                .tooltip(Component.literal("This is only meant for developers who know what they're doing."))
                .option(Option.createBuilder<String>()
                    .name(Component.literal("API Key"))
                    .description(OptionDescription.of(Component.literal("The DonutSMP API key.")))
                    .binding(
                        defaults.developmentConfig.apiKey,
                        { config.developmentConfig.apiKey },
                        { newVal -> config.developmentConfig.apiKey = newVal }
                    )
                    .controller { StringControllerBuilder.create(it) }
                    .build())
                .group(OptionGroup.createBuilder()
                    .name(Component.literal("API Settings"))
                    .option(Option.createBuilder<Int>()
                        .name(Component.literal("Overhead Stats Refresh Rate"))
                        .description(OptionDescription.of(Component.literal("Refresh rate in seconds")))
                        .binding(
                            defaults.developmentConfig.overheadStatsRefreshRate,
                            { config.developmentConfig.overheadStatsRefreshRate },
                            { newVal -> config.developmentConfig.overheadStatsRefreshRate = newVal }
                        )
                        .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(1, 60).step(1) }
                        .build())
                    .build())
                .build()
        }
    }
}
