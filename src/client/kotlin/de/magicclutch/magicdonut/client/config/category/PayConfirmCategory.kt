package de.magicclutch.magicdonut.client.config.category

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.LongFieldControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import net.minecraft.network.chat.Component

object PayConfirmCategory {
    fun create(defaults: MagicDonutConfig, config: MagicDonutConfig): ConfigCategory {
        val d = defaults.payConfirmConfig
        val c = config.payConfirmConfig

        return ConfigCategory.createBuilder()
            .name(Component.translatable("magicdonut.config.payconfirm"))
            .tooltip(Component.translatable("magicdonut.config.payconfirm.tooltip"))
            .group(
                OptionGroup.createBuilder()
                    .name(Component.translatable("magicdonut.config.group.general"))
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Component.translatable("magicdonut.config.payconfirm.enabled"))
                            .description(OptionDescription.of(Component.translatable("magicdonut.config.payconfirm.enabled.desc")))
                            .binding(d.enabled, { c.enabled }, { c.enabled = it })
                            .controller { TickBoxControllerBuilder.create(it) }
                            .build()
                    )
                    .option(
                        Option.createBuilder<Long>()
                            .name(Component.translatable("magicdonut.config.payconfirm.threshold"))
                            .description(OptionDescription.of(Component.translatable("magicdonut.config.payconfirm.threshold.desc")))
                            .binding(d.confirmThreshold, { c.confirmThreshold }, { c.confirmThreshold = it })
                            .controller { LongFieldControllerBuilder.create(it).min(1L) }
                            .build()
                    )
                    .option(
                        Option.createBuilder<Int>()
                            .name(Component.translatable("magicdonut.config.payconfirm.timeout"))
                            .description(OptionDescription.of(Component.translatable("magicdonut.config.payconfirm.timeout.desc")))
                            .binding(d.timeoutSeconds, { c.timeoutSeconds }, { c.timeoutSeconds = it })
                            .controller {
                                IntegerSliderControllerBuilder.create(it).range(10, 300).step(5)
                                    .formatValue { v -> Component.literal("${v}s") }
                            }
                            .build()
                    )
                    .build()
            )
            .build()
    }
}
