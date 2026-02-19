package de.magicclutch.magicdonut.client.config.category

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.donation.PositionEditScreen
import dev.isxander.yacl3.api.ButtonOption
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.controller.*
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.awt.Color

object DonationCategory {
    fun create(defaults: MagicDonutConfig, config: MagicDonutConfig): ConfigCategory {

        // --- GENERAL OPTIONS ---
        val enabledOption = Option.createBuilder<Boolean>()
            .name(Component.translatable("Enabled"))
            .description(OptionDescription.of(Component.translatable("Show or hide the donation overlay.")))
            .binding(
                defaults.donationConfig.enabled,
                { config.donationConfig.enabled },
                { config.donationConfig.enabled = it }
            )
            .controller { TickBoxControllerBuilder.create(it) }
            .build()

        // --- POSITION & SIZE ---
        val sizeOption = Option.createBuilder<Float>()
            .name(Component.translatable("Size"))
            .binding(defaults.donationConfig.size, { config.donationConfig.size }, { config.donationConfig.size = it })
            .controller { FloatSliderControllerBuilder.create(it).range(0.5f, 3.0f).step(0.1f) }
            .build()

        val openEditorOption = ButtonOption.createBuilder()
            .name(Component.translatable("Open Editor"))
            .description(OptionDescription.of(Component.translatable("Open Editor")))
            .action { _, _ ->
                val client = Minecraft.getInstance()
                client.execute { client.setScreen(PositionEditScreen()) }
            }
            .build()

        // --- TEXT OPTIONS ---
        val titleOption = Option.createBuilder<String>()
            .name(Component.translatable("Prefix"))
            .binding(defaults.donationConfig.customTitle, { config.donationConfig.customTitle }, { config.donationConfig.customTitle = it })
            .controller { StringControllerBuilder.create(it) }
            .build()

        val colorOption = Option.createBuilder<Color>()
            .name(Component.translatable("Color"))
            .binding(
                Color(defaults.donationConfig.color),
                { Color(config.donationConfig.color) },
                { config.donationConfig.color = it.rgb }
            )
            .controller { ColorControllerBuilder.create(it) }
            .build()

        val noDonationOption = Option.createBuilder<String>()
            .name(Component.translatable("No Donation Text"))
            .binding(
                defaults.donationConfig.noDonationText,
                { config.donationConfig.noDonationText },
                { config.donationConfig.noDonationText = it }
            )
            .controller { StringControllerBuilder.create(it) }
            .build()

        // --- BOX OPTIONS ---
        val boxToggleOption = Option.createBuilder<Boolean>()
            .name(Component.translatable("Toggle Box"))
            .binding(defaults.donationConfig.showBox, { config.donationConfig.showBox }, { config.donationConfig.showBox = it })
            .controller { TickBoxControllerBuilder.create(it) }
            .build()

        val roundedOption = Option.createBuilder<Boolean>()
            .name(Component.translatable("Rounded"))
            .binding(
                defaults.donationConfig.roundedCorners,
                { config.donationConfig.roundedCorners },
                { config.donationConfig.roundedCorners = it }
            )
            .controller { TickBoxControllerBuilder.create(it) }
            .build()

        val boxColorOption = Option.createBuilder<Color>()
            .name(Component.translatable("Box Color"))
            .binding(
                Color(defaults.donationConfig.boxColor, true),
                { Color(config.donationConfig.boxColor, true) },
                { config.donationConfig.boxColor = it.rgb }
            )
            .controller { ColorControllerBuilder.create(it).allowAlpha(true) }
            .build()

        // --- BUILD CATEGORY ---
        return ConfigCategory.createBuilder()
            .name(Component.translatable("magicdonut.config.donation"))
            // Hier habe ich die neue Gruppe "General" hinzugefügt
            .group(OptionGroup.createBuilder()
                .name(Component.translatable("General"))
                .option(enabledOption)
                .build())
            .group(OptionGroup.createBuilder()
                .name(Component.translatable("Layout"))
                .option(sizeOption)
                .option(openEditorOption)
                .build())
            .group(OptionGroup.createBuilder()
                .name(Component.translatable("Appearance"))
                .option(titleOption)
                .option(colorOption)
                .option(noDonationOption)
                .build())
            .group(OptionGroup.createBuilder()
                .name(Component.translatable("Box Settings"))
                .option(boxToggleOption)
                .option(roundedOption)
                .option(boxColorOption)
                .build())
            .build()
    }
}