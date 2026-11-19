package de.magicclutch.magicdonut.client.gui

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.util.AmountParser
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import java.util.Locale
import kotlin.math.roundToInt

/**
 * One line in a settings page: a label with a short description on the left and a control on the right.
 * Subclasses draw the control and handle its input; the base class draws the row itself.
 */
abstract class Row(val title: String, val description: String = "") {
    var bx = 0
    var by = 0
    var bw = 0
    var visible = false
    private var hover = 0f

    open val height: Int get() = if (description.isEmpty()) 26 else 32
    open val capturesKeys: Boolean get() = false

    protected val controlWidth = 150
    protected val controlX get() = bx + bw - controlWidth - 10
    protected val controlCenterY get() = by + height / 2

    open fun render(g: GuiGraphicsExtractor, font: Font, x: Int, y: Int, w: Int, mx: Int, my: Int, dt: Float) {
        bx = x
        by = y
        bw = w
        visible = true

        val hovered = inside(mx, my, x, y, w, height)
        hover = Theme.approach(hover, if (hovered) 1f else 0f, dt)

        g.roundRect(x, y, w, height, Theme.mix(Theme.ROW, Theme.ROW_HOVER, hover))
        if (hover > 0.02f) g.fill(x, y + 3, x + 2, y + height - 3, Theme.withAlpha(Theme.shimmer(), hover))

        drawLabels(g, font, hovered, mx, my)
        renderControl(g, font, mx, my, dt)
    }

    private fun drawLabels(g: GuiGraphicsExtractor, font: Font, hovered: Boolean, mx: Int, my: Int) {
        val textWidth = bw - controlWidth - 34
        val titleY = if (description.isEmpty()) by + (height - 8) / 2 else by + 6
        g.text(font, title, bx + 10, titleY, Theme.TEXT, false)
        if (description.isEmpty()) return

        val scale = 0.8f
        val limit = (textWidth / scale).toInt()
        val fits = font.width(description) <= limit
        val shown = if (fits) description else font.plainSubstrByWidth(description, limit - font.width("...")) + "..."
        g.scaledText(font, shown, bx + 10, by + 18, Theme.MUTED, scale)
        if (!fits && hovered && mx < controlX) {
            g.setTooltipForNextFrame(font, Component.literal(description), mx, my)
        }
    }

    protected abstract fun renderControl(g: GuiGraphicsExtractor, font: Font, mx: Int, my: Int, dt: Float)

    open fun mouseClicked(event: MouseButtonEvent): Boolean = false
    open fun mouseDragged(event: MouseButtonEvent): Boolean = false
    open fun mouseReleased() {}
    open fun keyPressed(event: KeyEvent): Boolean = false
    open fun charTyped(event: CharacterEvent): Boolean = false

    /** Called when another row (or nothing) takes the focus. */
    open fun blur() {}

    protected fun commit() = MagicDonutConfig.save()

    protected fun overRow(event: MouseButtonEvent): Boolean =
        visible && inside(event.x().toInt(), event.y().toInt(), bx, by, bw, height)
}

/** A section title between groups of rows. */
class HeaderRow(private val text: String) : Row(text) {
    override val height: Int get() = 22

    override fun render(g: GuiGraphicsExtractor, font: Font, x: Int, y: Int, w: Int, mx: Int, my: Int, dt: Float) {
        bx = x
        by = y
        bw = w
        visible = true
        g.text(font, text.uppercase(Locale.ROOT), x + 2, y + 9, Theme.ACCENT_2, false)
        val lineX = x + 6 + font.width(text.uppercase(Locale.ROOT))
        g.fillGradient(lineX, y + 13, x + w, y + 14, Theme.withAlpha(Theme.ACCENT, 0.7f), Theme.withAlpha(Theme.ACCENT, 0f))
    }

    override fun renderControl(g: GuiGraphicsExtractor, font: Font, mx: Int, my: Int, dt: Float) = Unit
}

/** A read-only line of text whose content can change, for example a status message. */
class InfoRow(private val text: () -> String, private val color: () -> Int = { Theme.MUTED }) : Row("") {
    override val height: Int get() = 20

    override fun render(g: GuiGraphicsExtractor, font: Font, x: Int, y: Int, w: Int, mx: Int, my: Int, dt: Float) {
        bx = x
        by = y
        bw = w
        visible = true
        g.roundRect(x, y, w, height, Theme.withAlpha(Theme.ROW, 0.6f))
        g.text(font, text(), x + 10, y + 6, color(), false)
    }

    override fun renderControl(g: GuiGraphicsExtractor, font: Font, mx: Int, my: Int, dt: Float) = Unit
}

class ToggleRow(
    title: String,
    description: String,
    private val get: () -> Boolean,
    private val set: (Boolean) -> Unit,
) : Row(title, description) {
    private var knob = if (get()) 1f else 0f

    override fun renderControl(g: GuiGraphicsExtractor, font: Font, mx: Int, my: Int, dt: Float) {
        knob = Theme.approach(knob, if (get()) 1f else 0f, dt, 18f)
        val w = 30
        val h = 14
        val x = bx + bw - w - 14
        val y = controlCenterY - h / 2
        g.roundRect(x, y, w, h, Theme.mix(Theme.CONTROL, Theme.ACCENT, knob), 7)
        g.frame(x, y, w, h, Theme.mix(Theme.BORDER, Theme.ACCENT_2, knob))
        val knobX = x + 2 + ((w - h) * knob).roundToInt()
        g.roundRect(knobX, y + 2, h - 4, h - 4, Theme.mix(Theme.MUTED, 0xFFFFFFFF.toInt(), knob), 5)
        val label = if (get()) "ON" else "OFF"
        g.rightText(font, label, x - 6, controlCenterY - 4, if (get()) Theme.GOOD else Theme.MUTED)
    }

    override fun mouseClicked(event: MouseButtonEvent): Boolean {
        if (event.button() != MouseButton.LEFT || !overRow(event)) return false
        set(!get())
        commit()
        return true
    }
}

class SliderRow(
    title: String,
    description: String,
    private val min: Float,
    private val max: Float,
    private val step: Float,
    private val unit: String,
    private val get: () -> Float,
    private val set: (Float) -> Unit,
) : Row(title, description) {
    private var dragging = false
    private var fill = 0f

    private val trackX get() = controlX + 50
    private val trackW get() = controlWidth - 50

    override fun renderControl(g: GuiGraphicsExtractor, font: Font, mx: Int, my: Int, dt: Float) {
        val value = get().coerceIn(min, max)
        fill = Theme.approach(fill, (value - min) / (max - min), dt, 22f)
        val y = controlCenterY
        g.text(font, format(value) + unit, controlX, y - 4, Theme.TEXT, false)

        g.roundRect(trackX, y - 2, trackW, 4, Theme.CONTROL, 2)
        val filled = (trackW * fill).roundToInt()
        g.fillGradient(trackX, y - 2, trackX + filled, y + 2, Theme.ACCENT, Theme.ACCENT_2)
        val knobX = trackX + filled - 3
        val active = dragging || inside(mx, my, trackX - 4, y - 8, trackW + 8, 16)
        g.roundRect(knobX, y - 5, 6, 10, if (active) 0xFFFFFFFF.toInt() else Theme.TEXT, 2)
    }

    private fun format(value: Float): String =
        if (step >= 1f || step == 0f && max - min >= 100f) value.roundToInt().toString()
        else "%.2f".format(Locale.ROOT, value).trimEnd('0').trimEnd('.')

    private fun applyMouse(mouseX: Double) {
        val raw = min + ((mouseX - trackX) / trackW).toFloat().coerceIn(0f, 1f) * (max - min)
        val snapped = if (step > 0f) (min + ((raw - min) / step).roundToInt() * step) else raw
        set(snapped.coerceIn(min, max))
    }

    override fun mouseClicked(event: MouseButtonEvent): Boolean {
        if (event.button() != MouseButton.LEFT || !visible) return false
        if (!inside(event.x().toInt(), event.y().toInt(), trackX - 6, controlCenterY - 9, trackW + 12, 18)) return false
        dragging = true
        applyMouse(event.x())
        return true
    }

    override fun mouseDragged(event: MouseButtonEvent): Boolean {
        if (!dragging) return false
        applyMouse(event.x())
        return true
    }

    override fun mouseReleased() {
        if (dragging) {
            dragging = false
            commit()
        }
    }
}

/** A single-line text field. Text is applied while typing and saved on Enter or when the field loses focus. */
open class TextRow(
    title: String,
    description: String,
    private val get: () -> String,
    private val set: (String) -> Unit,
    maxLength: Int = 64,
    private val validate: (String) -> Boolean = { true },
) : Row(title, description) {
    protected val box: EditBox = EditBox(Minecraft.getInstance().font, 0, 0, 100, 14, Component.literal(title)).apply {
        setBordered(false)
        setMaxLength(maxLength)
        setTextColor(Theme.TEXT)
        setValue(get())
    }
    private var dirty = false
    protected var focusAnim = 0f

    override val capturesKeys: Boolean get() = box.isFocused

    protected open val boxX get() = controlX
    protected open val boxW get() = controlWidth

    override fun renderControl(g: GuiGraphicsExtractor, font: Font, mx: Int, my: Int, dt: Float) {
        if (!box.isFocused) {
            val current = get()
            if (box.value != current) box.value = current
        }
        focusAnim = Theme.approach(focusAnim, if (box.isFocused) 1f else 0f, dt)
        val y = controlCenterY - 9
        g.roundRect(boxX, y, boxW, 18, Theme.CONTROL)
        g.frame(boxX, y, boxW, 18, Theme.mix(Theme.BORDER, Theme.ACCENT_2, focusAnim))
        box.x = boxX + 5
        box.y = y + 5
        box.width = boxW - 10
        box.extractRenderState(g, mx, my, dt)
    }

    override fun mouseClicked(event: MouseButtonEvent): Boolean {
        if (event.button() != MouseButton.LEFT || !visible) return false
        val y = controlCenterY - 9
        val onBox = inside(event.x().toInt(), event.y().toInt(), boxX, y, boxW, 18)
        if (!onBox) {
            blur()
            return false
        }
        box.isFocused = true
        box.mouseClicked(event, false)
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (!box.isFocused) return false
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN) {
            blur()
            return true
        }
        val handled = box.keyPressed(event)
        if (handled) applyText()
        return handled
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (!box.isFocused) return false
        val handled = box.charTyped(event)
        if (handled) applyText()
        return handled
    }

    private fun applyText() {
        if (validate(box.value)) {
            set(box.value)
            dirty = true
        }
    }

    override fun blur() {
        if (box.isFocused) box.isFocused = false
        if (dirty) {
            dirty = false
            commit()
        }
    }
}

/** A money amount edited as text, so values like `5m` or `2.5k` work. Invalid input is ignored. */
class AmountRow(
    title: String,
    description: String,
    get: () -> Long,
    set: (Long) -> Unit,
) : TextRow(
    title, description,
    get = { get().toString() },
    set = { text -> AmountParser.parse(text)?.let(set) },
    maxLength = 18,
    validate = { AmountParser.parse(it) != null },
)

class CycleRow<T>(
    title: String,
    description: String,
    private val values: List<T>,
    private val label: (T) -> String,
    private val get: () -> T,
    private val set: (T) -> Unit,
) : Row(title, description) {
    override fun renderControl(g: GuiGraphicsExtractor, font: Font, mx: Int, my: Int, dt: Float) {
        val y = controlCenterY - 9
        val hovered = inside(mx, my, controlX, y, controlWidth, 18)
        g.roundRect(controlX, y, controlWidth, 18, Theme.CONTROL)
        g.frame(controlX, y, controlWidth, 18, if (hovered) Theme.ACCENT else Theme.BORDER)
        g.text(font, "<", controlX + 6, y + 5, Theme.ACCENT_2, false)
        g.rightText(font, ">", controlX + controlWidth - 6, y + 5, Theme.ACCENT_2)
        g.centeredText(font, label(get()), controlX + controlWidth / 2, y + 5, Theme.TEXT)
    }

    override fun mouseClicked(event: MouseButtonEvent): Boolean {
        if (!visible || !inside(event.x().toInt(), event.y().toInt(), controlX, controlCenterY - 9, controlWidth, 18)) return false
        val index = values.indexOf(get()).coerceAtLeast(0)
        val next = when (event.button()) {
            MouseButton.LEFT -> index + 1
            MouseButton.RIGHT -> index - 1
            else -> return false
        }
        set(values[Math.floorMod(next, values.size)])
        commit()
        return true
    }
}

/** An ARGB color edited as hex text (`RRGGBB` or `AARRGGBB`), with a live swatch and a few presets. */
class ColorRow(
    title: String,
    description: String,
    private val get: () -> Int,
    private val set: (Int) -> Unit,
    private val alpha: Boolean = false,
) : TextRow(
    title, description,
    get = { hex(get(), alpha) },
    set = { text -> parse(text, alpha)?.let(set) },
    maxLength = 8,
    validate = { parse(it, alpha) != null },
) {
    private val presets = intArrayOf(0xFFFFFFFF.toInt(), Theme.ACCENT, Theme.ACCENT_2, Theme.GOOD, Theme.GOLD, Theme.BAD)

    override val boxX get() = controlX + 22 + presets.size * 0
    override val boxW get() = controlWidth - 22

    override fun renderControl(g: GuiGraphicsExtractor, font: Font, mx: Int, my: Int, dt: Float) {
        super.renderControl(g, font, mx, my, dt)
        val y = controlCenterY - 9
        g.roundRect(controlX, y, 18, 18, Theme.CONTROL)
        g.fill(controlX + 2, y + 2, controlX + 16, y + 16, get())
        g.frame(controlX, y, 18, 18, Theme.BORDER)
    }

    override fun mouseClicked(event: MouseButtonEvent): Boolean {
        if (event.button() == MouseButton.LEFT && visible && inside(event.x().toInt(), event.y().toInt(), controlX, controlCenterY - 9, 18, 18)) {
            // Clicking the swatch cycles through the presets.
            val current = get()
            val index = presets.indexOfFirst { (it and 0x00FFFFFF) == (current and 0x00FFFFFF) }
            val next = presets[(index + 1).mod(presets.size)]
            set(if (alpha) (current and 0xFF000000.toInt()) or (next and 0x00FFFFFF) else next)
            commit()
            return true
        }
        return super.mouseClicked(event)
    }

    private companion object {
        fun hex(color: Int, alpha: Boolean): String =
            if (alpha) "%08X".format(Locale.ROOT, color) else "%06X".format(Locale.ROOT, color and 0xFFFFFF)

        fun parse(text: String, alpha: Boolean): Int? {
            val clean = text.trim().removePrefix("#")
            if (!clean.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
            return when {
                clean.length == 6 -> clean.toInt(16) or 0xFF000000.toInt()
                clean.length == 8 && alpha -> clean.toLong(16).toInt()
                else -> null
            }
        }
    }
}

class ButtonRow(
    title: String,
    description: String,
    private val label: String,
    private val danger: Boolean = false,
    private val action: () -> Unit,
) : Row(title, description) {
    private var press = 0f

    override fun renderControl(g: GuiGraphicsExtractor, font: Font, mx: Int, my: Int, dt: Float) {
        val y = controlCenterY - 9
        val hovered = inside(mx, my, controlX, y, controlWidth, 18)
        press = Theme.approach(press, if (hovered) 1f else 0f, dt)
        val base = if (danger) Theme.BAD else Theme.ACCENT
        g.roundRect(controlX, y, controlWidth, 18, Theme.mix(Theme.CONTROL, Theme.withAlpha(base, 0.55f).let { Theme.mix(Theme.CONTROL, base, 0.55f) }, press))
        g.frame(controlX, y, controlWidth, 18, Theme.mix(Theme.BORDER, base, 0.4f + 0.6f * press))
        g.centeredText(font, label, controlX + controlWidth / 2, y + 5, Theme.TEXT)
    }

    override fun mouseClicked(event: MouseButtonEvent): Boolean {
        if (event.button() != MouseButton.LEFT || !visible || !inside(event.x().toInt(), event.y().toInt(), controlX, controlCenterY - 9, controlWidth, 18)) return false
        action()
        return true
    }
}
