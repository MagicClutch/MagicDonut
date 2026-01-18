package de.magicclutch.magicdonut.client.payconfirm

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket
import net.minecraft.network.chat.TextColor
import java.util.*

object PayConfirmClient {
    private var pendingCommand: String? = null
    private var timeoutTimer: Timer? = null

    fun init() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->

            // Main Command: /pay
            dispatcher.register(
                ClientCommandManager.literal("pay")
                    .executes { context ->
                        val nearby = findNearbyPlayer()
                        val msg = if (nearby != null) "§eNutze: /pay $nearby <menge>" else "§cSyntax: /pay <spieler> <menge>"
                        context.source.sendFeedback(Component.literal(msg))
                        Command.SINGLE_SUCCESS
                    }
                    .then(ClientCommandManager.argument("target", StringArgumentType.word())
                        // START TAB-COMPLETION FIX
                        .suggests { context, builder ->
                            val client = Minecraft.getInstance()
                            val player = client.player
                            val level = client.level

                            val suggestions = if (level != null && player != null) {
                                // 1. Suche Spieler in der Nähe (Renderdistanz)
                                val nearbyPlayers = level.players()
                                    .filter { it != player && it.isAlive }
                                    .sortedBy { it.distanceToSqr(player) } // Der Nächste zuerst
                                    .map { it.gameProfile.name }

                                // 2. Falls leer, nimm alle Online-Spieler
                                if (nearbyPlayers.isEmpty()) {
                                    client.connection?.onlinePlayers
                                        ?.map { it.profile.name }
                                        ?.filter { it != player.gameProfile.name }
                                        ?.sorted() ?: emptyList()
                                } else {
                                    nearbyPlayers
                                }
                            } else {
                                emptyList()
                            }

                            val remaining = builder.remaining.lowercase()
                            suggestions.forEach { name ->
                                if (name.lowercase().startsWith(remaining)) {
                                    builder.suggest(name)
                                }
                            }
                            builder.buildFuture()
                        }
                        // END TAB-COMPLETION FIX
                        .then(ClientCommandManager.argument("amount", StringArgumentType.string())
                            .executes { context ->
                                val target = StringArgumentType.getString(context, "target")
                                val amountStr = StringArgumentType.getString(context, "amount")
                                handlePay(context.source, target, amountStr)
                            }
                        )
                    )
            )

            dispatcher.register(
                ClientCommandManager.literal("payconfirm").executes { context ->
                    val cmd = pendingCommand
                    if (cmd != null) {
                        Minecraft.getInstance().player?.connection?.send(ServerboundChatCommandPacket(cmd))
                        cancelPending()
                        context.source.sendFeedback(Component.literal("§a§l✔ §aPayment sent!"))
                    } else {
                        context.source.sendError(Component.literal("§cNo payment pending."))
                    }
                    Command.SINGLE_SUCCESS
                }
            )

            dispatcher.register(
                ClientCommandManager.literal("paycancel").executes { context ->
                    if (pendingCommand != null) {
                        cancelPending()
                        context.source.sendFeedback(Component.literal("§e§l➡ §ePayment cancelled."))
                    } else {
                        context.source.sendError(Component.literal("§cThere are no payments in process."))
                    }
                    Command.SINGLE_SUCCESS
                }
            )
        }
    }

    private fun handlePay(source: FabricClientCommandSource, target: String, amountStr: String): Int {
        val amount = parseAmount(amountStr)
        if (amount == null) {
            source.sendError(Component.literal("§cUnknown amount: $amountStr"))
            return Command.SINGLE_SUCCESS
        }

        val config = MagicDonutConfig.instance.payConfirmConfig
        val fullCommand = "pay $target $amount"
        fun formatToShort(amount: Long): String {
            val b = amount / 1_000_000_000.0
            val m = amount / 1_000_000.0
            val k = amount / 1_000.0

            return when {
                amount >= 1_000_000_000L -> "%.2f".format(Locale.ENGLISH, b).trimEnd('0').trimEnd('.') + "b"
                amount >= 1_000_000L -> "%.2f".format(Locale.ENGLISH, m).trimEnd('0').trimEnd('.') + "m"
                amount >= 1_000L -> "%.2f".format(Locale.ENGLISH, k).trimEnd('0').trimEnd('.') + "k"
                else -> amount.toString()
            }
        }
        if (config.enabled && amount >= config.confirmThreshold) {
            startTimeout(source, config.timeoutSeconds)
            pendingCommand = fullCommand

            val display = formatToShort(amount)
            source.sendFeedback(Component.literal("Payment for §a$display §rto §a$target?"))

            val confirmBtn = Component.literal("[✔ Confirm]").withStyle { style ->
                style.withColor(TextColor.fromRgb(0x55FF55))
                    .withBold(true)
                    .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/payconfirm"))
                    .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§a/payconfirm")))
            }

            val cancelBtn = Component.literal("[❌ Cancel]").withStyle { style ->
                style.withColor(TextColor.fromRgb(0xFF5555))
                    .withBold(true)
                    .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/paycancel"))
                    .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§c/paycancel")))
            }

            source.sendFeedback(
                Component.literal("Click").withStyle { it.withColor(TextColor.fromRgb(0xAAAAAA)) }
                    .append(confirmBtn)
                    .append(Component.literal(" or ").withStyle { it.withColor(TextColor.fromRgb(0xAAAAAA)) })
                    .append(cancelBtn)
                    .append(Component.literal(".").withStyle { it.withColor(TextColor.fromRgb(0xAAAAAA)) })
            )
        } else {
            Minecraft.getInstance().player?.connection?.send(ServerboundChatCommandPacket(fullCommand))
        }
        return Command.SINGLE_SUCCESS
    }

    private fun parseAmount(input: String): Long? {
        val clean = input.trim().uppercase()
        val multiplier = when {
            clean.endsWith("K") -> 1_000L
            clean.endsWith("M") -> 1_000_000L
            clean.endsWith("B") -> 1_000_000_000L
            else -> 1L
        }
        val numericPart = clean.replace("[KMB]".toRegex(), "")
        return numericPart.toDoubleOrNull()?.let { (it * multiplier).toLong() }
    }

    private fun findNearbyPlayer(): String? {
        val client = Minecraft.getInstance()
        val player = client.player ?: return null
        return client.level?.players()
            ?.filter { it != player && it.isAlive }
            ?.minByOrNull { it.distanceToSqr(player) }
            ?.gameProfile?.name
    }

    private fun formatNumber(n: Long) = "%,d".format(n).replace(",", ".")

    private fun startTimeout(source: FabricClientCommandSource, seconds: Int) {
        cancelPending()
        timeoutTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    if (pendingCommand != null) {
                        pendingCommand = null
                        source.sendFeedback(Component.literal("§c⌛ Payment expired."))
                    }
                }
            }, seconds * 1000L)
        }
    }

    private fun cancelPending() {
        pendingCommand = null
        timeoutTimer?.cancel()
        timeoutTimer = null
    }
}