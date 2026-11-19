package de.magicclutch.magicdonut.client.config.category

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.service.APIService
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.LabelOption
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.controller.StringControllerBuilder
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

object DevelopmentCategory {
    fun create(defaults: MagicDonutConfig, config: MagicDonutConfig): ConfigCategory {
        val d = defaults.developmentConfig
        val c = config.developmentConfig

        val status = if (APIService.isProxyMode()) {
            Component.translatable("magicdonut.config.dev.status.proxy").withStyle(ChatFormatting.GREEN)
        } else if (APIService.hasApiKey()) {
            Component.translatable("magicdonut.config.dev.status.ok").withStyle(ChatFormatting.GREEN)
        } else {
            Component.translatable("magicdonut.config.dev.status.missing").withStyle(ChatFormatting.YELLOW)
        }

        return ConfigCategory.createBuilder()
            .name(Component.translatable("magicdonut.config.dev"))
            .tooltip(Component.translatable("magicdonut.config.dev.tooltip"))
            .option(LabelOption.create(status))
            .option(LabelOption.create(Component.translatable("magicdonut.config.dev.help")))
            .group(
                OptionGroup.createBuilder()
                    .name(Component.translatable("magicdonut.config.group.api"))
                    .option(
                        Option.createBuilder<String>()
                            .name(Component.translatable("magicdonut.config.dev.apikey"))
                            .description(OptionDescription.of(Component.translatable("magicdonut.config.dev.apikey.desc")))
                            .binding(d.apiKey, { c.apiKey }, { c.apiKey = it.trim() })
                            .controller { StringControllerBuilder.create(it) }
                            .build()
                    )
                    .option(
                        Option.createBuilder<String>()
                            .name(Component.translatable("magicdonut.config.dev.baseurl"))
                            .description(OptionDescription.of(Component.translatable("magicdonut.config.dev.baseurl.desc")))
                            .binding(d.apiBaseUrl, { c.apiBaseUrl }, { c.apiBaseUrl = it.trim() })
                            .controller { StringControllerBuilder.create(it) }
                            .build()
                    )
                    .build()
            )
            .build()
    }
}
