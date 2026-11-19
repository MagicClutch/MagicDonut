package de.magicclutch.magicdonut.client.config.category

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.gamble.GambleScreen
import dev.isxander.yacl3.api.ButtonOption
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.LabelOption
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.controller.LongFieldControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object GambleCategory {
    fun create(defaults: MagicDonutConfig, config: MagicDonutConfig): ConfigCategory {
        val d = defaults.gambleConfig
        val c = config.gambleConfig

        return ConfigCategory.createBuilder()
            .name(Component.translatable("magicdonut.config.gamble"))
            .tooltip(Component.translatable("magicdonut.config.gamble.tooltip"))
            .option(LabelOption.create(Component.translatable("magicdonut.config.gamble.help")))
            .group(
                OptionGroup.createBuilder()
                    .name(Component.translatable("magicdonut.config.group.simulator"))
                    .option(
                        ButtonOption.createBuilder()
                            .name(Component.translatable("magicdonut.config.gamble.open"))
                            .description(OptionDescription.of(Component.translatable("magicdonut.config.gamble.open.desc")))
                            .action { screen, _ ->
                                val client = Minecraft.getInstance()
                                client.execute { client.gui.setScreen(GambleScreen(screen)) }
                            }
                            .build()
                    )
                    .option(
                        Option.createBuilder<Long>()
                            .name(Component.translatable("magicdonut.config.gamble.starting"))
                            .description(OptionDescription.of(Component.translatable("magicdonut.config.gamble.starting.desc")))
                            .binding(d.startingBalance, { c.startingBalance }, { c.startingBalance = it.coerceAtLeast(1L) })
                            .controller { LongFieldControllerBuilder.create(it).min(1L) }
                            .build()
                    )
                    .option(
                        Option.createBuilder<Boolean>()
                            .name(Component.translatable("magicdonut.config.gamble.sound"))
                            .binding(d.soundEnabled, { c.soundEnabled }, { c.soundEnabled = it })
                            .controller { TickBoxControllerBuilder.create(it) }
                            .build()
                    )
                    .build()
            )
            .build()
    }
}
