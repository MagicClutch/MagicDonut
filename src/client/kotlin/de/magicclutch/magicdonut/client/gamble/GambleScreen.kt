package de.magicclutch.magicdonut.client.gamble

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.util.AmountParser
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents

/** Offline casino. Uses a virtual balance stored in the config and never talks to the server. */
class GambleScreen(private val parent: Screen? = null) : Screen(Component.translatable("magicdonut.gamble.title")) {
    private val config get() = MagicDonutConfig.instance.gambleConfig

    private lateinit var betBox: EditBox
    private var resultText: String = "Pick a game and place your bet."
    private var resultColor: Int = COLOR_MUTED
    private var reels: List<String> = listOf("?", "?", "?")

    override fun init() {
        val centerX = width / 2
        val top = height / 2 - 70

        betBox = EditBox(font, centerX - 60, top + 30, 120, 20, Component.translatable("magicdonut.gamble.bet"))
        betBox.setHint(Component.translatable("magicdonut.gamble.bet.hint"))
        betBox.value = "100"
        addRenderableWidget(betBox)

        addRenderableWidget(small(Component.literal("1/2"), centerX - 128, top + 30) { scaleBet(0.5) })
        addRenderableWidget(small(Component.literal("x2"), centerX - 96, top + 30) { scaleBet(2.0) })
        addRenderableWidget(small(Component.literal("Max"), centerX + 64, top + 30) { betBox.value = config.balance.toString() })

        addRenderableWidget(game(Component.literal("Heads"), centerX - 128, top + 62) { play { GambleGames.coinflip(it, GambleGames.CoinSide.HEADS).let { o -> o to null } } })
        addRenderableWidget(game(Component.literal("Tails"), centerX - 42, top + 62) { play { GambleGames.coinflip(it, GambleGames.CoinSide.TAILS).let { o -> o to null } } })
        addRenderableWidget(game(Component.literal("Dice"), centerX + 44, top + 62) { play { GambleGames.dice(it) to null } })
        addRenderableWidget(
            Button.builder(Component.literal("Spin Slots")) {
                play { bet -> GambleGames.slots(bet).let { it.outcome to it.reels } }
            }.bounds(centerX - 128, top + 88, 256, 20).build()
        )

        addRenderableWidget(
            Button.builder(Component.literal("Reset balance")) {
                config.resetStats()
                MagicDonutConfig.HANDLER.save()
                resultText = "Balance reset to $${AmountParser.formatShort(config.balance)}."
                resultColor = COLOR_MUTED
            }.bounds(centerX - 128, top + 150, 124, 20).build()
        )
        addRenderableWidget(
            Button.builder(Component.translatable("gui.done")) { onClose() }
                .bounds(centerX + 4, top + 150, 124, 20).build()
        )
    }

    private fun small(label: Component, x: Int, y: Int, action: () -> Unit) =
        Button.builder(label) { action() }.bounds(x, y, 28, 20).build()

    private fun game(label: Component, x: Int, y: Int, action: () -> Unit) =
        Button.builder(label) { action() }.bounds(x, y, 84, 20).build()

    private fun scaleBet(factor: Double) {
        val bet = parseBet() ?: 1L
        betBox.value = (bet * factor).toLong().coerceIn(1L, maxOf(1L, config.balance)).toString()
    }

    private fun parseBet(): Long? = AmountParser.parse(betBox.value)

    private fun play(game: (Long) -> Pair<GambleGames.Outcome, List<String>?>) {
        val bet = parseBet()
        if (bet == null) {
            resultText = "Enter a valid bet, e.g. 500 or 2.5k."
            resultColor = COLOR_BAD
            return
        }
        if (bet > config.balance) {
            resultText = "You only have $${AmountParser.formatShort(config.balance)}."
            resultColor = COLOR_BAD
            return
        }

        val (outcome, newReels) = game(bet)
        if (newReels != null) reels = newReels

        config.balance += outcome.payout - bet
        config.gamesPlayed++
        if (outcome.won) {
            config.gamesWon++
            config.biggestWin = maxOf(config.biggestWin, outcome.payout - bet)
        }
        MagicDonutConfig.HANDLER.save()

        val net = outcome.payout - bet
        resultText = "${outcome.description} (${if (net >= 0) "+" else "-"}$${AmountParser.formatShort(kotlin.math.abs(net))})"
        resultColor = if (net > 0) COLOR_GOOD else if (net == 0L) COLOR_MUTED else COLOR_BAD

        if (config.soundEnabled) {
            val sound = if (outcome.won) SoundEvents.PLAYER_LEVELUP else SoundEvents.VILLAGER_NO
            minecraft.soundManager.play(SimpleSoundInstance.forUI(sound, 1.0f))
        }
        if (config.balance <= 0L) {
            resultText += " Out of money - press Reset balance."
        }
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(context, mouseX, mouseY, delta)

        val centerX = width / 2
        val top = height / 2 - 70

        context.centeredText(font, title, centerX, top - 20, COLOR_TEXT)
        context.centeredText(font, "Virtual money only - nothing here touches the server.", centerX, top - 8, COLOR_MUTED)
        context.centeredText(font, "Balance: $${AmountParser.formatShort(config.balance)}", centerX, top + 16, COLOR_GOOD)

        context.centeredText(font, reels.joinToString("   "), centerX, top + 114, COLOR_TEXT)
        context.centeredText(font, resultText, centerX, top + 130, resultColor)

        val played = config.gamesPlayed
        val rate = if (played == 0) 0 else config.gamesWon * 100 / played
        context.centeredText(
            font,
            "Played $played  |  Won $rate%  |  Biggest win $${AmountParser.formatShort(config.biggestWin)}",
            centerX, top + 178, COLOR_MUTED
        )
    }

    override fun onClose() {
        minecraft.gui.setScreen(parent)
    }

    private companion object {
        const val COLOR_TEXT = 0xFFFFFFFF.toInt()
        const val COLOR_MUTED = 0xFFAAAAAA.toInt()
        const val COLOR_GOOD = 0xFF55FF55.toInt()
        const val COLOR_BAD = 0xFFFF5555.toInt()
    }
}
