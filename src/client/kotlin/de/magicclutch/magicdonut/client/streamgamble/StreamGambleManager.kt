package de.magicclutch.magicdonut.client.streamgamble

import de.magicclutch.magicdonut.client.auction.AuctionManager
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.service.MinecraftClientService
import de.magicclutch.magicdonut.client.util.AmountParser
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import java.security.SecureRandom
import java.util.ArrayDeque

/**
 * Stream gamble: a viewer pays the host, and an item strip decides who wins. The host wins with the configured chance
 * (default 60%), which is shown on screen the whole time. Nothing is paid automatically: when the player wins, the host
 * gets a chat line with a click-to-fill `/pay` command.
 */
object StreamGambleManager {
    enum class Phase(val seconds: Float) {
        ANNOUNCE(1.8f),
        SPIN(5.5f),
        RESULT(9f),
    }

    /** The repeating pattern of the strip: `true` is a host cell. 6 host and 4 player cells = 60% / 40%. */
    private val basePattern = listOf(true, false, true, true, false, true, false, true, true, false)
    const val PATTERN_REPEATS = 4

    class Round(
        val player: String,
        val stake: Long,
        val hostWins: Boolean,
        /** Index of the strip cell the pointer ends on. */
        val landing: Int,
        /** Offset inside the landing cell, so the pointer does not always stop dead center. */
        val jitter: Float,
    ) {
        var phase: Phase = Phase.ANNOUNCE
        var phaseStartMs: Long = System.currentTimeMillis()

        val cells: List<Boolean> = List(basePattern.size * PATTERN_REPEATS) { basePattern[it % basePattern.size] }

        fun phaseProgress(now: Long = System.currentTimeMillis()): Float =
            ((now - phaseStartMs) / 1000f / phase.seconds).coerceIn(0f, 1f)
    }

    private val random = SecureRandom()
    private val queue = ArrayDeque<Pair<String, Long>>()

    @Volatile
    var current: Round? = null
        private set

    val queued: Int get() = queue.size

    fun hostChance(): Double = MagicDonutConfig.instance.streamGambleConfig.hostChancePercent.coerceIn(1, 99) / 100.0

    fun hostName(): String {
        val configured = MagicDonutConfig.instance.streamGambleConfig.hostName.trim()
        return configured.ifEmpty { MinecraftClientService.client.user.name }
    }

    fun init() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.level == null) {
                current = null
                queue.clear()
                return@register
            }
            advance()
        }
    }

    /** Called for every "X paid you $N" message. */
    fun onPayment(player: String, amount: Double) {
        val config = MagicDonutConfig.instance.streamGambleConfig
        if (!config.enabled || !config.autoStart) return
        // While an auction runs, payments are bids, not stakes.
        if (AuctionManager.current != null) return
        if (amount < config.minStake) return
        enqueue(player, amount.toLong())
    }

    fun enqueue(player: String, stake: Long) {
        queue.add(player to stake)
        if (current == null) startNext()
    }

    fun skip() {
        current = null
        startNext()
    }

    fun clear() {
        queue.clear()
        current = null
    }

    private fun startNext() {
        val next = queue.poll() ?: return
        current = createRound(next.first, next.second)
    }

    private fun createRound(player: String, stake: Long): Round {
        val hostWins = random.nextDouble() < hostChance()
        val pattern = List(basePattern.size * PATTERN_REPEATS) { basePattern[it % basePattern.size] }
        // Land somewhere late on the strip, on a cell that belongs to the winning side.
        val candidates = (28 until 36).filter { pattern[it] == hostWins }
        val landing = candidates[random.nextInt(candidates.size)]
        val jitter = (random.nextDouble() - 0.5).toFloat() * 0.7f
        return Round(player, stake, hostWins, landing, jitter)
    }

    private fun advance() {
        val round = current ?: return
        if (round.phaseProgress() < 1f) return

        when (round.phase) {
            Phase.ANNOUNCE -> enter(round, Phase.SPIN)
            Phase.SPIN -> {
                enter(round, Phase.RESULT)
                announceResult(round)
            }
            Phase.RESULT -> {
                current = null
                startNext()
            }
        }
    }

    private fun enter(round: Round, phase: Phase) {
        round.phase = phase
        round.phaseStartMs = System.currentTimeMillis()
    }

    fun payoutFor(round: Round): Long =
        (round.stake * MagicDonutConfig.instance.streamGambleConfig.payoutMultiplier).toLong()

    private fun announceResult(round: Round) {
        val config = MagicDonutConfig.instance.streamGambleConfig
        if (config.soundEnabled) {
            val sound = if (round.hostWins) SoundEvents.VILLAGER_NO else SoundEvents.PLAYER_LEVELUP
            MinecraftClientService.client.soundManager.play(SimpleSoundInstance.forUI(sound, 1.0f))
        }
        if (round.hostWins) {
            MinecraftClientService.sendChatMessage(
                Component.literal("[Stream Gamble] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("${round.player} lost $${AmountParser.formatShort(round.stake)}.").withStyle(ChatFormatting.GRAY))
            )
        } else {
            val payout = payoutFor(round)
            val suggestion = "/pay ${round.player} $payout"
            MinecraftClientService.sendChatMessage(
                Component.literal("[Stream Gamble] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("${round.player} won! Click to pay out $${AmountParser.formatShort(payout)}: ").withStyle(ChatFormatting.GREEN))
                    .append(
                        Component.literal("[$suggestion]").withStyle { style ->
                            style.withColor(ChatFormatting.AQUA).withUnderlined(true).withClickEvent(ClickEvent.SuggestCommand(suggestion))
                        }
                    )
            )
        }
    }
}
