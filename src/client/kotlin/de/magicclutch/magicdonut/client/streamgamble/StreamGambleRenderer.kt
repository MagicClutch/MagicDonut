package de.magicclutch.magicdonut.client.streamgamble

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.gui.Theme
import de.magicclutch.magicdonut.client.gui.frame
import de.magicclutch.magicdonut.client.gui.rightText
import de.magicclutch.magicdonut.client.gui.roundRect
import de.magicclutch.magicdonut.client.gui.scaledText
import de.magicclutch.magicdonut.client.streamgamble.StreamGambleManager.Phase
import de.magicclutch.magicdonut.client.util.AmountParser
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * The stream overlay: the player's name, the stake, the odds (always visible) and a strip of items that scrolls and slows
 * down onto the winner. Host cells show a nether star, player cells a diamond; their share on the strip matches the odds.
 */
object StreamGambleRenderer {
    private const val PANEL_W = 340
    private const val PANEL_H = 128
    private const val CELL = 36
    private const val STRIP_W = 312
    private const val STRIP_H = 44

    private val hostItem = ItemStack(Items.NETHER_STAR)
    private val playerItem = ItemStack(Items.DIAMOND)

    private var lastCell = -1

    fun register() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("magicdonut", "stream_gamble")
        ) { context, _ -> render(context) }
    }

    private fun render(g: GuiGraphicsExtractor) {
        val config = MagicDonutConfig.instance.streamGambleConfig
        if (!config.enabled) return
        val round = StreamGambleManager.current ?: return

        val mc = Minecraft.getInstance()
        if (mc.level == null) return
        val screenW = mc.window.guiScaledWidth
        val screenH = mc.window.guiScaledHeight
        if (screenW <= 0 || screenH <= 0) return

        val font = mc.font
        val scale = config.scale.coerceIn(0.5f, 2.5f)
        val pose = g.pose()
        pose.pushMatrix()
        try {
            pose.translate(screenW / 2f, screenH * config.y.coerceIn(0f, 0.85f))
            pose.scale(scale, scale)

            val x = -PANEL_W / 2
            g.roundRect(x - 1, -1, PANEL_W + 2, PANEL_H + 2, Theme.withAlpha(Theme.ACCENT, 0.55f), 4)
            g.roundRect(x, 0, PANEL_W, PANEL_H, 0xE60F0F1A.toInt(), 4)

            // Top line: label, host name and the odds, which are always shown.
            g.text(font, "STREAM GAMBLE", x + 10, 8, Theme.ACCENT_2, false)
            val hostPct = (StreamGambleManager.hostChance() * 100).roundToInt()
            g.rightText(font, "${StreamGambleManager.hostName()} $hostPct%   |   ${round.player} ${100 - hostPct}%", x + PANEL_W - 10, 8, Theme.TEXT)
            val barX = x + 10
            val barW = PANEL_W - 20
            val hostW = (barW * hostPct / 100f).roundToInt()
            g.fill(barX, 19, barX + hostW, 22, Theme.ACCENT)
            g.fill(barX + hostW, 19, barX + barW, 22, Theme.ACCENT_2)

            // Player and stake.
            g.scaledText(font, round.player, x + 10, 29, Theme.TEXT, 1.5f, true)
            val stake = "$${AmountParser.formatShort(round.stake)}"
            g.rightText(font, "paid  $stake", x + PANEL_W - 10, 31, Theme.GOLD)
            g.rightText(font, "wins ${config.payoutMultiplier.toString().trimEnd('0').trimEnd('.')}x = $${AmountParser.formatShort(StreamGambleManager.payoutFor(round))}", x + PANEL_W - 10, 42, Theme.MUTED)

            drawStrip(g, round, x + (PANEL_W - STRIP_W) / 2, 56)
            drawFooter(g, font, round, x)
        } finally {
            pose.popMatrix()
        }
    }

    private fun drawStrip(g: GuiGraphicsExtractor, round: StreamGambleManager.Round, left: Int, top: Int) {
        val font = Minecraft.getInstance().font
        g.roundRect(left - 2, top - 2, STRIP_W + 4, STRIP_H + 4, Theme.SIDEBAR, 3)

        val finalOffset = round.landing * CELL + CELL / 2f + round.jitter * CELL
        val position = when (round.phase) {
            Phase.ANNOUNCE -> 4 * CELL + CELL / 2f
            Phase.SPIN -> {
                val t = round.phaseProgress()
                val eased = 1f - (1f - t) * (1f - t) * (1f - t) * (1f - t) * (1f - t)
                val start = 4 * CELL + CELL / 2f
                start + (finalOffset - start) * eased
            }
            Phase.RESULT -> finalOffset
        }

        g.enableScissor(left, top, left + STRIP_W, top + STRIP_H)
        val first = floor((position - STRIP_W / 2f) / CELL).toInt() - 1
        val last = floor((position + STRIP_W / 2f) / CELL).toInt() + 1
        for (i in first..last) {
            if (i < 0 || i >= round.cells.size) continue
            val isHost = round.cells[i]
            val cx = left + STRIP_W / 2 + (i * CELL + CELL / 2f - position).roundToInt()
            val tint = if (isHost) Theme.ACCENT else Theme.ACCENT_2
            g.roundRect(cx - CELL / 2 + 1, top + 2, CELL - 2, STRIP_H - 4, Theme.withAlpha(tint, 0.22f), 3)
            g.frame(cx - CELL / 2 + 1, top + 2, CELL - 2, STRIP_H - 4, Theme.withAlpha(tint, 0.6f))

            val pose = g.pose()
            pose.pushMatrix()
            pose.translate((cx - 12).toFloat(), (top + STRIP_H / 2 - 12).toFloat())
            pose.scale(1.5f, 1.5f)
            g.item(if (isHost) hostItem else playerItem, 0, 0)
            pose.popMatrix()
        }
        g.fillGradient(left, top, left + 26, top + STRIP_H, 0xF00B0B15.toInt(), 0x000B0B15)
        g.fillGradient(left + STRIP_W - 26, top, left + STRIP_W, top + STRIP_H, 0x000B0B15, 0xF00B0B15.toInt())
        g.disableScissor()

        // Pointer in the middle.
        val centerX = left + STRIP_W / 2
        g.fill(centerX - 1, top - 5, centerX + 1, top + STRIP_H + 5, Theme.GOLD)
        g.fill(centerX - 4, top - 5, centerX + 4, top - 3, Theme.GOLD)
        g.fill(centerX - 4, top + STRIP_H + 3, centerX + 4, top + STRIP_H + 5, Theme.GOLD)

        if (round.phase == Phase.SPIN && MagicDonutConfig.instance.streamGambleConfig.soundEnabled) {
            val cell = floor(position / CELL).toInt()
            if (cell != lastCell) {
                lastCell = cell
                val pitch = 1.1f + (1f - abs(round.phaseProgress() - 0.5f) * 2f) * 0.4f
                Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, pitch))
            }
        }
        if (font.lineHeight < 0) return // keeps `font` referenced for future labels
    }

    private fun drawFooter(g: GuiGraphicsExtractor, font: net.minecraft.client.gui.Font, round: StreamGambleManager.Round, x: Int) {
        val y = PANEL_H - 18
        when (round.phase) {
            Phase.ANNOUNCE -> g.centeredText(font, "Spinning in a moment...", 0, y + 3, Theme.MUTED)
            Phase.SPIN -> g.centeredText(font, "Good luck!", 0, y + 3, Theme.MUTED)
            Phase.RESULT -> {
                val win = !round.hostWins
                val text = if (win) "${round.player} WINS $${AmountParser.formatShort(StreamGambleManager.payoutFor(round))}" else "${StreamGambleManager.hostName()} WINS"
                val color = if (win) Theme.GOOD else Theme.ACCENT
                val pulse = 0.65f + 0.35f * kotlin.math.sin(System.currentTimeMillis() / 160.0).toFloat()
                g.roundRect(x + 10, y - 2, PANEL_W - 20, 16, Theme.withAlpha(color, 0.25f * pulse), 3)
                g.centeredText(font, text, 0, y + 2, color)
            }
        }
        if (StreamGambleManager.queued > 0) {
            g.rightText(font, "+${StreamGambleManager.queued} waiting", x + PANEL_W - 10, PANEL_H - 12, Theme.MUTED)
        }
    }
}
