package de.magicclutch.magicdonut.client.config.category

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.config.configuration.OverheadStatsConfig
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.controller.EnumControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import net.minecraft.network.chat.Component

object OverheadStatsCategory {
    fun create(defaults: MagicDonutConfig, config: MagicDonutConfig): ConfigCategory {
        val enabled = Option.createBuilder<Boolean>()
            .name(Component.literal("Enable Overhead Stats"))
            .description(OptionDescription.of(Component.literal("Show a chosen stat above your head.")))
            .binding(defaults.overheadStatsConfig.enabled, { config.overheadStatsConfig.enabled }, { config.overheadStatsConfig.enabled = it })
            .controller { TickBoxControllerBuilder.create(it) }
            .build()

        val statOption = Option.createBuilder<OverheadStatsConfig.StatField>()
            .name(Component.literal("Displayed Stat"))
            .description(OptionDescription.of(Component.literal("Select which stat to render overhead.")))
            .binding(defaults.overheadStatsConfig.stat, { config.overheadStatsConfig.stat }, { config.overheadStatsConfig.stat = it })
            .controller { EnumControllerBuilder.create(it).enumClass(OverheadStatsConfig.StatField::class.java) }
            .build()

        return ConfigCategory.createBuilder()
            .name(Component.literal("Overhead Stats"))
            .group(
                OptionGroup.createBuilder()
                    .name(Component.literal("Display"))
                    .option(enabled)
                    .option(statOption)
                    .build()
            )
            .build()
    }
}
