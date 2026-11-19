package de.magicclutch.magicdonut.client.auction

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import de.magicclutch.magicdonut.client.util.AmountParser
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

/**
 * A player-run auction: the seller holds an item, other players bid by paying the seller
 * (`/pay <seller> <amount>`), and the highest single payment wins.
 *
 * This is purely informational: bids are read from the "X paid you $N" chat message and
 * nothing is bought, sold or paid automatically. Refunding losing bidders is up to the seller.
 */
object AuctionManager {
    data class Bid(val player: String, val amount: Double)

    class Auction(
        val item: ItemStack,
        val minimumBid: Long,
        val endsAtMs: Long,
    ) {
        val bids: MutableList<Bid> = mutableListOf()

        /** Where the hologram floats in the world. */
        @Volatile
        var anchor: Vec3? = null

        val highest: Bid?
            get() = bids.maxByOrNull { it.amount }

        fun secondsLeft(now: Long = System.currentTimeMillis()): Long =
            ((endsAtMs - now) / 1000L).coerceAtLeast(0L)
    }

    /** How long a finished auction stays on screen. */
    private const val RESULT_VISIBLE_MS = 30_000L

    @Volatile
    var current: Auction? = null
        private set

    /** The last finished auction, shown on the HUD for a short while. */
    @Volatile
    var finished: Auction? = null
        private set
    private var finishedAtMs: Long = 0L

    val hasVisibleAuction: Boolean
        get() = current != null || visibleFinished() != null

    fun visibleFinished(): Auction? =
        finished?.takeIf { System.currentTimeMillis() - finishedAtMs < RESULT_VISIBLE_MS }

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val auction = current ?: return@register
            if (client.level == null) {
                current = null
            } else if (System.currentTimeMillis() >= auction.endsAtMs) {
                end()
            }
        }
    }

    fun start(item: ItemStack, seconds: Int, minimumBid: Long, anchor: Vec3?): Boolean {
        if (item.isEmpty || current != null) return false
        current = Auction(item.copy(), minimumBid, System.currentTimeMillis() + seconds * 1000L).also { it.anchor = anchor }
        finished = null
        return true
    }

    /** Ends the running auction now and announces the result locally. Returns it, or `null` if none ran. */
    fun end(): Auction? {
        val auction = current ?: return null
        current = null
        finished = auction
        finishedAtMs = System.currentTimeMillis()

        val winner = auction.highest
        val itemName = auction.item.hoverName
        val message = if (winner == null) {
            Component.literal("[Auction] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("No bids for ").withStyle(ChatFormatting.GRAY))
                .append(itemName)
        } else {
            Component.literal("[Auction] ").withStyle(ChatFormatting.GOLD)
                .append(itemName)
                .append(Component.literal(" goes to ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(winner.player).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" for ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("$" + AmountParser.formatShort(winner.amount)).withStyle(ChatFormatting.GREEN))
        }
        MinecraftClientService.sendChatMessage(message)
        return auction
    }

    fun cancel(): Boolean {
        if (current == null) return false
        current = null
        return true
    }

    /** Called for every "X paid you $N" message while an auction is running. */
    fun onPayment(player: String, amount: Double) {
        val auction = current ?: return
        if (amount < auction.minimumBid) return

        val previousTop = auction.highest
        auction.bids += Bid(player, amount)
        if (previousTop != null && amount <= previousTop.amount) return

        MinecraftClientService.sendChatMessage(
            Component.literal("[Auction] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("New highest bid: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("$" + AmountParser.formatShort(amount)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" by ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(player).withStyle(ChatFormatting.GREEN))
        )

        if (MagicDonutConfig.instance.auctionConfig.playBidSound) {
            val client = MinecraftClientService.client
            client.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f))
        }
    }
}

/** A point in front of the player (horizontally), slightly below eye height: where a new hologram floats. */
fun anchorInFront(player: net.minecraft.world.entity.player.Player, distance: Double = 2.4): Vec3 {
    val look = player.lookAngle
    var horizontal = Vec3(look.x, 0.0, look.z)
    horizontal = if (horizontal.lengthSqr() < 1.0E-4) Vec3(0.0, 0.0, 1.0) else horizontal.normalize()
    val eye = player.getEyePosition()
    return Vec3(eye.x + horizontal.x * distance, eye.y - 0.35, eye.z + horizontal.z * distance)
}
