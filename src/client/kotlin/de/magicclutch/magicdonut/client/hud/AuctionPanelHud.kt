package de.magicclutch.magicdonut.client.hud

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import de.magicclutch.magicdonut.client.auction.AuctionManager
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.util.AmountParser
import net.minecraft.client.Minecraft
import org.polyfrost.compose.composables.PolyBox
import org.polyfrost.compose.composables.PolyCanvas
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyPath
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The auction panel as a normal OneConfig HUD: OneConfig draws its background (color, rounding, border) and lets you
 * move and scale it. The content is drawn on one canvas whose size this class computes from the text, so the box is
 * always exactly as big as the content plus the same empty margin on all four sides, and everything is centered.
 */
class AuctionPanelHud : Hud("magicdonut_auction_panel", "Auction Panel", Category.INFO) {
    @Switch(title = "Show countdown ring", description = "The ring with the time in it. Off: the time is shown as text.")
    var showRing: Boolean = true

    @Switch(title = "Glowing outline", description = "A glowing line around the whole panel while the auction runs. It follows the color of the ring: green, orange, red.")
    var glowOutline: Boolean = true

    private class Line(val text: String, val argb: Int)

    private class View(val name: String, val time: String, val lines: List<Line>) {
        override fun equals(other: Any?): Boolean =
            other is View && name == other.name && time == other.time &&
                lines.size == other.lines.size && lines.indices.all { lines[it].text == other.lines[it].text && lines[it].argb == other.lines[it].argb }

        override fun hashCode(): Int = name.hashCode() * 31 + time.hashCode()
    }

    private var view: MutableState<View> = mutableStateOf(buildView())

    init {
        // The margin is part of the canvas, so the HUD's own padding stays 0.
        padLeft = 0f
        padRight = 0f
        padTop = 0f
        padBottom = 0f
        live = this
    }

    private fun buildView(): View {
        val running = AuctionManager.current
        val auction = running ?: AuctionManager.visibleFinished()
        val sold = auction != null && running == null
        val stack = auction?.item
        val name = if (stack != null) "${stack.count}x ${stack.hoverName.string}" else "1x Diamond"
        val time = timeText(sold, auction)
        val lines = ArrayList<Line>()
        if (!showRing) lines.add(Line(time, WHITE))
        val top = auction?.highest
        when {
            auction == null -> {
                lines.add(Line("Top bid: $1.5m", GREEN))
                lines.add(Line("by Steve", GRAY))
            }
            top != null -> {
                lines.add(Line((if (sold) "SOLD: " else "Top bid: ") + "$" + AmountParser.formatShort(top.amount), GREEN))
                lines.add(Line("by ${top.player}", GRAY))
            }
            else -> {
                lines.add(Line("Min bid: $" + AmountParser.formatShort(auction.minimumBid), GRAY))
                if (sold) lines.add(Line("No bids", GRAY))
            }
        }
        return View(name, time, lines)
    }

    private fun timeText(sold: Boolean, auction: AuctionManager.Auction?): String = when {
        auction == null -> "1:12"
        sold -> "SOLD"
        else -> auction.secondsLeft().let { "%d:%02d".format(it / 60, it % 60) }
    }

    override fun update(): Boolean {
        val next = buildView()
        if (next == view.value) return false
        view.value = next
        return true
    }

    /** Checked about ten times a second (in nanoseconds); the ring itself redraws every frame. */
    override fun updateFrequency(): Long = 100_000_000L

    @Composable
    override fun Content() {
        val v = view.value
        val scale = textScale
        val font = Minecraft.getInstance().font

        // Size of the content in HUD units (before the scale): widest line, and all heights added up.
        val lineWidths = v.lines.map { font.width(it.text).toFloat() }
        val contentWidth = maxOf(font.width(v.name).toFloat(), lineWidths.maxOrNull() ?: 0f, if (showRing) RING_SIZE else 0f, MIN_CONTENT_WIDTH)
        var contentHeight = LINE_HEIGHT                                        // the item name
        if (showRing) contentHeight += GAP + RING_SIZE
        if (v.lines.isNotEmpty()) contentHeight += GAP + v.lines.size * LINE_HEIGHT + (v.lines.size - 1) * LINE_GAP
        val boxWidth = (contentWidth + 2 * MARGIN) * scale
        val boxHeight = (contentHeight + 2 * MARGIN) * scale

        PolyBox(modifier = hudBackground()) {
            PolyCanvas(modifier = PolyModifier.size(boxWidth, boxHeight), live = true) { x, y, w, h ->
                if (glowOutline) drawGlow(x, y, w, h, scale)
                val center = x + w / 2f
                var cursor = y + MARGIN * scale

                // Heading: the item name, centered.
                mcText(v.name, center - font.width(v.name) * scale / 2f, cursor, WHITE, true, scale)
                cursor += (LINE_HEIGHT + GAP) * scale

                if (showRing) {
                    drawRing(center, cursor + RING_SIZE * scale / 2f, scale)
                    cursor += (RING_SIZE + GAP) * scale
                }
                for (line in v.lines) {
                    mcText(line.text, center - font.width(line.text) * scale / 2f, cursor, line.argb, true, scale)
                    cursor += (LINE_HEIGHT + LINE_GAP) * scale
                }
            }
        }
    }

    /** A glowing line around the whole box: a few soft, wide strokes under a thin bright one, gently pulsing. */
    private fun org.polyfrost.compose.render.RenderContext.drawGlow(x: Float, y: Float, w: Float, h: Float, scale: Float) {
        val auction = AuctionManager.current ?: AuctionManager.visibleFinished()
        val sold = auction != null && AuctionManager.current == null
        val fraction = when {
            auction == null -> 0.65f
            sold -> 1f
            else -> {
                val total = (auction.endsAtMs - auction.startedAtMs).coerceAtLeast(1L)
                ((auction.endsAtMs - System.currentTimeMillis()).coerceAtLeast(0L) / total.toFloat()).coerceIn(0f, 1f)
            }
        }
        val rgb = when {
            sold || fraction > 0.5f -> GREEN
            fraction > 0.2f -> ORANGE
            else -> RED
        } and 0xFFFFFF
        // Faster pulse the less time is left.
        val speed = if (fraction > 0.2f) 2.2 else 5.0
        val pulse = (0.75 + 0.25 * sin(System.currentTimeMillis() / 1000.0 * speed)).toFloat()
        val radius = 8f * scale
        val layers = arrayOf(floatArrayOf(7f, 0.10f), floatArrayOf(5f, 0.16f), floatArrayOf(3.2f, 0.28f), floatArrayOf(1.6f, 0.95f))
        for ((width, alpha) in layers) {
            val stroke = width * scale
            val inset = stroke / 2f
            val a = ((alpha * pulse).coerceIn(0f, 1f) * 255).toInt()
            rectStroke(x + inset, y + inset, w - stroke, h - stroke, PolyColor((a shl 24) or rgb), stroke, radius)
        }
    }

    /** The countdown ring: a vector shape redrawn every frame, so it is smooth at any size and runs down without steps. */
    private fun org.polyfrost.compose.render.RenderContext.drawRing(cx: Float, cy: Float, scale: Float) {
        val auction = AuctionManager.current ?: AuctionManager.visibleFinished()
        val sold = auction != null && AuctionManager.current == null
        val fraction = when {
            auction == null -> 0.65f
            sold -> if (auction.highest != null) 1f else 0f
            else -> {
                val total = (auction.endsAtMs - auction.startedAtMs).coerceAtLeast(1L)
                ((auction.endsAtMs - System.currentTimeMillis()).coerceAtLeast(0L) / total.toFloat()).coerceIn(0f, 1f)
            }
        }
        val color = when {
            sold || fraction > 0.5f -> GREEN
            fraction > 0.2f -> ORANGE
            else -> RED
        }
        val thickness = 4f * scale
        val radius = RING_SIZE * scale / 2f - thickness / 2f - 0.5f
        val outer = radius + thickness / 2f
        val inner = radius - thickness / 2f

        // The dim track: outer circle with the inner circle cut out.
        val track = PolyPath().apply {
            evenOdd = true
            circle(cx, cy, outer)
            circle(cx, cy, inner)
        }
        fillPath(track, PolyColor(0x33FFFFFF))

        // The arc, from the top clockwise, with round caps.
        if (fraction > 0.003f) {
            val start = -PI.toFloat() / 2f
            val end = start + fraction * 2f * PI.toFloat()
            val steps = (120 * fraction).toInt().coerceAtLeast(2)
            val arc = PolyPath()
            for (i in 0..steps) {
                val a = start + (end - start) * i / steps
                if (i == 0) arc.moveTo(cx + cos(a) * outer, cy + sin(a) * outer) else arc.lineTo(cx + cos(a) * outer, cy + sin(a) * outer)
            }
            for (i in steps downTo 0) {
                val a = start + (end - start) * i / steps
                arc.lineTo(cx + cos(a) * inner, cy + sin(a) * inner)
            }
            arc.close()
            fillPath(arc, PolyColor(color))
            if (fraction < 0.995f) {
                for (a in floatArrayOf(start, end)) {
                    fillPath(PolyPath().apply { circle(cx + cos(a) * radius, cy + sin(a) * radius, thickness / 2f) }, PolyColor(color))
                }
            }
        }

        // The time in the middle.
        val font = Minecraft.getInstance().font
        val time = timeText(sold, auction)
        // Under 10 seconds the time shakes, harder and harder towards zero.
        val (shakeX, shakeY) = de.magicclutch.magicdonut.client.auction.AuctionSounds.shake(auction)
        mcText(time, cx - font.width(time) * scale / 2f + shakeX * 2.5f * scale, cy - 4f * scale + shakeY * 2f * scale, WHITE, true, scale)
    }

    private fun PolyPath.circle(cx: Float, cy: Float, r: Float) {
        val steps = 48
        for (i in 0 until steps) {
            val a = 2f * PI.toFloat() * i / steps
            if (i == 0) moveTo(cx + cos(a) * r, cy + sin(a) * r) else lineTo(cx + cos(a) * r, cy + sin(a) * r)
        }
        close()
    }

    override fun shouldShow(): Boolean {
        if (HudManager.isEditing) return true
        if (Minecraft.getInstance().level == null) return false
        if (!MagicDonutConfig.instance.auctionConfig.enabled) return false
        return AuctionManager.current != null || AuctionManager.visibleFinished() != null
    }

    override fun setup() {
        super.setup()
        if (isReal) updateWhenChanged("showRing")
        update()
    }

    override fun showByDefault(): Boolean = true

    override fun defaultPosition(): Pair<Float, Float> = 330f to 70f

    override fun minimumSize(): Pair<Float, Float> = 60f to 30f

    override fun clone(): Hud = (super.clone() as AuctionPanelHud).also { it.view = mutableStateOf(it.buildView()) }

    companion object {
        private const val MARGIN = 10f           // empty space on all four sides
        private const val GAP = 6f
        private const val LINE_HEIGHT = 9f
        private const val LINE_GAP = 2f
        private const val RING_SIZE = 36f
        private const val MIN_CONTENT_WIDTH = 60f
        private const val WHITE = 0xFFFFFFFF.toInt()
        private const val GRAY = 0xFFAAAAAA.toInt()
        private const val GREEN = 0xFF55FF55.toInt()
        private const val ORANGE = 0xFFFFAA00.toInt()
        private const val RED = 0xFFFF5555.toInt()

        @JvmStatic
        var live: AuctionPanelHud? = null
            private set
    }
}
