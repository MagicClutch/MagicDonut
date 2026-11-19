package de.magicclutch.magicdonut.client.config.category

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.LabelOption
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import net.minecraft.network.chat.Component

object ProfitCategory {
    fun create(defaults: MagicDonutConfig, config: MagicDonutConfig): ConfigCategory {
        val d = defaults.profitConfig
        val c = config.profitConfig

        return ConfigCategory.createBuilder()
            .name(Component.translatable("magicdonut.config.profit"))
            .tooltip(Component.translatable("magicdonut.config.profit.tooltip"))
            .option(LabelOption.create(Component.translatable("magicdonut.config.profit.help")))
            .group(
                OptionGroup.createBuilder()
                    .name(Component.translatable("magicdonut.config.group.general"))
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Component.translatable("magicdonut.config.profit.enabled"))
                            .description(OptionDescription.of(Component.translatable("magicdonut.config.profit.enabled.desc")))
                            .binding(d.enabled, { c.enabled }, { c.enabled = it })
                            .controller { TickBoxControllerBuilder.create(it) }
                            .build()
                    )
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Component.translatable("magicdonut.config.profit.balance"))
                            .binding(d.showBalance, { c.showBalance }, { c.showBalance = it })
                            .controller { TickBoxControllerBuilder.create(it) }
                            .build()
                    )
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Component.translatable("magicdonut.config.profit.today"))
                            .binding(d.showProfit, { c.showProfit }, { c.showProfit = it })
                            .controller { TickBoxControllerBuilder.create(it) }
                            .build()
                    )
                    .build()
            )
            .group(
                OptionGroup.createBuilder()
                    .name(Component.translatable("magicdonut.config.group.layout"))
                    .option(
                        Option.createBuilder<Int>()
                            .name(Component.translatable("magicdonut.config.profit.refresh"))
                            .description(OptionDescription.of(Component.translatable("magicdonut.config.profit.refresh.desc")))
                            .binding(d.refreshSeconds, { c.refreshSeconds }, { c.refreshSeconds = it })
                            .controller {
                                IntegerSliderControllerBuilder.create(it).range(10, 300).step(5)
                                    .formatValue { v -> Component.literal("${v}s") }
                            }
                            .build()
                    )
                    .option(
                        Option.createBuilder<Int>()
                            .name(Component.translatable("magicdonut.config.profit.offset"))
                            .binding(d.hudBottomOffset, { c.hudBottomOffset }, { c.hudBottomOffset = it })
                            .controller { IntegerSliderControllerBuilder.create(it).range(20, 200).step(2) }
                            .build()
                    )
                    .build()
            )
            .build()
    }
}
