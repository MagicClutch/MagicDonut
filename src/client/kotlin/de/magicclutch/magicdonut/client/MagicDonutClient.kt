package de.magicclutch.magicdonut.client

import com.mojang.blaze3d.platform.InputConstants
import de.magicclutch.magicdonut.client.config.ConfigCommand
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.donation.DonationCommand
import de.magicclutch.magicdonut.client.donation.DonationManager
import de.magicclutch.magicdonut.client.donation.DonationRenderer
import de.magicclutch.magicdonut.client.overheadstats.StatsRenderer
import de.magicclutch.magicdonut.client.payconfirm.PayConfirmClient
import de.magicclutch.magicdonut.client.service.APIService
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.KeyMapping
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class MagicDonutClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ -> ConfigCommand.registerCommands(dispatcher) }

        ClientReceiveMessageEvents.GAME.register { component, _ ->
            val content = component.string

            if(MagicDonutConfig.instance.developmentConfig.apiKey.isNotBlank() || !MinecraftClientService.isOnDonut) {
                return@register
            }

            val matcher = APIService.apiTokenPattern.matcher(content)
            if(matcher.find()) {
                val apiKey = matcher.group(2)
                MagicDonutConfig.instance.developmentConfig.apiKey = apiKey
                MagicDonutConfig.HANDLER.save()
                println("API Key $apiKey saved.")
                MinecraftClientService.client.player
                MinecraftClientService.sendChatMessage(
                    Component.literal("§a[MagicDonutUtils] API-Key saved ✓")
                )
            }
        }

        StatsRenderer().init()
        PayConfirmClient.init()
        DonationCommand.register()
        DonationRenderer.register()
        ClientReceiveMessageEvents.GAME.register { message, _ ->
            DonationManager.handleChatMessage(message)
        }
    }

    companion object {
        private val category = "MagicDonut"
        val payButtonBinding: KeyMapping = KeyBindingHelper.registerKeyBinding(
            KeyMapping(
                "magicdonut.quickpay",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_CONTROL,
                category
            )
        )
    }
}
