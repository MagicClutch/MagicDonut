package de.magicclutch.magicdonut.client

import de.magicclutch.magicdonut.client.config.ConfigCommand
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.service.APIService
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.network.chat.Component

class MagicDonutClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ -> ConfigCommand.registerCommands(dispatcher) }
        ClientReceiveMessageEvents.GAME.register { component, _ ->
            val content = component.string

            val matcher = APIService.apiTokenPattern.matcher(content)
            if(matcher.find()) {
                val apiKey = matcher.group(2)
                MagicDonutConfig.instance.developmentConfig.apiKey = apiKey
                MagicDonutConfig.HANDLER.save()
                println("API Key $apiKey saved.")
                MinecraftClientService.client.player
                MinecraftClientService.sendChatMessage(
                    Component.literal("§a[MagicDonut] API-Key saved ✓")
                )
            }
        }
    }
}
