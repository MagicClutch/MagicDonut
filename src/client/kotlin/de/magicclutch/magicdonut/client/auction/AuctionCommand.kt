package de.magicclutch.magicdonut.client.auction

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import de.magicclutch.magicdonut.client.util.AmountParser
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

/**
 * /auction start [seconds] [minimumBid]  - auction the item in your main hand
 * /auction status | end | cancel | announce | move
 */
object AuctionCommand {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommands.literal("auction")
                    .then(
                        ClientCommands.literal("start")
                            .executes { start(it, null, null) }
                            .then(
                                ClientCommands.argument("seconds", IntegerArgumentType.integer(10, 3600))
                                    .executes { start(it, IntegerArgumentType.getInteger(it, "seconds"), null) }
                                    .then(
                                        ClientCommands.argument("minBid", StringArgumentType.word())
                                            .executes {
                                                start(
                                                    it,
                                                    IntegerArgumentType.getInteger(it, "seconds"),
                                                    StringArgumentType.getString(it, "minBid")
                                                )
                                            }
                                    )
                            )
                    )
                    .then(ClientCommands.literal("status").executes { status(it.source) })
                    .then(ClientCommands.literal("end").executes { end(it.source) })
                    .then(ClientCommands.literal("cancel").executes { cancel(it.source) })
                    .then(ClientCommands.literal("announce").executes { announce(it.source) })
                    .then(ClientCommands.literal("move").executes { move(it.source) })
            )
        }
    }

    private fun start(context: CommandContext<FabricClientCommandSource>, seconds: Int?, minBidText: String?): Int {
        val source = context.source
        val config = MagicDonutConfig.instance.auctionConfig

        val minBid = if (minBidText == null) {
            config.defaultMinimumBid
        } else {
            AmountParser.parse(minBidText) ?: run {
                error(source, "Unknown minimum bid: $minBidText")
                return Command.SINGLE_SUCCESS
            }
        }

        val held = MinecraftClientService.client.player?.mainHandItem
        if (held == null || held.isEmpty) {
            error(source, "Hold the item you want to auction in your main hand.")
            return Command.SINGLE_SUCCESS
        }
        if (AuctionManager.current != null) {
            error(source, "An auction is already running. Use /auction end or /auction cancel first.")
            return Command.SINGLE_SUCCESS
        }

        val player = MinecraftClientService.client.player!!
        AuctionManager.start(held, seconds ?: config.defaultDurationSeconds, minBid, anchorInFront(player))
        info(source, "Auction started. Bids are payments to you; the highest payment wins.")
        info(source, "Use /auction announce to post it in chat. Remember to refund the losing bidders.")
        return Command.SINGLE_SUCCESS
    }

    private fun status(source: FabricClientCommandSource): Int {
        val auction = AuctionManager.current
        if (auction == null) {
            error(source, "No auction is running.")
            return Command.SINGLE_SUCCESS
        }
        info(source, "${auction.item.count}x ${auction.item.hoverName.string} | ${auction.secondsLeft()}s left | min bid $${AmountParser.formatShort(auction.minimumBid)}")
        val ranking = auction.bids.sortedByDescending { it.amount }.take(5)
        if (ranking.isEmpty()) info(source, "No bids yet.")
        ranking.forEachIndexed { index, bid ->
            info(source, "#${index + 1} ${bid.player} - $${AmountParser.formatShort(bid.amount)}")
        }
        return Command.SINGLE_SUCCESS
    }

    private fun end(source: FabricClientCommandSource): Int {
        if (AuctionManager.end() == null) error(source, "No auction is running.")
        return Command.SINGLE_SUCCESS
    }

    private fun cancel(source: FabricClientCommandSource): Int {
        if (AuctionManager.cancel()) info(source, "Auction cancelled.") else error(source, "No auction is running.")
        return Command.SINGLE_SUCCESS
    }

    private fun move(source: FabricClientCommandSource): Int {
        val auction = AuctionManager.current
        val player = MinecraftClientService.client.player
        if (auction == null || player == null) {
            error(source, "No auction is running.")
        } else {
            auction.anchor = anchorInFront(player)
            info(source, "Hologram moved in front of you.")
        }
        return Command.SINGLE_SUCCESS
    }

    private fun announce(source: FabricClientCommandSource): Int {
        val auction = AuctionManager.current
        val player = MinecraftClientService.client.player
        if (auction == null || player == null) {
            error(source, "No auction is running.")
            return Command.SINGLE_SUCCESS
        }
        player.connection.sendChat(AuctionManager.announcementText(auction, player.gameProfile.name))
        return Command.SINGLE_SUCCESS
    }

    private fun info(source: FabricClientCommandSource, text: String) {
        source.sendFeedback(
            Component.literal("[Auction] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(text).withStyle(ChatFormatting.GRAY))
        )
    }

    private fun error(source: FabricClientCommandSource, text: String) {
        source.sendError(Component.literal(text).withStyle(ChatFormatting.RED))
    }
}
