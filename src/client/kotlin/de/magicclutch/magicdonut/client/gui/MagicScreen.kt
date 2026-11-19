package de.magicclutch.magicdonut.client.gui

import com.mojang.blaze3d.platform.InputConstants
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Items
import net.minecraft.world.item.ItemStack
import kotlin.math.max

/**
 * The settings screen of MagicDonutUtils: a purple window with a sidebar, a search box, a home page with a tile for
 * every feature (quick toggles and play buttons) and one page of settings per feature.
 */
class MagicScreen(private val parent: Screen? = null) : Screen(Component.literal("MagicDonutUtils")) {
    private val pages: List<Page> = SettingsCatalog.pages(this)
    private var selected = HOME
    private var rows: List<Row> = emptyList()

    private var focused: Row? = null
    private var search: EditBox? = null
    private var searchText = ""

    private var scroll = 0f
    private var scrollTarget = 0f
    private var openProgress = 0f
    private var indicatorY = 0f
    private var lastFrameNanos = System.nanoTime()

    private val sideHover = FloatArray(pages.size + 1)
    private val tileHover = FloatArray(pages.size)
    private var doneHover = 0f

    private var winX = 0
    private var winY = 0
    private var winW = 0
    private var winH = 0

    private val sideW = 118
    private val contentX get() = winX + sideW + 12
    private val contentW get() = winW - sideW - 24
    private val contentTop get() = winY + 42
    private val contentBottom get() = winY + winH - 28
    private val version: String = FabricLoader.getInstance().getModContainer("magicdonututils")
        .map { it.metadata.version.friendlyString }.orElse("dev")

    override fun init() {
        winW = clampInt(width - 24, 300, 470)
        winH = clampInt(height - 24, 200, 290)
        winX = (width - winW) / 2
        winY = (height - winH) / 2

        search = EditBox(font, contentX + contentW - 124, winY + 12, 124, 16, Component.literal("Search")).also { box ->
            box.setBordered(false)
            box.setTextColor(Theme.TEXT)
            box.setHint(Component.literal("Search settings...").withStyle { it.withColor(Theme.MUTED and 0xFFFFFF) })
            box.setMaxLength(32)
            box.value = searchText
            box.setResponder {
                searchText = it
                rebuildRows()
                scrollTarget = 0f
                scroll = 0f
            }
        }
        rebuildRows()
        indicatorY = entryY(indexOfEntry(selected)).toFloat()
    }

    // ---- page and row management -------------------------------------------------------------------------

    private fun rebuildRows() {
        val query = searchText.trim().lowercase()
        rows = when {
            query.isNotEmpty() -> pages.flatMap { page ->
                val hits = page.rows.filter { it !is HeaderRow && it !is InfoRow && matches(it, query) }
                if (hits.isEmpty()) emptyList() else listOf<Row>(HeaderRow(page.name)) + hits
            }
            selected >= 0 -> pages[selected].rows
            else -> emptyList()
        }
    }

    private fun matches(row: Row, query: String): Boolean =
        row.title.lowercase().contains(query) || row.description.lowercase().contains(query)

    private fun select(page: Int) {
        blurAll()
        selected = page
        searchText = ""
        search?.value = ""
        scroll = 0f
        scrollTarget = 0f
        rebuildRows()
    }

    private fun blurAll() {
        focused?.blur()
        focused = null
        search?.isFocused = false
    }

    private val searching get() = searchText.isNotBlank()

    // ---- layout helpers ------------------------------------------------------------------------------------

    private fun indexOfEntry(page: Int) = page + 1 // entry 0 is Home
    private fun entryY(entry: Int) = winY + 48 + entry * 24

    private val tileColumns = 2
    private val tileHeight = 46
    private val tileGap = 6
    private fun tileWidth() = (contentW - tileGap) / tileColumns

    // ---- rendering -----------------------------------------------------------------------------------------

    override fun extractBackground(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        context.fill(0, 0, width, height, Theme.OVERLAY)
    }

    override fun extractRenderState(g: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(g, mouseX, mouseY, delta)

        val now = System.nanoTime()
        val dt = ((now - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameNanos = now
        openProgress = (openProgress + dt * 4.5f).coerceAtMost(1f)
        scroll = Theme.approach(scroll, scrollTarget, dt, 16f)

        val slide = ((1f - Theme.easeOutCubic(openProgress)) * 16).toInt()
        val p = g.pose()
        p.pushMatrix()
        p.translate(0f, slide.toFloat())
        try {
            drawWindow(g)
            drawSidebar(g, mouseX, mouseY - slide, dt)
            drawHeader(g, mouseX, mouseY - slide, dt)
            drawContent(g, mouseX, mouseY - slide, dt)
            drawFooter(g, mouseX, mouseY - slide, dt)
        } finally {
            p.popMatrix()
        }
    }

    private fun drawWindow(g: GuiGraphicsExtractor) {
        g.roundRect(winX - 1, winY - 1, winW + 2, winH + 2, Theme.withAlpha(Theme.ACCENT, 0.45f), 4)
        g.roundRect(winX, winY, winW, winH, Theme.WINDOW, 4)
        g.roundRect(winX, winY, sideW, winH, Theme.SIDEBAR, 4)
        g.fill(winX + sideW - 2, winY, winX + sideW, winY + winH, Theme.SIDEBAR)
        g.fill(winX + sideW, winY + 4, winX + sideW + 1, winY + winH - 4, Theme.BORDER)
        // animated accent line along the top edge
        var x = winX + 4
        while (x < winX + winW - 4) {
            g.fill(x, winY, minOf(x + 6, winX + winW - 4), winY + 1, Theme.shimmer((x - winX) / winW.toFloat() * 0.6f))
            x += 6
        }
    }

    private fun drawSidebar(g: GuiGraphicsExtractor, mx: Int, my: Int, dt: Float) {
        g.scaledText(font, "MagicDonut", winX + 12, winY + 12, Theme.shimmer(), 1.35f, true)
        g.scaledText(font, "UTILS  v$version", winX + 13, winY + 28, Theme.MUTED, 0.75f)

        indicatorY = Theme.approach(indicatorY, entryY(indexOfEntry(if (searching) HOME else selected)).toFloat(), dt, 16f)
        if (!searching) {
            g.roundRect(winX + 6, indicatorY.toInt(), sideW - 12, 22, Theme.withAlpha(Theme.ACCENT, 0.28f), 3)
            g.fill(winX + 6, indicatorY.toInt() + 3, winX + 8, indicatorY.toInt() + 19, Theme.ACCENT)
        }

        for (entry in 0..pages.size) {
            val y = entryY(entry)
            val hovered = inside(mx, my, winX + 6, y, sideW - 12, 22)
            sideHover[entry] = Theme.approach(sideHover[entry], if (hovered) 1f else 0f, dt)
            if (sideHover[entry] > 0.02f) g.roundRect(winX + 6, y, sideW - 12, 22, Theme.withAlpha(0xFFFFFFFF.toInt(), 0.06f * sideHover[entry]), 3)

            val icon = if (entry == 0) Items.COMPASS else pages[entry - 1].icon
            val label = if (entry == 0) "Home" else pages[entry - 1].name
            val active = !searching && entry == indexOfEntry(selected)
            g.item(ItemStack(icon), winX + 12, y + 3)
            g.text(font, label, winX + 34, y + 7, if (active) Theme.TEXT else Theme.mix(Theme.MUTED, Theme.TEXT, sideHover[entry]), false)
        }
    }

    private fun drawHeader(g: GuiGraphicsExtractor, mx: Int, my: Int, dt: Float) {
        val title = when {
            searching -> "Search results"
            selected == HOME -> "Home"
            else -> pages[selected].name
        }
        g.scaledText(font, title, contentX, winY + 12, Theme.TEXT, 1.4f, true)
        g.fillGradient(contentX, winY + 33, contentX + contentW, winY + 34, Theme.withAlpha(Theme.ACCENT, 0.9f), Theme.withAlpha(Theme.ACCENT_2, 0f))

        val box = search ?: return
        val x = contentX + contentW - 130
        g.roundRect(x, winY + 9, 130, 20, Theme.CONTROL)
        g.frame(x, winY + 9, 130, 20, if (box.isFocused) Theme.ACCENT_2 else Theme.BORDER)
        box.x = x + 6
        box.y = winY + 15
        box.width = 118
        box.extractRenderState(g, mx, my, dt)
    }

    private fun drawContent(g: GuiGraphicsExtractor, mx: Int, my: Int, dt: Float) {
        val top = contentTop
        val bottom = contentBottom
        val viewH = bottom - top
        pages.forEach { page -> page.rows.forEach { it.visible = false } }
        rows.forEach { it.visible = false }

        g.enableScissor(contentX - 2, top, contentX + contentW + 8, bottom)
        val total = if (selected == HOME && !searching) drawHome(g, mx, my, dt, top) else drawRows(g, mx, my, dt, top)
        g.disableScissor()

        val maxScroll = max(0, total - viewH)
        scrollTarget = scrollTarget.coerceIn(0f, maxScroll.toFloat())
        scroll = scroll.coerceIn(0f, maxScroll.toFloat() + 1f)

        if (maxScroll > 0) {
            val barH = max(16, viewH * viewH / total)
            val barY = top + ((viewH - barH) * (scroll / maxScroll)).toInt()
            g.roundRect(contentX + contentW + 4, top, 3, viewH, Theme.withAlpha(Theme.CONTROL, 0.8f), 1)
            g.roundRect(contentX + contentW + 4, barY, 3, barH, Theme.ACCENT, 1)
        }
        if (rows.isEmpty() && (searching || selected != HOME)) {
            g.centeredText(font, "Nothing found", contentX + contentW / 2, top + 20, Theme.MUTED)
        }
    }

    private fun drawRows(g: GuiGraphicsExtractor, mx: Int, my: Int, dt: Float, top: Int): Int {
        val insideView = my in contentTop until contentBottom
        var y = top - scroll.toInt()
        var total = 0
        for (row in rows) {
            val h = row.height
            if (y + h > top && y < contentBottom) {
                row.render(g, font, contentX, y, contentW, if (insideView) mx else -9999, if (insideView) my else -9999, dt)
            }
            y += h + 4
            total += h + 4
        }
        return total
    }

    private fun drawHome(g: GuiGraphicsExtractor, mx: Int, my: Int, dt: Float, top: Int): Int {
        val tw = tileWidth()
        val insideView = my in contentTop until contentBottom
        for ((index, page) in pages.withIndex()) {
            val col = index % tileColumns
            val rowIndex = index / tileColumns
            val x = contentX + col * (tw + tileGap)
            val y = top - scroll.toInt() + rowIndex * (tileHeight + tileGap)
            if (y + tileHeight < top || y > contentBottom) continue

            val hovered = insideView && inside(mx, my, x, y, tw, tileHeight)
            tileHover[index] = Theme.approach(tileHover[index], if (hovered) 1f else 0f, dt)
            val hv = tileHover[index]

            g.roundRect(x, y, tw, tileHeight, Theme.mix(Theme.ROW, Theme.ROW_HOVER, hv), 3)
            g.frame(x, y, tw, tileHeight, Theme.mix(Theme.BORDER, Theme.ACCENT, hv))
            g.fillGradient(x + 1, y + tileHeight - 2, x + tw - 1, y + tileHeight - 1, Theme.withAlpha(Theme.ACCENT, hv * 0.9f), Theme.withAlpha(Theme.ACCENT_2, hv * 0.9f))

            val p = g.pose()
            p.pushMatrix()
            p.translate((x + 10).toFloat(), (y + 11).toFloat())
            p.scale(1.5f, 1.5f)
            g.item(ItemStack(page.icon), 0, 0)
            p.popMatrix()

            g.text(font, page.name, x + 42, y + 11, Theme.TEXT, false)
            g.scaledText(font, page.summary, x + 42, y + 24, Theme.MUTED, 0.8f)

            val toggle = page.toggle
            val launch = page.launch
            when {
                toggle != null -> {
                    val on = toggle.first()
                    val label = if (on) "ON" else "OFF"
                    val pillX = x + tw - 36
                    g.roundRect(pillX, y + 6, 30, 13, if (on) Theme.withAlpha(Theme.GOOD, 0.3f) else Theme.CONTROL, 6)
                    g.frame(pillX, y + 6, 30, 13, if (on) Theme.GOOD else Theme.BORDER)
                    g.centeredText(font, label, pillX + 15, y + 9, if (on) Theme.GOOD else Theme.MUTED)
                }
                launch != null -> {
                    val pillX = x + tw - 42
                    g.roundRect(pillX, y + 6, 36, 13, Theme.withAlpha(Theme.ACCENT, 0.5f + 0.3f * hv), 6)
                    g.centeredText(font, launch.first, pillX + 18, y + 9, Theme.TEXT)
                }
            }
        }
        val tileRows = (pages.size + tileColumns - 1) / tileColumns
        return tileRows * (tileHeight + tileGap)
    }

    private fun drawFooter(g: GuiGraphicsExtractor, mx: Int, my: Int, dt: Float) {
        val y = winY + winH - 22
        g.fill(winX + sideW + 1, y - 3, winX + winW - 4, y - 2, Theme.BORDER)
        g.text(font, "Changes are saved automatically", contentX, y + 4, Theme.MUTED, false)

        val bx = winX + winW - 74
        val hovered = inside(mx, my, bx, y, 62, 16)
        doneHover = Theme.approach(doneHover, if (hovered) 1f else 0f, dt)
        g.roundRect(bx, y, 62, 16, Theme.mix(Theme.withAlpha(Theme.ACCENT, 0.55f), Theme.ACCENT, doneHover), 3)
        g.centeredText(font, "Done", bx + 31, y + 4, Theme.TEXT)
    }

    // ---- input ---------------------------------------------------------------------------------------------

    private fun localMouseY(event: MouseButtonEvent): Int {
        val slide = ((1f - Theme.easeOutCubic(openProgress)) * 16).toInt()
        return event.y().toInt() - slide
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        if (event.button() != MouseButton.LEFT && event.button() != MouseButton.RIGHT) return super.mouseClicked(event, isDoubleClick)
        val mx = event.x().toInt()
        val my = localMouseY(event)

        // Sidebar
        if (inside(mx, my, winX + 6, entryY(0), sideW - 12, (pages.size + 1) * 24)) {
            val entry = (my - entryY(0)) / 24
            if (entry in 0..pages.size) {
                select(entry - 1)
                return true
            }
        }

        // Footer button
        if (inside(mx, my, winX + winW - 74, winY + winH - 22, 62, 16)) {
            onClose()
            return true
        }

        // Search box
        val box = search
        if (box != null && inside(mx, my, contentX + contentW - 130, winY + 9, 130, 20)) {
            focused?.blur()
            focused = null
            box.isFocused = true
            box.mouseClicked(adjusted(event), false)
            return true
        }
        box?.isFocused = false

        // Content
        if (inside(mx, my, contentX, contentTop, contentW, contentBottom - contentTop)) {
            if (selected == HOME && !searching) return clickHome(mx, my, event.button())

            val adj = adjusted(event)
            for (row in rows) {
                if (!row.visible) continue
                if (row.mouseClicked(adj)) {
                    if (focused !== row) focused?.blur()
                    focused = if (row.capturesKeys) row else null
                    return true
                }
            }
            focused?.blur()
            focused = null
            return true
        }
        return super.mouseClicked(event, isDoubleClick)
    }

    /** The same event, with the y coordinate undone from the opening slide animation. */
    private fun adjusted(event: MouseButtonEvent): MouseButtonEvent {
        val slide = ((1f - Theme.easeOutCubic(openProgress)) * 16).toInt()
        return if (slide == 0) event else MouseButtonEvent(event.x(), event.y() - slide, event.buttonInfo())
    }

    private fun clickHome(mx: Int, my: Int, button: Int): Boolean {
        val tw = tileWidth()
        for ((index, page) in pages.withIndex()) {
            val x = contentX + (index % tileColumns) * (tw + tileGap)
            val y = contentTop - scroll.toInt() + (index / tileColumns) * (tileHeight + tileGap)
            if (!inside(mx, my, x, y, tw, tileHeight)) continue

            val toggle = page.toggle
            val launch = page.launch
            if (button == MouseButton.LEFT && toggle != null && inside(mx, my, x + tw - 38, y + 4, 34, 17)) {
                toggle.second(!toggle.first())
            } else if (button == MouseButton.LEFT && launch != null && inside(mx, my, x + tw - 44, y + 4, 40, 17)) {
                launch.second()
            } else {
                select(index)
            }
            return true
        }
        return true
    }

    override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        val adj = adjusted(event)
        var handled = false
        for (row in rows) if (row.visible && row.mouseDragged(adj)) handled = true
        return handled || super.mouseDragged(event, deltaX, deltaY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        rows.forEach { if (it.visible) it.mouseReleased() }
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        scrollTarget -= (verticalAmount * 26).toFloat()
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val box = search
        if (box != null && box.isFocused) {
            if (event.key() == InputConstants.KEY_ESCAPE || event.key() == InputConstants.KEY_RETURN) {
                box.isFocused = false
                return true
            }
            return box.keyPressed(event)
        }
        val row = focused
        if (row != null && row.capturesKeys) {
            if (event.key() == InputConstants.KEY_ESCAPE) {
                row.blur()
                focused = null
                return true
            }
            if (row.keyPressed(event)) return true
        }
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        val box = search
        if (box != null && box.isFocused) return box.charTyped(event)
        return focused?.charTyped(event) == true || super.charTyped(event)
    }

    override fun isPauseScreen(): Boolean = false

    override fun onClose() {
        blurAll()
        MagicDonutConfig.save()
        Minecraft.getInstance().gui.setScreen(parent)
    }

    private companion object {
        const val HOME = -1
    }
}
