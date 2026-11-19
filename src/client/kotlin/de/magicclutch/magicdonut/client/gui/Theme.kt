package de.magicclutch.magicdonut.client.gui

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

/** Colors and small drawing helpers shared by every MagicDonutUtils screen. */
object Theme {
    // Surfaces
    const val OVERLAY = 0xC8070710.toInt()
    const val WINDOW = 0xF00F0F1A.toInt()
    const val SIDEBAR = 0xFF0B0B15.toInt()
    const val ROW = 0xFF171726.toInt()
    const val ROW_HOVER = 0xFF1F1F33.toInt()
    const val CONTROL = 0xFF0E0E1A.toInt()
    const val BORDER = 0xFF2A2A44.toInt()

    // Accents
    const val ACCENT = 0xFF8B5CF6.toInt()
    const val ACCENT_2 = 0xFF22D3EE.toInt()
    const val GOLD = 0xFFFBBF24.toInt()

    // Text
    const val TEXT = 0xFFE9E9F5.toInt()
    const val MUTED = 0xFF8888A6.toInt()
    const val GOOD = 0xFF4ADE80.toInt()
    const val BAD = 0xFFF87171.toInt()

    fun mix(from: Int, to: Int, t: Float): Int {
        val f = t.coerceIn(0f, 1f)
        fun channel(shift: Int): Int {
            val a = (from shr shift) and 0xFF
            val b = (to shr shift) and 0xFF
            return (a + (b - a) * f).toInt().coerceIn(0, 255)
        }
        return (channel(24) shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    fun withAlpha(color: Int, alpha: Float): Int =
        ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24) or (color and 0x00FFFFFF)

    /** A slowly shifting purple-to-cyan color, used for the animated accent lines. */
    fun shimmer(offset: Float = 0f): Int {
        val t = ((System.currentTimeMillis() % 6000L) / 6000f + offset) % 1f
        val wave = (kotlin.math.sin(t * 2 * Math.PI).toFloat() + 1f) / 2f
        return mix(ACCENT, ACCENT_2, wave)
    }

    fun rainbow(offset: Float = 0f): Int =
        Color.HSBtoRGB(((System.currentTimeMillis() % 5000L) / 5000f + offset) % 1f, 0.55f, 1f)

    fun easeOutCubic(t: Float): Float {
        val x = 1f - t.coerceIn(0f, 1f)
        return 1f - x * x * x
    }

    /** Moves [current] towards [target] by an exponential smoothing step, frame rate independent. */
    fun approach(current: Float, target: Float, dt: Float, speed: Float = 14f): Float {
        val k = 1f - kotlin.math.exp(-speed * dt)
        return current + (target - current) * k
    }
}

/** A filled rectangle with the corners cut, which reads as rounded at GUI scale. */
fun GuiGraphicsExtractor.roundRect(x: Int, y: Int, w: Int, h: Int, color: Int, radius: Int = 2) {
    if (w <= 0 || h <= 0) return
    val r = min(radius, min(w, h) / 2)
    if (r <= 0) {
        fill(x, y, x + w, y + h, color)
        return
    }
    fill(x + r, y, x + w - r, y + h, color)
    fill(x, y + r, x + r, y + h - r, color)
    fill(x + w - r, y + r, x + w, y + h - r, color)
    if (r > 1) {
        // soften the diagonal a little
        fill(x + 1, y + 1, x + r, y + r, color)
        fill(x + w - r, y + 1, x + w - 1, y + r, color)
        fill(x + 1, y + h - r, x + r, y + h - 1, color)
        fill(x + w - r, y + h - r, x + w - 1, y + h - 1, color)
    }
}

fun GuiGraphicsExtractor.frame(x: Int, y: Int, w: Int, h: Int, color: Int) {
    fill(x, y, x + w, y + 1, color)
    fill(x, y + h - 1, x + w, y + h, color)
    fill(x, y, x + 1, y + h, color)
    fill(x + w - 1, y, x + w, y + h, color)
}

fun GuiGraphicsExtractor.scaledText(font: Font, text: String, x: Int, y: Int, color: Int, scale: Float, shadow: Boolean = false) {
    val p = pose()
    p.pushMatrix()
    p.translate(x.toFloat(), y.toFloat())
    p.scale(scale, scale)
    text(font, text, 0, 0, color, shadow)
    p.popMatrix()
}

fun GuiGraphicsExtractor.rightText(font: Font, text: String, rightX: Int, y: Int, color: Int, shadow: Boolean = false) {
    text(font, text, rightX - font.width(text), y, color, shadow)
}

fun inside(mx: Int, my: Int, x: Int, y: Int, w: Int, h: Int): Boolean =
    mx >= x && mx < x + w && my >= y && my < y + h

fun clampInt(value: Int, low: Int, high: Int): Int = max(low, min(high, value))
