package de.magicclutch.magicdonut.client.gui

import de.magicclutch.magicdonut.client.auction.AuctionManager
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.config.configuration.DevelopmentConfig
import de.magicclutch.magicdonut.client.config.configuration.OverheadStatsConfig
import de.magicclutch.magicdonut.client.donation.PositionEditScreen
import de.magicclutch.magicdonut.client.gamble.GambleScreen
import de.magicclutch.magicdonut.client.profit.ProfitTracker
import de.magicclutch.magicdonut.client.service.APIService
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import kotlin.math.roundToInt

/** A settings page. [toggle] and [launch] power the quick actions on the home screen tiles. */
class Page(
    val name: String,
    val icon: Item,
    val summary: String,
    val rows: List<Row>,
    val toggle: Pair<() -> Boolean, (Boolean) -> Unit>? = null,
    val launch: Pair<String, () -> Unit>? = null,
)

/** Collects the rows of one page. Every control reads and writes the live [MagicDonutConfig]. */
class PageBuilder {
    val rows = mutableListOf<Row>()

    fun header(text: String) { rows += HeaderRow(text) }

    fun info(color: () -> Int = { Theme.MUTED }, text: () -> String) { rows += InfoRow(text, color) }

    fun toggle(title: String, description: String, get: () -> Boolean, set: (Boolean) -> Unit) {
        rows += ToggleRow(title, description, get, set)
    }

    fun slider(title: String, description: String, min: Float, max: Float, step: Float, unit: String = "", get: () -> Float, set: (Float) -> Unit) {
        rows += SliderRow(title, description, min, max, step, unit, get, set)
    }

    fun intSlider(title: String, description: String, min: Int, max: Int, step: Int = 1, unit: String = "", get: () -> Int, set: (Int) -> Unit) {
        rows += SliderRow(title, description, min.toFloat(), max.toFloat(), step.toFloat(), unit, { get().toFloat() }, { set(it.roundToInt()) })
    }

    fun text(title: String, description: String, maxLength: Int = 64, get: () -> String, set: (String) -> Unit) {
        rows += TextRow(title, description, get, set, maxLength)
    }

    fun amount(title: String, description: String, get: () -> Long, set: (Long) -> Unit) {
        rows += AmountRow(title, description, get, set)
    }

    fun <T> cycle(title: String, description: String, values: List<T>, label: (T) -> String, get: () -> T, set: (T) -> Unit) {
        rows += CycleRow(title, description, values, label, get, set)
    }

    fun color(title: String, description: String, alpha: Boolean = false, get: () -> Int, set: (Int) -> Unit) {
        rows += ColorRow(title, description, get, set, alpha)
    }

    fun button(title: String, description: String, label: String, danger: Boolean = false, action: () -> Unit) {
        rows += ButtonRow(title, description, label, danger, action)
    }
}

object SettingsCatalog {
    private fun page(
        name: String,
        icon: Item,
        summary: String,
        toggle: Pair<() -> Boolean, (Boolean) -> Unit>? = null,
        launch: Pair<String, () -> Unit>? = null,
        block: PageBuilder.() -> Unit,
    ): Page =
        Page(name, icon, summary, PageBuilder().apply(block).rows, toggle, launch)

    private fun open(screen: Screen) {
        val client = Minecraft.getInstance()
        client.execute { client.gui.setScreen(screen) }
    }

    fun pages(current: Screen): List<Page> {
        val c = MagicDonutConfig.instance
        return listOf(
            payConfirm(c),
            donation(c),
            overheadStats(c),
            auction(c),
            gamble(c, current),
            dailyBalance(c),
            developer(c),
            commands(),
        )
    }

    private fun payConfirm(c: MagicDonutConfig) = page("Pay Confirm", Items.GOLD_INGOT, "Confirm big payments", ({ c.payConfirmConfig.enabled } to { v: Boolean -> c.payConfirmConfig.enabled = v; MagicDonutConfig.save() })) {
        val p = c.payConfirmConfig
        header("Safety")
        toggle("Confirm large payments", "Ask for a click before big /pay commands go out.", { p.enabled }, { p.enabled = it })
        amount("Confirm from amount", "Payments of at least this much need confirmation. Accepts 5000000, 5m or 2.5k.", { p.confirmThreshold }, { p.confirmThreshold = it })
        intSlider("Confirmation timeout", "How long a pending payment stays confirmable.", 10, 300, 5, "s", { p.timeoutSeconds }, { p.timeoutSeconds = it })
    }

    private fun donation(c: MagicDonutConfig) = page("Donation HUD", Items.DIAMOND, "Highest donation on screen", ({ c.donationConfig.enabled } to { v: Boolean -> c.donationConfig.enabled = v; MagicDonutConfig.save() })) {
        val d = c.donationConfig
        header("General")
        toggle("Show HUD", "Your highest received donation, on screen.", { d.enabled }, { d.enabled = it })
        button("Move on screen", "Drag the HUD with the mouse, scroll to resize.", "Open editor") { open(PositionEditScreen()) }
        button("Reset highscore", "Forget the highest donation so far.", "Reset", danger = true) { d.reset() }
        header("Layout")
        slider("Horizontal position", "0 is the left edge, 1 the right edge.", 0f, 1f, 0.01f, "", { d.x }, { d.x = it })
        slider("Vertical position", "0 is the top edge, 1 the bottom edge.", 0f, 1f, 0.01f, "", { d.y }, { d.y = it })
        slider("Size", "Scale of the whole HUD.", 0.5f, 3f, 0.1f, "x", { d.size }, { d.size = it })
        header("Appearance")
        text("Prefix", "Text shown before the donation.", get = { d.customTitle }, set = { d.customTitle = it })
        text("No-donation text", "Shown until the first donation arrives.", get = { d.noDonationText }, set = { d.noDonationText = it })
        color("Text color", "Hex color of the text. Click the swatch to try presets.", get = { d.color }, set = { d.color = it })
        toggle("Background box", "Draw a box behind the text.", { d.showBox }, { d.showBox = it })
        color("Box color", "Hex with alpha (AARRGGBB).", alpha = true, get = { d.boxColor }, set = { d.boxColor = it })
    }

    private fun overheadStats(c: MagicDonutConfig) = page("Overhead Stats", Items.NAME_TAG, "Stats above name tags", ({ c.overheadStatsConfig.enabled } to { v: Boolean -> c.overheadStatsConfig.enabled = v; MagicDonutConfig.save() })) {
        val s = c.overheadStatsConfig
        header("General")
        toggle("Show stats", "Stats above player name tags. Needs an API key or a stats proxy.", { s.enabled }, { s.enabled = it })
        cycle("Display mode", "One fixed stat, or cycle through all of them.", OverheadStatsConfig.DisplayMode.entries,
            { it.name.lowercase().replaceFirstChar(Char::uppercase) }, { s.displayMode }, { s.displayMode = it })
        cycle("Stat", "The stat shown in fixed mode.", OverheadStatsConfig.StatField.entries, { it.displayName }, { s.stat }, { s.stat = it })
        intSlider("Cycle interval", "Seconds per stat in cycle mode.", 1, 30, 1, "s", { s.cycleSeconds }, { s.cycleSeconds = it })
        header("Layout")
        slider("Height", "Distance above the name tag.", -2f, 4f, 0.05f, "", { s.statYOffset }, { s.statYOffset = it })
        slider("Scale", "Text size relative to the vanilla name tag.", 0.2f, 2f, 0.05f, "x", { s.statScale }, { s.statScale = it })
        header("Performance")
        intSlider("Requests per minute", "Global cap for API requests. Lower it if you hit rate limits.", 1, 250, 1, "", { s.maxRequestsPerMinute }, { s.maxRequestsPerMinute = it })
    }

    private fun auction(c: MagicDonutConfig) = page("Auction", Items.BELL, "Hologram auctions", ({ c.auctionConfig.enabled } to { v: Boolean -> c.auctionConfig.enabled = v; MagicDonutConfig.save() })) {
        val a = c.auctionConfig
        header("How it works")
        info { "Hold an item, run /auction start. The highest payment to you wins." }
        header("General")
        toggle("Show hologram panel", "The floating item panel while an auction runs.", { a.enabled }, { a.enabled = it })
        intSlider("Default duration", "Used when /auction start has no duration.", 10, 900, 10, "s", { a.defaultDurationSeconds }, { a.defaultDurationSeconds = it })
        amount("Default minimum bid", "Payments below this are ignored. Accepts 1000, 1k or 2.5m.", { a.defaultMinimumBid }, { a.defaultMinimumBid = it })
        toggle("Sound on new top bid", "A short sound when someone takes the lead.", { a.playBidSound }, { a.playBidSound = it })
        header("Hologram")
        slider("Horizontal position", "0 is the left edge, 1 the right edge.", 0f, 1f, 0.01f, "", { a.x }, { a.x = it })
        slider("Vertical position", "0 is the top edge, 1 the bottom edge.", 0f, 1f, 0.01f, "", { a.y }, { a.y = it })
        slider("Scale", "Size of the panel.", 0.5f, 3f, 0.1f, "x", { a.scale }, { a.scale = it })
        toggle("Floating animation", "The item gently bobs up and down.", { a.bobbingItem }, { a.bobbingItem = it })
        header("Control")
        button("Cancel running auction", "Stops the current auction without a winner.", "Cancel", danger = true) { AuctionManager.cancel() }
    }

    private fun gamble(c: MagicDonutConfig, current: Screen) = page("Gamble", Items.NETHER_STAR, "Slot machine with items", launch = ("Play" to { open(GambleScreen(current)) })) {
        val g = c.gambleConfig
        header("Slot machine")
        info { "Virtual money only. Nothing here touches the server." }
        button("Play", "Open the slot machine. Also available as /gamble.", "Spin!") { open(GambleScreen(current)) }
        amount("Starting balance", "Balance after Reset balance. Accepts 10000 or 10k.", { g.startingBalance }, { g.startingBalance = it.coerceAtLeast(1L) })
        toggle("Sounds", "Spin, win and lose sounds.", { g.soundEnabled }, { g.soundEnabled = it })
        header("Data")
        button("Reset balance and stats", "Back to the starting balance, stats cleared.", "Reset", danger = true) {
            g.resetStats()
            MagicDonutConfig.save()
        }
    }

    private fun dailyBalance(c: MagicDonutConfig) = page("Daily Balance", Items.CLOCK, "Today's profit HUD", ({ c.profitConfig.enabled } to { v: Boolean -> c.profitConfig.enabled = v; MagicDonutConfig.save() })) {
        val p = c.profitConfig
        header("HUD")
        toggle("Show HUD", "Balance and today's profit above the hotbar. Needs an API key or a stats proxy.", { p.enabled }, { p.enabled = it })
        toggle("Show balance", "Your current balance.", { p.showBalance }, { p.showBalance = it })
        toggle("Show today's profit", "Gain or loss since the starting point.", { p.showProfit }, { p.showProfit = it })
        intSlider("Refresh interval", "Each refresh is one stats request for your account.", 10, 300, 5, "s", { p.refreshSeconds }, { p.refreshSeconds = it })
        intSlider("Height above screen bottom", "Move the HUD up or down.", 20, 200, 2, "", { p.hudBottomOffset }, { p.hudBottomOffset = it })
        header("Starting point")
        button("Set to current balance", "Today's profit starts from zero. Also /dailybalance start.", "Set") { ProfitTracker.startToday() }
        button("Clear starting point", "The next refresh becomes the new starting point.", "Clear", danger = true) { ProfitTracker.resetBaseline() }
    }

    private fun developer(c: MagicDonutConfig) = page("Developer", Items.COMMAND_BLOCK, "API key and proxy") {
        val d = c.developmentConfig
        header("Status")
        info(
            color = { if (APIService.isAvailable()) Theme.GOOD else Theme.GOLD },
            text = {
                when {
                    APIService.isProxyMode() -> "Using a stats proxy: you are verified with your Minecraft account."
                    APIService.hasApiKey() -> "API key set: stats features are available."
                    else -> "No API key: stats features are off. Everything else works without one."
                }
            }
        )
        header("API")
        text("API key", "Your personal DonutSMP API key. Never share it. Leave empty with a proxy.", 64, { d.apiKey }, { d.apiKey = it.trim() })
        text("API base URL", "Only change this to a proxy you trust. Default: ${DevelopmentConfig.DEFAULT_API_BASE_URL}", 128, { d.apiBaseUrl }, { d.apiBaseUrl = it.trim() })
    }

    private fun commands() = page("Commands", Items.BOOK, "All chat commands") {
        header("Chat commands")
        info { "/pay <player> <amount>      Safer /pay with confirmation" }
        info { "/auction start [s] [minBid] Auction the item in your hand" }
        info { "/auction status | end | cancel | announce" }
        info { "/gamble                     Open the slot machine" }
        info { "/dailybalance [start|reset|toggle]" }
        info { "/donation reset             Reset the donation highscore" }
        info { "/magicdonututils            Open this screen" }
        header("Keys")
        info { "QuickPay: hold the key, right-click a player (Controls)" }
        info { "Balance HUD toggle: bind a key in Controls" }
    }
}
