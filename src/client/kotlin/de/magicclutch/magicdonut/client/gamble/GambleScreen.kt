package de.magicclutch.magicdonut.client.gamble

import com.mojang.blaze3d.platform.InputConstants
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.gamble.GambleGames.Symbol
import de.magicclutch.magicdonut.client.gui.MouseButton
import de.magicclutch.magicdonut.client.gui.Theme
import de.magicclutch.magicdonut.client.gui.frame
import de.magicclutch.magicdonut.client.gui.inside
import de.magicclutch.magicdonut.client.gui.rightText
import de.magicclutch.magicdonut.client.gui.roundRect
import de.magicclutch.magicdonut.client.gui.scaledText
import de.magicclutch.magicdonut.client.util.AmountParser
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.math.abs
import kotlin.math.floor
import kotlin.random.Random

/**
 * An offline slot machine built from Minecraft items. It uses a virtual balance stored in the config and never touches
 * the server. The spin result is decided up front; the reels only animate towards it.
 */
class GambleScreen(private val parent: Screen? = null) : Screen(Component.translatable("magicdonut.gamble.title")) {
    private val config get() = MagicDonutConfig.instance.gambleConfig

    // ---- reels ---------------------------------------------------------------------------------------------

    private class Reel {
        var position = 0.0
        var start = 0.0
        var end = 0.0
        var elapsed = 0f
        var duration = 0f
        var spinning = false
    }

    /** The order of symbols on every reel, repeated so the strip loops. */
    private val strip: List<Symbol> = listOf(
        Symbol.REDSTONE, Symbol.GOLD, Symbol.REDSTONE, Symbol.EMERALD, Symbol.GOLD,
        Symbol.REDSTONE, Symbol.DIAMOND, Symbol.GOLD, Symbol.EMERALD, Symbol.REDSTONE,
        Symbol.STAR, Symbol.GOLD, Symbol.REDSTONE, Symbol.EMERALD, Symbol.DIAMOND,
    )
    private val reels = List(3) { Reel().apply { position = Random.nextInt(strip.size).toDouble() } }

    private var pending: GambleGames.SpinResult? = null
    private var pendingBet = 0L
    private var message = "Place your bet and pull the lever."
    private var messageColor = Theme.MUTED
    private var winFlash = 0f
    private var lastFrameNanos = System.nanoTime()
    private var spinHover = 0f
    private val buttonHover = FloatArray(4)

    private lateinit var betBox: EditBox

    private val spinning get() = reels.any { it.spinning }

    // ---- layout --------------------------------------------------------------------------------------------

    private val panelW = 310
    private val panelH = 214
    private val panelX get() = (width - panelW) / 2
    private val panelY get() = (height - panelH) / 2
    private val cell = 24
    private val reelW = 48
    private val reelGap = 6
    private val reelsX get() = panelX + 18
    private val reelsY get() = panelY + 50
    private val reelsH get() = cell * 3
    private val betY get() = panelY + 134

    override fun init() {
        betBox = EditBox(font, panelX + 24, betY + 5, 76, 12, Component.translatable("magicdonut.gamble.bet")).also {
            it.setBordered(false)
            it.setTextColor(Theme.TEXT)
            it.setMaxLength(14)
            it.setHint(Component.literal("Bet").withStyle { s -> s.withColor(Theme.MUTED and 0xFFFFFF) })
            it.value = if (this::betBox.isInitialized) betBox.value else "100"
        }
    }

    // ---- game flow -----------------------------------------------------------------------------------------

    private fun parseBet(): Long? = AmountParser.parse(betBox.value)

    private fun scaleBet(factor: Double) {
        val bet = parseBet() ?: 1L
        betBox.value = (bet * factor).toLong().coerceIn(1L, maxOf(1L, config.balance)).toString()
    }

    private fun spin() {
        if (spinning) return
        val bet = parseBet()
        if (bet == null) {
            say("Enter a valid bet, for example 500 or 2.5k.", Theme.BAD)
            return
        }
        if (bet > config.balance) {
            say("You only have $${AmountParser.formatShort(config.balance)}.", Theme.BAD)
            return
        }

        val result = GambleGames.spin(bet)
        config.balance -= bet
        config.gamesPlayed++
        MagicDonutConfig.save()
        pending = result
        pendingBet = bet
        winFlash = 0f
        say("Spinning...", Theme.MUTED)

        result.reels.forEachIndexed { index, symbol ->
            val reel = reels[index]
            val size = strip.size
            val current = floor(reel.position).toInt()
            val matches = strip.indices.filter { strip[it] == symbol }
            val targetIndex = matches.random()
            val forward = Math.floorMod(targetIndex - current, size)
            reel.start = reel.position
            reel.end = reel.position + size * (3 + index * 2) + forward
            reel.elapsed = 0f
            reel.duration = 1.5f + index * 0.55f
            reel.spinning = true
        }
        click(1.2f)
    }

    private fun finishSpin() {
        val result = pending ?: return
        pending = null
        config.balance += result.payout
        val profit = result.payout - pendingBet
        if (result.won && profit > 0) {
            config.gamesWon++
            config.biggestWin = maxOf(config.biggestWin, profit)
        } else if (result.won) {
            config.gamesWon++
        }
        MagicDonutConfig.save()

        val net = if (profit >= 0) "+$${AmountParser.formatShort(profit)}" else "-$${AmountParser.formatShort(abs(profit))}"
        if (profit > 0) {
            winFlash = 1f
            say("${result.description}  $net", Theme.GOOD)
            play(SoundEvents.PLAYER_LEVELUP)
        } else if (profit == 0L) {
            say("${result.description}  $net", Theme.GOLD)
        } else {
            say("${result.description}  $net", Theme.BAD)
            if (config.balance <= 0L) message += "  Out of money: reset your balance in the settings."
        }
    }

    private fun say(text: String, color: Int) {
        message = text
        messageColor = color
    }

    private fun play(sound: net.minecraft.sounds.SoundEvent, pitch: Float = 1f) {
        if (!config.soundEnabled) return
        minecraft.soundManager.play(SimpleSoundInstance.forUI(sound, pitch))
    }

    private fun click(pitch: Float) = play(SoundEvents.EXPERIENCE_ORB_PICKUP, pitch)

    // ---- rendering -----------------------------------------------------------------------------------------

    override fun extractBackground(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        context.fill(0, 0, width, height, Theme.OVERLAY)
    }

    override fun extractRenderState(g: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(g, mouseX, mouseY, delta)

        val now = System.nanoTime()
        val dt = ((now - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameNanos = now
        advanceReels(dt)
        winFlash = (winFlash - dt * 0.5f).coerceAtLeast(0f)

        drawPanel(g)
        drawReels(g)
        drawPaytable(g)
        drawControls(g, mouseX, mouseY, dt)
        drawFooter(g)
    }

    private fun advanceReels(dt: Float) {
        var anyStopped = false
        for ((index, reel) in reels.withIndex()) {
            if (!reel.spinning) continue
            reel.elapsed += dt
            val t = (reel.elapsed / reel.duration).coerceIn(0f, 1f)
            // Fast at the start, easing out hard at the end so the reel "settles" on its symbol.
            val eased = 1f - (1f - t) * (1f - t) * (1f - t) * (1f - t)
            reel.position = reel.start + (reel.end - reel.start) * eased
            if (t >= 1f) {
                reel.spinning = false
                reel.position = reel.end
                click(0.8f + index * 0.25f)
                anyStopped = true
            }
        }
        if (anyStopped && !spinning) finishSpin()
    }

    private fun drawPanel(g: GuiGraphicsExtractor) {
        val x = panelX
        val y = panelY
        g.roundRect(x - 1, y - 1, panelW + 2, panelH + 2, Theme.withAlpha(Theme.mix(Theme.ACCENT, Theme.GOLD, winFlash), 0.5f + 0.4f * winFlash), 4)
        g.roundRect(x, y, panelW, panelH, Theme.WINDOW, 4)
        var lx = x + 4
        while (lx < x + panelW - 4) {
            g.fill(lx, y, minOf(lx + 6, x + panelW - 4), y + 1, Theme.shimmer((lx - x) / panelW.toFloat() * 0.6f))
            lx += 6
        }
        g.scaledText(font, "SLOT MACHINE", x + 18, y + 12, Theme.shimmer(), 1.5f, true)
        g.scaledText(font, "Virtual money only. Nothing here touches the server.", x + 18, y + 29, Theme.MUTED, 0.75f)
        g.rightText(font, "$${AmountParser.formatShort(config.balance)}", x + panelW - 18, y + 14, Theme.GOOD, true)
        g.rightText(font, "Balance", x + panelW - 18, y + 26, Theme.MUTED)
    }

    private fun drawReels(g: GuiGraphicsExtractor) {
        val totalW = reels.size * reelW + (reels.size - 1) * reelGap
        g.roundRect(reelsX - 6, reelsY - 6, totalW + 12, reelsH + 12, Theme.SIDEBAR, 4)
        g.frame(reelsX - 6, reelsY - 6, totalW + 12, reelsH + 12, Theme.mix(Theme.BORDER, Theme.GOLD, winFlash))

        for ((index, reel) in reels.withIndex()) {
            val rx = reelsX + index * (reelW + reelGap)
            g.roundRect(rx, reelsY, reelW, reelsH, Theme.CONTROL, 3)

            g.enableScissor(rx, reelsY, rx + reelW, reelsY + reelsH)
            val base = floor(reel.position).toInt()
            for (i in base - 1..base + 2) {
                val symbol = strip[Math.floorMod(i, strip.size)]
                val y = reelsY + reelsH / 2 - cell / 2 + ((reel.position - i) * cell).toInt()
                val p = g.pose()
                p.pushMatrix()
                p.translate((rx + reelW / 2 - 12).toFloat(), (y + cell / 2 - 12).toFloat())
                p.scale(1.5f, 1.5f)
                g.item(ItemStack(itemFor(symbol)), 0, 0)
                p.popMatrix()
            }
            // soft fade at the top and bottom edge
            g.fillGradient(rx, reelsY, rx + reelW, reelsY + 14, 0xE00B0B15.toInt(), 0x000B0B15)
            g.fillGradient(rx, reelsY + reelsH - 14, rx + reelW, reelsY + reelsH, 0x000B0B15, 0xE00B0B15.toInt())
            g.disableScissor()
        }

        // payline
        val lineColor = Theme.mix(Theme.ACCENT_2, Theme.GOLD, winFlash)
        val ly = reelsY + reelsH / 2 - cell / 2
        g.frame(reelsX - 3, ly, totalW + 6, cell, Theme.withAlpha(lineColor, 0.6f + 0.4f * winFlash))
        g.fill(reelsX - 8, reelsY + reelsH / 2 - 1, reelsX - 4, reelsY + reelsH / 2 + 1, lineColor)
        g.fill(reelsX + totalW + 4, reelsY + reelsH / 2 - 1, reelsX + totalW + 8, reelsY + reelsH / 2 + 1, lineColor)
    }

    private fun drawPaytable(g: GuiGraphicsExtractor) {
        val x = panelX + 196
        val y = panelY + 44
        g.roundRect(x, y, 98, 90, Theme.ROW, 3)
        g.text(font, "PAYOUTS", x + 8, y + 6, Theme.ACCENT_2, false)
        for ((i, symbol) in Symbol.entries.withIndex()) {
            val ry = y + 18 + i * 14
            g.item(ItemStack(itemFor(symbol)), x + 6, ry - 3)
            g.text(font, "x3", x + 28, ry + 1, Theme.MUTED, false)
            g.rightText(font, "${symbol.tripleMultiplier}x", x + 90, ry + 1, if (symbol == Symbol.STAR) Theme.GOLD else Theme.TEXT)
        }
        g.scaledText(font, "Pair = bet back", x + 8, y + 82 - 1, Theme.MUTED, 0.75f)
    }

    private fun chipRects(): List<IntArray> {
        val x = panelX + 108
        return listOf(
            intArrayOf(x, betY, 26, 18),
            intArrayOf(x + 30, betY, 26, 18),
            intArrayOf(x + 60, betY, 30, 18),
        )
    }

    private fun spinRect() = intArrayOf(panelX + 18, betY + 24, 154, 24)
    private fun doneRect() = intArrayOf(panelX + panelW - 80, panelY + panelH - 28, 62, 16)

    private fun drawControls(g: GuiGraphicsExtractor, mx: Int, my: Int, dt: Float) {
        // bet field
        g.roundRect(panelX + 18, betY, 86, 18, Theme.CONTROL)
        g.frame(panelX + 18, betY, 86, 18, if (betBox.isFocused) Theme.ACCENT_2 else Theme.BORDER)
        betBox.x = panelX + 24
        betBox.y = betY + 5
        betBox.extractRenderState(g, mx, my, dt)

        val labels = listOf("1/2", "x2", "Max")
        chipRects().forEachIndexed { i, r ->
            val hovered = inside(mx, my, r[0], r[1], r[2], r[3]) && !spinning
            buttonHover[i] = Theme.approach(buttonHover[i], if (hovered) 1f else 0f, dt)
            g.roundRect(r[0], r[1], r[2], r[3], Theme.mix(Theme.ROW, Theme.ROW_HOVER, buttonHover[i]))
            g.frame(r[0], r[1], r[2], r[3], Theme.mix(Theme.BORDER, Theme.ACCENT, buttonHover[i]))
            g.centeredText(font, labels[i], r[0] + r[2] / 2, r[1] + 5, Theme.TEXT)
        }

        // spin button
        val s = spinRect()
        val hovered = inside(mx, my, s[0], s[1], s[2], s[3]) && !spinning
        spinHover = Theme.approach(spinHover, if (hovered) 1f else 0f, dt)
        val base = if (spinning) Theme.CONTROL else Theme.mix(Theme.withAlpha(Theme.ACCENT, 0.7f), Theme.ACCENT, spinHover)
        g.roundRect(s[0], s[1], s[2], s[3], base, 3)
        g.frame(s[0], s[1], s[2], s[3], if (spinning) Theme.BORDER else Theme.mix(Theme.ACCENT, Theme.ACCENT_2, spinHover))
        g.centeredText(font, if (spinning) "Spinning..." else "SPIN", s[0] + s[2] / 2, s[1] + 8, if (spinning) Theme.MUTED else Theme.TEXT)
    }

    private fun drawFooter(g: GuiGraphicsExtractor) {
        g.centeredText(font, message, panelX + panelW / 2, panelY + panelH - 52 + 0, messageColor)
        val played = config.gamesPlayed
        val rate = if (played == 0) 0 else config.gamesWon * 100 / played
        g.scaledText(font, "Played $played   Won $rate%   Biggest win $${AmountParser.formatShort(config.biggestWin)}", panelX + 18, panelY + panelH - 24, Theme.MUTED, 0.8f)

        val d = doneRect()
        g.roundRect(d[0], d[1], d[2], d[3], Theme.withAlpha(Theme.ACCENT, 0.55f), 3)
        g.centeredText(font, "Done", d[0] + d[2] / 2, d[1] + 4, Theme.TEXT)
    }

    private fun itemFor(symbol: Symbol) = when (symbol) {
        Symbol.STAR -> Items.NETHER_STAR
        Symbol.DIAMOND -> Items.DIAMOND
        Symbol.EMERALD -> Items.EMERALD
        Symbol.GOLD -> Items.GOLD_INGOT
        Symbol.REDSTONE -> Items.REDSTONE
    }

    // ---- input ---------------------------------------------------------------------------------------------

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        val mx = event.x().toInt()
        val my = event.y().toInt()
        if (event.button() != MouseButton.LEFT) return super.mouseClicked(event, isDoubleClick)

        val d = doneRect()
        if (inside(mx, my, d[0], d[1], d[2], d[3])) {
            onClose()
            return true
        }
        if (inside(mx, my, panelX + 18, betY, 86, 18)) {
            betBox.isFocused = true
            betBox.mouseClicked(event, false)
            return true
        }
        betBox.isFocused = false

        if (spinning) return true
        val chips = chipRects()
        chips.forEachIndexed { i, r ->
            if (inside(mx, my, r[0], r[1], r[2], r[3])) {
                when (i) {
                    0 -> scaleBet(0.5)
                    1 -> scaleBet(2.0)
                    else -> betBox.value = config.balance.coerceAtLeast(1L).toString()
                }
                click(1.4f)
                return true
            }
        }
        val s = spinRect()
        if (inside(mx, my, s[0], s[1], s[2], s[3])) {
            spin()
            return true
        }
        return super.mouseClicked(event, isDoubleClick)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (betBox.isFocused) {
            if (event.key() == InputConstants.KEY_RETURN) {
                betBox.isFocused = false
                spin()
                return true
            }
            if (event.key() == InputConstants.KEY_ESCAPE) {
                betBox.isFocused = false
                return true
            }
            if (betBox.keyPressed(event)) return true
        } else if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_SPACE) {
            spin()
            return true
        }
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean =
        (betBox.isFocused && betBox.charTyped(event)) || super.charTyped(event)

    override fun isPauseScreen(): Boolean = false

    override fun onClose() {
        // Never lose a running spin: pay it out right away.
        if (spinning) {
            reels.forEach { it.spinning = false; it.position = it.end }
            finishSpin()
        }
        MagicDonutConfig.save()
        minecraft.gui.setScreen(parent)
    }
}
