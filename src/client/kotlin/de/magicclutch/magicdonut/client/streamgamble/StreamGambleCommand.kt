package de.magicclutch.magicdonut.client.streamgamble

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import de.magicclutch.magicdonut.client.util.AmountParser
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

/** /streamgamble test <name> <amount> | skip | clear | status */
object StreamGambleCommand {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("streamgamble")
                    .executes { status(it.source) }
                    .then(ClientCommands.literal("status").executes { status(it.source) })
                    .then(
                        ClientCommands.literal("test")
                            .then(
                                ClientCommands.argument("name", StringArgumentType.word())
                                    .then(
                                        ClientCommands.argument("amount", StringArgumentType.word()).executes {
                                            test(
                                                it.source,
                                                StringArgumentType.getString(it, "name"),
                                                StringArgumentType.getString(it, "amount")
                                            )
                                        }
                                    )
                            )
                    )
                    .then(ClientCommands.literal("skip").executes {
                        StreamGambleManager.skip()
                        info(it.source, "Skipped the current round.")
                        Command.SINGLE_SUCCESS
                    })
                    .then(ClientCommands.literal("clear").executes {
                        StreamGambleManager.clear()
                        info(it.source, "Cleared the current round and the queue.")
                        Command.SINGLE_SUCCESS
                    })
            )
        }
    }

    private fun test(source: FabricClientCommandSource, name: String, amountText: String): Int {
        val amount = AmountParser.parse(amountText)
        if (amount == null) {
            source.sendError(Component.literal("Unknown amount: $amountText").withStyle(ChatFormatting.RED))
            return Command.SINGLE_SUCCESS
        }
        StreamGambleManager.enqueue(name, amount)
        info(source, "Test round queued for $name ($${AmountParser.formatShort(amount)}). The overlay must be enabled in the settings.")
        return Command.SINGLE_SUCCESS
    }

    private fun status(source: FabricClientCommandSource): Int {
        val hostPct = (StreamGambleManager.hostChance() * 100).toInt()
        val current = StreamGambleManager.current
        info(source, "Odds: host $hostPct% / player ${100 - hostPct}%. " +
            (if (current != null) "Running: ${current.player} ($${AmountParser.formatShort(current.stake)}). " else "No round running. ") +
            "${StreamGambleManager.queued} waiting.")
        return Command.SINGLE_SUCCESS
    }

    private fun info(source: FabricClientCommandSource, text: String) {
        source.sendFeedback(
            Component.literal("[Stream Gamble] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(text).withStyle(ChatFormatting.GRAY))
        )
    }
}
