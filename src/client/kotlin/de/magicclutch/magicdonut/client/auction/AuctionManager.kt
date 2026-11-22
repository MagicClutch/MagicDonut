package de.magicclutch.magicdonut.client.auction

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import de.magicclutch.magicdonut.client.util.AmountParser
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
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
        @Volatile var endsAtMs: Long,
    ) {
        /** When the last bid added time, and how much, for the "+3s" effect. */
        val startedAtMs: Long = System.currentTimeMillis()

        /**
         * Stream delay (0 to 60 s): when the timer hits 0 it is reset to this many seconds, so bids that reach
         * viewers late still count. Applied again if a new top bid came in during the delay.
         */
        @Volatile var streamDelaySeconds: Int = 0
        @Volatile var lastDelayAtMs: Long = 0L

        /** The timer was reset by the stream delay and no new top bid has come in since: shows "Stream Delay +Ns". */
        fun inStreamDelay(): Boolean = lastDelayAtMs > 0L && lastBidAtMs <= lastDelayAtMs && endsAtMs > System.currentTimeMillis()

        /** When the last new top bid came in: the items spin once around after it. */
        @Volatile var lastBidAtMs: Long = 0L
        @Volatile var lastBonusAtMs: Long = 0L
        @Volatile var lastBonusSeconds: Int = 0

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
    const val MAX_STREAM_DELAY_SECONDS = 60

    /** The tick above 10 seconds is quieter than the countdown sounds. */
    private const val TICK_VOLUME = 0.5f

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
            val auction = current ?: return@register.also { lastTickSecond = -1L }
            if (client.level != null) tickSounds(auction)
            if (client.level == null) {
                current = null
            } else if (System.currentTimeMillis() >= auction.endsAtMs) {
                // The timer hit 0: with a stream delay it restarts at that many seconds, once per round of bids.
                val newBidSinceDelay = auction.lastDelayAtMs == 0L || auction.lastBidAtMs > auction.lastDelayAtMs
                if (auction.streamDelaySeconds > 0 && newBidSinceDelay) {
                    val now = System.currentTimeMillis()
                    auction.lastDelayAtMs = now
                    auction.endsAtMs = now + auction.streamDelaySeconds * 1000L
                } else {
                    end()
                }
            }
        }
    }

    private var lastTickSecond = -1L

    /** A tick every second; from 10 down to 0 a "dim" every second, each one a step higher in pitch. */
    private fun tickSounds(auction: Auction) {
        val seconds = auction.secondsLeft()
        if (seconds == lastTickSecond) return
        lastTickSecond = seconds
        if (seconds > 10) {
            play(AuctionSounds.TICK, 1.0f, TICK_VOLUME)
        } else {
            // 1.2 semitones per second: 10 -> 0 climbs exactly one octave (pitch 1.0 to 2.0).
            val pitch = Math.pow(2.0, (10 - seconds) * 1.2 / 12.0).toFloat()
            play(AuctionSounds.DIM, pitch)
        }
    }

    /** Plays a UI sound, scaled by the "Sound volume" setting. */
    private fun play(sound: net.minecraft.sounds.SoundEvent, pitch: Float, volume: Float = 0.8f) {
        val scaled = volume * MagicDonutConfig.instance.auctionConfig.soundVolume.coerceIn(0f, 1f)
        if (scaled <= 0f) return
        MinecraftClientService.client.soundManager.play(SimpleSoundInstance.forUI(sound, pitch, scaled.coerceIn(0f, 1f)))
    }

    fun start(item: ItemStack, seconds: Int, minimumBid: Long, anchor: Vec3?): Boolean {
        if (item.isEmpty || current != null) return false
        val delay = MagicDonutConfig.instance.auctionConfig.streamDelaySeconds.coerceIn(0, MAX_STREAM_DELAY_SECONDS)
        current = Auction(item.copy(), minimumBid, System.currentTimeMillis() + seconds * 1000L).also {
            it.anchor = anchor
            it.streamDelaySeconds = delay
        }
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
        if (winner != null) sendWinnerButtons(winner.player)

        // The level-up jingle is the "won" sound, so it only plays when there is a winner.
        if (winner != null) play(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f)
        return auction
    }

    // Player names come from chat, so only allow what a username can contain before putting it in a command.
    private val safeName = Regex("""[A-Za-z0-9_.]{1,32}""")

    /** Two clickable buttons under the result: reach the winner to hand over the item. */
    private fun sendWinnerButtons(player: String) {
        if (!safeName.matches(player)) return
        MinecraftClientService.sendChatMessage(
            Component.literal("[Auction] ").withStyle(ChatFormatting.GOLD)
                .append(button("[Teleport]", ChatFormatting.AQUA, "/tpa $player"))
                .append(Component.literal("  "))
                .append(button("[Orders]", ChatFormatting.YELLOW, "/orders $player"))
        )
    }

    private fun button(label: String, color: ChatFormatting, command: String): MutableComponent =
        Component.literal(label).withStyle { style ->
            style.withColor(color)
                .withBold(true)
                .withClickEvent(ClickEvent.RunCommand(command))
                .withHoverEvent(HoverEvent.ShowText(Component.literal(command).withStyle(color)))
        }

    /** Sets the stream delay of the running auction (0 to 60 seconds): the timer restarts at this value when it hits 0. */
    fun setStreamDelay(seconds: Int): Boolean {
        val auction = current ?: return false
        auction.streamDelaySeconds = seconds.coerceIn(0, MAX_STREAM_DELAY_SECONDS)
        return true
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

        val config = MagicDonutConfig.instance.auctionConfig

        val previousTop = auction.highest
        auction.bids += Bid(player, amount)
        // Bids that don't beat the current top bid add no time and trigger no effect (stops $1, $2, ... spam).
        if (previousTop != null && amount <= previousTop.amount) return

        auction.lastBidAtMs = System.currentTimeMillis()

        // The bell only rings for a bid above the current top bid, so $1 spam stays silent.
        play(AuctionSounds.BELL, 1.0f, 0.4f)

        // A new top bid buys a little more time (anti-sniping), with a small effect.
        val bonus = config.bidTimeBonusSeconds.coerceAtLeast(0)
        if (bonus > 0) {
            auction.endsAtMs += bonus * 1000L
            auction.lastBonusAtMs = System.currentTimeMillis()
            auction.lastBonusSeconds = bonus
        }
        AuctionHologram.burst(auction)

        MinecraftClientService.sendChatMessage(
            Component.literal("[Auction] ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("New highest bid: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("$" + AmountParser.formatShort(amount)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" by ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(player).withStyle(ChatFormatting.GREEN))
        )

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
