package de.magicclutch.magicdonut.client.config

import com.mojang.brigadier.CommandDispatcher
import de.magicclutch.magicdonut.client.profit.ScoreboardDebug
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.network.chat.Component
import org.polyfrost.oneconfig.api.ui.v1.OneConfigUI

class ConfigCommand {
    companion object {
        fun registerCommands(
            dispatcher: CommandDispatcher<FabricClientCommandSource>
        ) {
            dispatcher.register(
                ClientCommands.literal("magicdonututils")
                    .executes { _ ->
                        // Open on the next tick: the chat screen is still closing while a command runs.
                        MinecraftClientService.client.schedule { OneConfigUI.open() }
                        1
                    }
                    // Debug: dumps the sidebar scoreboard to the log so the money line can be located.
                    .then(
                        ClientCommands.literal("scoreboard").executes { _ ->
                            val summary = ScoreboardDebug.dump(MinecraftClientService.client)
                            MinecraftClientService.sendChatMessage(Component.literal(summary))
                            1
                        }
                    )
            )
        }
    }
}
