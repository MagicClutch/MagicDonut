package de.magicclutch.magicdonut.client.gamble

import com.mojang.brigadier.Command
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands

object GambleCommand {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("gamble").executes {
                    val client = MinecraftClientService.client
                    // The chat screen closes right after a command runs, so open ours on the next tick.
                    client.schedule { client.gui.setScreen(GambleScreen()) }
                    Command.SINGLE_SUCCESS
                }
            )
        }
    }
}
