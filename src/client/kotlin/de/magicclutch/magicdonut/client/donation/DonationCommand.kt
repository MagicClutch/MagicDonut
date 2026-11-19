package de.magicclutch.magicdonut.client.donation

import com.mojang.brigadier.CommandDispatcher
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandBuildContext
import net.minecraft.network.chat.Component

object DonationCommand {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess ->
            registerCommands(dispatcher, registryAccess)
        }
    }

    private fun registerCommands(
        dispatcher: CommandDispatcher<FabricClientCommandSource>,
        registryAccess: CommandBuildContext
    ) {
        dispatcher.register(
            literal("donation")
                .then(literal("reset").executes { context ->
                    MagicDonutConfig.instance.donationConfig.reset()

                    context.source.sendFeedback(Component.literal("[MagicDonut] Donation-HUD reset!").withStyle(ChatFormatting.GREEN))
                    1
                })
        )
    }
}
