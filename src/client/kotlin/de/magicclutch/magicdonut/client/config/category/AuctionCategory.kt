package de.magicclutch.magicdonut.client.config.category

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.LabelOption
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.LongFieldControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import net.minecraft.network.chat.Component

object AuctionCategory {
    fun create(defaults: MagicDonutConfig, config: MagicDonutConfig): ConfigCategory {
        val d = defaults.auctionConfig
        val c = config.auctionConfig

        return ConfigCategory.createBuilder()
            .name(Component.translatable("magicdonut.config.auction"))
            .tooltip(Component.translatable("magicdonut.config.auction.tooltip"))
            .option(LabelOption.create(Component.translatable("magicdonut.config.auction.help")))
            .group(
                OptionGroup.createBuilder()
                    .name(Component.translatable("magicdonut.config.group.general"))
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Component.translatable("magicdonut.config.auction.enabled"))
                            .description(OptionDescription.of(Component.translatable("magicdonut.config.auction.enabled.desc")))
                            .binding(d.enabled, { c.enabled }, { c.enabled = it })
                            .controller { TickBoxControllerBuilder.create(it) }
                            .build()
                    )
                    .option(
                        Option.createBuilder<Int>()
                            .name(Component.translatable("magicdonut.config.auction.duration"))
                            .description(OptionDescription.of(Component.translatable("magicdonut.config.auction.duration.desc")))
                            .binding(d.defaultDurationSeconds, { c.defaultDurationSeconds }, { c.defaultDurationSeconds = it })
                            .controller {
                                IntegerSliderControllerBuilder.create(it).range(10, 900).step(10)
                                    .formatValue { v -> Component.literal("${v}s") }
                            }
                            .build()
                    )
                    .option(
                        Option.createBuilder<Long>()
                            .name(Component.translatable("magicdonut.config.auction.minbid"))
                            .description(OptionDescription.of(Component.translatable("magicdonut.config.auction.minbid.desc")))
                            .binding(d.defaultMinimumBid, { c.defaultMinimumBid }, { c.defaultMinimumBid = it.coerceAtLeast(1L) })
                            .controller { LongFieldControllerBuilder.create(it).min(1L) }
                            .build()
                    )
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Component.translatable("magicdonut.config.auction.sound"))
                            .binding(d.playBidSound, { c.playBidSound }, { c.playBidSound = it })
                            .controller { TickBoxControllerBuilder.create(it) }
                            .build()
                    )
                    .build()
            )
            .group(
                OptionGroup.createBuilder()
                    .name(Component.translatable("magicdonut.config.group.hologram"))
                    .option(
                        Option.createBuilder<Float>()
                            .name(Component.translatable("magicdonut.config.auction.x"))
                            .binding(d.x, { c.x }, { c.x = it })
                            .controller { FloatSliderControllerBuilder.create(it).range(0f, 1f).step(0.01f) }
                            .build()
                    )
                    .option(
                        Option.createBuilder<Float>()
                            .name(Component.translatable("magicdonut.config.auction.y"))
                            .binding(d.y, { c.y }, { c.y = it })
                            .controller { FloatSliderControllerBuilder.create(it).range(0f, 1f).step(0.01f) }
                            .build()
                    )
                    .option(
                        Option.createBuilder<Float>()
                            .name(Component.translatable("magicdonut.config.auction.scale"))
                            .binding(d.scale, { c.scale }, { c.scale = it })
                            .controller { FloatSliderControllerBuilder.create(it).range(0.5f, 3f).step(0.1f) }
                            .build()
                    )
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Component.translatable("magicdonut.config.auction.bobbing"))
                            .binding(d.bobbingItem, { c.bobbingItem }, { c.bobbingItem = it })
                            .controller { TickBoxControllerBuilder.create(it) }
                            .build()
                    )
                    .build()
            )
            .build()
    }
}
