package de.magicclutch.magicdonut.client.config.category

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.config.configuration.OverheadStatsConfig
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.controller.CyclingListControllerBuilder
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import net.minecraft.network.chat.Component

object OverheadStatsCategory {
    fun create(defaults: MagicDonutConfig, config: MagicDonutConfig): ConfigCategory {
        val allowedStats = listOf(
            OverheadStatsConfig.StatField.MONEY,
            OverheadStatsConfig.StatField.DEATHS,
            OverheadStatsConfig.StatField.KILLS,
            OverheadStatsConfig.StatField.SHARDS,
            OverheadStatsConfig.StatField.PLAYTIME
        )

        // If the config contains an old/hidden enum value, the cycling controller can crash.
        // Normalize to a selectable value before building the UI.
        if (config.overheadStatsConfig.stat !in allowedStats) {
            config.overheadStatsConfig.stat = OverheadStatsConfig.StatField.MONEY
        }

        val enabled = Option.createBuilder<Boolean>()
            .name(Component.literal("Enable Overhead Stats"))
            .description(OptionDescription.of(Component.literal("Show a chosen stat above your head.")))
            .binding(defaults.overheadStatsConfig.enabled, { config.overheadStatsConfig.enabled }, { config.overheadStatsConfig.enabled = it })
            .controller { TickBoxControllerBuilder.create(it) }
            .build()

        val displayModeOption = Option.createBuilder<OverheadStatsConfig.DisplayMode>()
            .name(Component.literal("Display Mode"))
            .description(OptionDescription.of(Component.literal("Fixed shows one stat. Cycle rotates through stats automatically.")))
            .binding(defaults.overheadStatsConfig.displayMode, { config.overheadStatsConfig.displayMode }, { config.overheadStatsConfig.displayMode = it })
            .controller {
                CyclingListControllerBuilder.create(it)
                    .values(OverheadStatsConfig.DisplayMode.entries)
                    .valueFormatter { v -> Component.literal(v.name.lowercase().replaceFirstChar { c -> c.uppercase() }) }
            }
            .build()

        val cycleSecondsOption = Option.createBuilder<Int>()
            .name(Component.literal("Cycle Seconds"))
            .description(OptionDescription.of(Component.literal("How long each stat is shown when in Cycle mode.")))
            .binding(defaults.overheadStatsConfig.cycleSeconds, { config.overheadStatsConfig.cycleSeconds }, { config.overheadStatsConfig.cycleSeconds = it })
            .controller { opt -> IntegerSliderControllerBuilder.create(opt).range(1, 30).step(1) }
            .build()

        val statOption = Option.createBuilder<OverheadStatsConfig.StatField>()
            .name(Component.literal("Displayed Stat"))
            .description(OptionDescription.of(Component.literal("Select which stat to render overhead.")))
            .binding(defaults.overheadStatsConfig.stat, { config.overheadStatsConfig.stat }, { config.overheadStatsConfig.stat = it })
            .controller {
                CyclingListControllerBuilder.create(it)
                    .values(allowedStats)
                    .valueFormatter { v -> Component.literal(v.toString()) }
            }
            .build()

        val yOffsetOption = Option.createBuilder<Float>()
            .name(Component.literal("Stat Y Offset"))
            .description(OptionDescription.of(Component.literal("Vertical offset relative to the username. Negative is below, positive is above.")))
            .binding(defaults.overheadStatsConfig.statYOffset, { config.overheadStatsConfig.statYOffset }, { config.overheadStatsConfig.statYOffset = it })
            .controller { opt -> FloatSliderControllerBuilder.create(opt).range(0f, 4.0f).step(0.05f) }
            .build()

        val scaleOption = Option.createBuilder<Float>()
            .name(Component.literal("Stat Scale"))
            .description(OptionDescription.of(Component.literal("Scale factor for the stat text (1.0 = vanilla size).")))
            .binding(defaults.overheadStatsConfig.statScale, { config.overheadStatsConfig.statScale }, { config.overheadStatsConfig.statScale = it })
            .controller { opt -> FloatSliderControllerBuilder.create(opt).range(0.25f, 2.0f).step(0.05f) }
            .build()


        return ConfigCategory.createBuilder()
            .name(Component.literal("Overhead Stats"))
            .group(
                OptionGroup.createBuilder()
                    .name(Component.literal("Display"))
                    .option(enabled)
                    .option(displayModeOption)
                    .option(cycleSecondsOption)
                    .option(statOption)
                    .option(yOffsetOption)
                    .option(scaleOption)
                    .build()
            )
            .build()
    }
}
