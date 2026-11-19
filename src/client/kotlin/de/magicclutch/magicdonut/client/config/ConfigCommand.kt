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
                        // The chat screen is usually already closed when a command runs, so the parent may be null.
                        val parent = client.gui.screen()
                        client.schedule {
                            client.gui.setScreen(MagicDonutConfig.createGUI(parent))
                        }
                        1
                    }
            )
        }
    }
}
