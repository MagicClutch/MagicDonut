package de.magicclutch.magicdonut.client.profit

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.util.AmountParser
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import kotlin.math.abs

/** /dailybalance [start|reset]  (also available as /donutbalance) */
object ProfitCommand {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            for (name in listOf("dailybalance", "donutbalance")) {
                dispatcher.register(
                    ClientCommands.literal(name)
                        .executes { status(it.source) }
                        .then(ClientCommands.literal("start").executes { start(it) })
                        .then(ClientCommands.literal("reset").executes { reset(it) })
                )
            }
        }
    }

    private fun status(source: FabricClientCommandSource): Int {
        val balance = ProfitTracker.currentBalance
        if (balance == null) {
            error(source, "Balance not found yet. Join DonutSMP; it is read from the tab list footer.")
            return Command.SINGLE_SUCCESS
        }
        info(source, "Balance: $${AmountParser.formatShort(balance)}")
        val profit = ProfitTracker.profitToday
        if (profit != null) {
            val sign = if (profit >= 0) "+" else "-"
            info(source, "Today: $sign$${AmountParser.formatShort(abs(profit))} (since $${AmountParser.formatShort(MagicDonutConfig.instance.profitStartBalance)})")
        }
        info(source, "Use /dailybalance start to set today's starting point to your current balance.")
        return Command.SINGLE_SUCCESS
    }

    private fun start(context: CommandContext<FabricClientCommandSource>): Int {
        if (ProfitTracker.startToday()) {
            info(context.source, "Starting point saved. Today's profit is now $0.")
        } else {
            error(context.source, "Balance not found yet, try again in a moment.")
        }
        return Command.SINGLE_SUCCESS
    }

    private fun reset(context: CommandContext<FabricClientCommandSource>): Int {
        ProfitTracker.resetBaseline()
        info(context.source, "Starting point cleared. The next balance refresh becomes the new starting point.")
        return Command.SINGLE_SUCCESS
    }


    private fun info(source: FabricClientCommandSource, text: String) {
        source.sendFeedback(
            Component.literal("[Balance] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(text).withStyle(ChatFormatting.GRAY))
        )
    }

    private fun error(source: FabricClientCommandSource, text: String) {
        source.sendError(Component.literal(text).withStyle(ChatFormatting.RED))
    }
}
