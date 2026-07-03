package de.magicclutch.magicdonut.client.config

import com.mojang.brigadier.CommandDispatcher
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

class ConfigCommand {
    companion object {
        fun registerCommands(
            dispatcher: CommandDispatcher<FabricClientCommandSource>
        ) {
            dispatcher.register(
                ClientCommands.literal("magicdonututils")
                    .executes { _ ->
                        val client = MinecraftClientService.client
                        val currentScreen = client.screen

                        if (currentScreen != null) {
                            client.schedule {
                                client.setScreen(
                                    MagicDonutConfig.createGUI(currentScreen)
                                )
                            }
                        }
                        1
                    }
            )
        }
    }
}
