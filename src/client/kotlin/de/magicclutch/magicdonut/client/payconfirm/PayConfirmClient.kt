package de.magicclutch.magicdonut.client.payconfirm

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import de.magicclutch.magicdonut.client.MagicDonutClient
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import de.magicclutch.magicdonut.client.util.AmountParser
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player

object PayConfirmClient {
    private var pendingCommand: String? = null
    private var pendingDeadlineMs: Long = 0L

    fun init() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("pay")
                    .executes { context ->
                        context.source.sendFeedback(
                            Component.literal("Syntax: /pay <player> <amount>").withStyle(ChatFormatting.RED)
                        )
                        Command.SINGLE_SUCCESS
                    }
                    .then(
                        ClientCommands.argument("target", StringArgumentType.word())
                            .then(
                                ClientCommands.argument("amount", StringArgumentType.string())
                                    .executes { context ->
                                        handlePay(
                                            context.source,
                                            StringArgumentType.getString(context, "target"),
                                            StringArgumentType.getString(context, "amount")
                                        )
                                    }
                            )
                    )
            )

            dispatcher.register(
                ClientCommands.literal("payconfirm").executes { context ->
                    val command = pendingCommand
                    if (command != null) {
                        cancelPending()
                        sendCommand(command)
                    } else {
                        context.source.sendError(Component.literal("No payment pending.").withStyle(ChatFormatting.RED))
                    }
                    Command.SINGLE_SUCCESS
                }
            )

            dispatcher.register(
                ClientCommands.literal("paycancel").executes { context ->
                    if (pendingCommand != null) {
                        cancelPending()
                        context.source.sendFeedback(
                            Component.literal("Payment cancelled.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                        )
                    } else {
                        context.source.sendError(Component.literal("There are no payments in process.").withStyle(ChatFormatting.RED))
                    }
                    Command.SINGLE_SUCCESS
                }
            )
        }

        // Expire pending payments on the client thread (no background timers needed).
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (pendingCommand == null) return@register
            if (client.level == null) {
                // Never let a pending payment survive a disconnect / server switch.
                cancelPending()
            } else if (System.currentTimeMillis() >= pendingDeadlineMs) {
                cancelPending()
                MinecraftClientService.sendChatMessage(
                    Component.literal("⌛ Payment expired.").withStyle(ChatFormatting.RED)
                )
            }
        }

        UseEntityCallback.EVENT.register { _, _, _, entity, _ ->
            if (!MagicDonutClient.payButtonBinding.isDown) return@register InteractionResult.PASS

            val target = entity as? Player ?: return@register InteractionResult.PASS

            MinecraftClientService.client.gui.setScreen(ChatScreen("/pay ${target.name.string} ", false))

            InteractionResult.SUCCESS
        }
    }

    private fun handlePay(source: FabricClientCommandSource, target: String, amountStr: String): Int {
        // This client command shadows /pay everywhere; on other servers just pass it through untouched.
        if (!MinecraftClientService.isOnDonut) {
            sendCommand("pay $target $amountStr")
            return Command.SINGLE_SUCCESS
        }

        val amount = AmountParser.parse(amountStr)
        if (amount == null) {
            source.sendError(Component.literal("Unknown amount: $amountStr").withStyle(ChatFormatting.RED))
            return Command.SINGLE_SUCCESS
        }

        val config = MagicDonutConfig.instance.payConfirmConfig
        val fullCommand = "pay $target $amount"

        if (!config.enabled || amount < config.confirmThreshold) {
            sendCommand(fullCommand)
            return Command.SINGLE_SUCCESS
        }

        pendingCommand = fullCommand
        pendingDeadlineMs = System.currentTimeMillis() + config.timeoutSeconds.coerceAtLeast(1) * 1000L

        source.sendFeedback(
            Component.literal("Payment for ")
                .append(Component.literal(AmountParser.formatShort(amount)).withStyle(ChatFormatting.GREEN))
                .append(" to ")
                .append(Component.literal(target).withStyle(ChatFormatting.GREEN))
                .append("?")
        )

        source.sendFeedback(
            Component.literal("Click to ").withStyle(ChatFormatting.GRAY)
                .append(button("[✔ Confirm]", ChatFormatting.GREEN, "/payconfirm"))
                .append(Component.literal(" or ").withStyle(ChatFormatting.GRAY))
                .append(button("[❌ Cancel]", ChatFormatting.RED, "/paycancel"))
                .append(Component.literal(".").withStyle(ChatFormatting.GRAY))
        )
        return Command.SINGLE_SUCCESS
    }

    private fun button(label: String, color: ChatFormatting, command: String): MutableComponent =
        Component.literal(label).withStyle { style ->
            style.withColor(color)
                .withBold(true)
                .withClickEvent(ClickEvent.RunCommand(command))
                .withHoverEvent(HoverEvent.ShowText(Component.literal(command).withStyle(color)))
        }

    private fun sendCommand(command: String) {
        MinecraftClientService.client.player?.connection?.sendCommand(command)
    }

    private fun cancelPending() {
        pendingCommand = null
    }
}
