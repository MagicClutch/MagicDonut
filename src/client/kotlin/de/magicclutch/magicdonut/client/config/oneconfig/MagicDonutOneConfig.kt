package de.magicclutch.magicdonut.client.config.oneconfig

import de.magicclutch.magicdonut.client.auction.AuctionManager
import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.config.configuration.DevelopmentConfig
import de.magicclutch.magicdonut.client.config.configuration.OverheadStatsConfig
import de.magicclutch.magicdonut.client.donation.PositionEditScreen
import de.magicclutch.magicdonut.client.gamble.GambleScreen
import de.magicclutch.magicdonut.client.profit.ProfitTracker
import de.magicclutch.magicdonut.client.util.AmountParser
import net.minecraft.client.Minecraft
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.button
import org.polyfrost.oneconfig.api.config.v1.color
import org.polyfrost.oneconfig.api.config.v1.dropdown
import org.polyfrost.oneconfig.api.config.v1.slider
import org.polyfrost.oneconfig.api.config.v1.switch
import org.polyfrost.oneconfig.api.config.v1.text
import java.util.Collections
import java.util.WeakHashMap
import java.util.function.Predicate
import kotlin.math.roundToInt

/**
 * The OneConfig layout of MagicDonutUtils. It is only ever loaded when OneConfig is installed (it is created from
 * the `oneconfig:config` entrypoint), so the mod keeps working with just YACL / Mod Menu.
 *
 * [MagicDonutConfig] stays the single source of truth: this screen reads from it when it opens and writes every
 * change straight back, so YACL, Mod Menu and OneConfig always show the same values. Values OneConfig stored on its own
 * are overwritten with ours on startup.
 */
class MagicDonutOneConfig : Config(
    "magicdonututils",
    "assets/magicdonut/icon.png",
    "MagicDonutUtils",
    Config.Category.QOL
) {
    private class Binding(val id: String, val read: () -> Any, val write: (Any) -> Unit)

    private val bindings = mutableListOf<Binding>()
    private val boundProperties: MutableSet<Property<*>> = Collections.newSetFromMap(WeakHashMap())

    override fun makeTree(): Tree {
        bindings.clear()
        val tree = Tree.tree(id)
        val d = MagicDonutConfig() // a fresh instance holds the real defaults for "reset to default"
        val c = MagicDonutConfig.instance

        payConfirm(tree, d, c)
        donation(tree, d, c)
        overheadStats(tree, d, c)
        auction(tree, d, c)
        gamble(tree, d, c)
        dailyBalance(tree, d, c)
        developer(tree, d, c)
        return tree
    }

    override fun onInitialized(tree: Tree) {
        // OneConfig has just loaded its own stored values. Ours win, and every change is written back to ours.
        for (binding in bindings) {
            @Suppress("UNCHECKED_CAST")
            val property = tree.getProp(binding.id) as? Property<Any> ?: continue
            property.setAsSilently(binding.read())
            if (boundProperties.add(property)) {
                property.addCallback(Predicate<Any> { value ->
                    binding.write(value)
                    MagicDonutConfig.HANDLER.save()
                    false // false = do not cancel the change
                })
            }
        }
    }

    // ---- sections -------------------------------------------------------------------------------------------

    private fun payConfirm(t: Tree, d: MagicDonutConfig, c: MagicDonutConfig) {
        val cat = "Pay Confirm"
        val dc = d.payConfirmConfig
        val cc = c.payConfirmConfig
        toggle(t, "payConfirmEnabled", "Enabled", "Ask for confirmation before large /pay commands.", cat, "General", dc.enabled, { cc.enabled }, { cc.enabled = it })
        amount(t, "payConfirmThreshold", "Confirm from amount", "Payments of at least this amount must be confirmed. Accepts 5000000, 5m, 2.5k.", cat, "General", dc.confirmThreshold, { cc.confirmThreshold }, { cc.confirmThreshold = it })
        intSlider(t, "payConfirmTimeout", "Confirmation timeout", "How long a pending payment stays confirmable (seconds).", cat, "General", dc.timeoutSeconds, 10, 300, 5, "s", { cc.timeoutSeconds }, { cc.timeoutSeconds = it })
    }

    private fun donation(t: Tree, d: MagicDonutConfig, c: MagicDonutConfig) {
        val cat = "Donation HUD"
        val dc = d.donationConfig
        val cc = c.donationConfig
        toggle(t, "donationEnabled", "Enabled", "Show your highest received donation on screen.", cat, "General", dc.enabled, { cc.enabled }, { cc.enabled = it })
        button(t, "donationEditor", "Move on screen", "Drag the HUD with the mouse, scroll to resize.", "Open editor", cat, "Layout") {
            val client = Minecraft.getInstance()
            client.execute { client.gui.setScreen(PositionEditScreen()) }
        }
        floatSlider(t, "donationX", "Horizontal position", "0 = left edge, 1 = right edge.", cat, "Layout", dc.x, 0f, 1f, 0.01f, null, { cc.x }, { cc.x = it })
        floatSlider(t, "donationY", "Vertical position", "0 = top edge, 1 = bottom edge.", cat, "Layout", dc.y, 0f, 1f, 0.01f, null, { cc.y }, { cc.y = it })
        floatSlider(t, "donationSize", "Size", "Scale of the HUD.", cat, "Layout", dc.size, 0.5f, 3f, 0.1f, "x", { cc.size }, { cc.size = it })
        string(t, "donationTitle", "Prefix", "Text shown before the donation.", cat, "Appearance", dc.customTitle, { cc.customTitle }, { cc.customTitle = it })
        string(t, "donationEmpty", "No-donation text", "Shown until the first donation arrives.", cat, "Appearance", dc.noDonationText, { cc.noDonationText }, { cc.noDonationText = it })
        colorOption(t, "donationColor", "Text color", "", cat, "Appearance", dc.color, true, { cc.color }, { cc.color = it })
        toggle(t, "donationBox", "Background box", "Draw a box behind the text.", cat, "Appearance", dc.showBox, { cc.showBox }, { cc.showBox = it })
        colorOption(t, "donationBoxColor", "Box color", "", cat, "Appearance", dc.boxColor, true, { cc.boxColor }, { cc.boxColor = it })
        button(t, "donationReset", "Reset highscore", "Forget the highest donation.", "Reset", cat, "Data") {
            MagicDonutConfig.instance.donationConfig.reset()
        }
    }

    private fun overheadStats(t: Tree, d: MagicDonutConfig, c: MagicDonutConfig) {
        val cat = "Overhead Stats"
        val dc = d.overheadStatsConfig
        val cc = c.overheadStatsConfig
        toggle(t, "statsEnabled", "Enabled", "Show stats above player name tags. Needs an API key or a stats proxy.", cat, "General", dc.enabled, { cc.enabled }, { cc.enabled = it })
        enumDropdown(
            t, "statsDisplayMode", "Display mode", "Show one fixed stat, or cycle through all of them.", cat, "Display",
            dc.displayMode, mapOf(OverheadStatsConfig.DisplayMode.FIXED to "Fixed", OverheadStatsConfig.DisplayMode.CYCLE to "Cycle"),
            { cc.displayMode }, { cc.displayMode = it }
        )
        enumDropdown(
            t, "statsStat", "Stat", "The stat shown in fixed mode.", cat, "Display",
            dc.stat, OverheadStatsConfig.StatField.entries.associateWith { it.displayName },
            { cc.stat }, { cc.stat = it }
        )
        intSlider(t, "statsCycle", "Cycle interval", "Seconds per stat in cycle mode.", cat, "Display", dc.cycleSeconds, 1, 30, 1, "s", { cc.cycleSeconds }, { cc.cycleSeconds = it })
        floatSlider(t, "statsOffset", "Height", "Distance above the name tag.", cat, "Layout", dc.statYOffset, -2f, 4f, 0.05f, null, { cc.statYOffset }, { cc.statYOffset = it })
        floatSlider(t, "statsScale", "Scale", "Text size relative to the vanilla name tag.", cat, "Layout", dc.statScale, 0.2f, 2f, 0.05f, "x", { cc.statScale }, { cc.statScale = it })
        intSlider(t, "statsRate", "Requests per minute", "Global cap for API requests. Lower it if you hit rate limits.", cat, "Performance", dc.maxRequestsPerMinute, 1, 250, 1, null, { cc.maxRequestsPerMinute }, { cc.maxRequestsPerMinute = it })
    }

    private fun auction(t: Tree, d: MagicDonutConfig, c: MagicDonutConfig) {
        val cat = "Auction"
        val dc = d.auctionConfig
        val cc = c.auctionConfig
        toggle(t, "auctionEnabled", "Show hologram panel", "Hold an item and run /auction start. The highest payment wins.", cat, "General", dc.enabled, { cc.enabled }, { cc.enabled = it })
        intSlider(t, "auctionDuration", "Default duration", "Used when /auction start has no duration.", cat, "General", dc.defaultDurationSeconds, 10, 900, 10, "s", { cc.defaultDurationSeconds }, { cc.defaultDurationSeconds = it })
        amount(t, "auctionMinBid", "Default minimum bid", "Payments below this are ignored. Accepts 1000, 1k, 2.5m.", cat, "General", dc.defaultMinimumBid, { cc.defaultMinimumBid }, { cc.defaultMinimumBid = it })
        toggle(t, "auctionSound", "Sound on new top bid", "", cat, "General", dc.playBidSound, { cc.playBidSound }, { cc.playBidSound = it })
        floatSlider(t, "auctionX", "Horizontal position", "", cat, "Hologram", dc.x, 0f, 1f, 0.01f, null, { cc.x }, { cc.x = it })
        floatSlider(t, "auctionY", "Vertical position", "", cat, "Hologram", dc.y, 0f, 1f, 0.01f, null, { cc.y }, { cc.y = it })
        floatSlider(t, "auctionScale", "Scale", "", cat, "Hologram", dc.scale, 0.5f, 3f, 0.1f, "x", { cc.scale }, { cc.scale = it })
        toggle(t, "auctionBob", "Floating animation", "The item gently bobs up and down.", cat, "Hologram", dc.bobbingItem, { cc.bobbingItem }, { cc.bobbingItem = it })
        button(t, "auctionCancel", "Cancel running auction", "Stops the current auction without a winner.", "Cancel", cat, "Control") {
            AuctionManager.cancel()
        }
    }

    private fun gamble(t: Tree, d: MagicDonutConfig, c: MagicDonutConfig) {
        val cat = "Gamble"
        val dc = d.gambleConfig
        val cc = c.gambleConfig
        button(t, "gambleOpen", "Offline casino", "Coinflip, dice and slots with virtual money. Also available as /gamble.", "Open", cat, "Simulator") {
            val client = Minecraft.getInstance()
            client.execute { client.gui.setScreen(GambleScreen(client.gui.screen())) }
        }
        amount(t, "gambleStart", "Starting balance", "Balance after Reset balance. Accepts 10000, 10k.", cat, "Simulator", dc.startingBalance, { cc.startingBalance }, { cc.startingBalance = it })
        toggle(t, "gambleSound", "Sounds", "", cat, "Simulator", dc.soundEnabled, { cc.soundEnabled }, { cc.soundEnabled = it })
        button(t, "gambleReset", "Reset balance and stats", "Back to the starting balance.", "Reset", cat, "Data") {
            MagicDonutConfig.instance.gambleConfig.resetStats()
            MagicDonutConfig.HANDLER.save()
        }
    }

    private fun dailyBalance(t: Tree, d: MagicDonutConfig, c: MagicDonutConfig) {
        val cat = "Daily Balance"
        val dc = d.profitConfig
        val cc = c.profitConfig
        toggle(t, "profitEnabled", "Show HUD", "Balance and today's profit above the hotbar. Needs an API key or a stats proxy.", cat, "General", dc.enabled, { cc.enabled }, { cc.enabled = it })
        toggle(t, "profitBalance", "Show balance", "", cat, "General", dc.showBalance, { cc.showBalance }, { cc.showBalance = it })
        toggle(t, "profitToday", "Show today's profit", "", cat, "General", dc.showProfit, { cc.showProfit }, { cc.showProfit = it })
        intSlider(t, "profitRefresh", "Refresh interval", "Each refresh is one stats request for your account.", cat, "General", dc.refreshSeconds, 10, 300, 5, "s", { cc.refreshSeconds }, { cc.refreshSeconds = it })
        intSlider(t, "profitOffset", "Height above screen bottom", "", cat, "Layout", dc.hudBottomOffset, 20, 200, 2, null, { cc.hudBottomOffset }, { cc.hudBottomOffset = it })
        button(t, "profitStart", "Set today's starting point", "Uses your current balance. Also /dailybalance start.", "Set", cat, "Control") {
            ProfitTracker.startToday()
        }
        button(t, "profitReset", "Clear starting point", "The next refresh becomes the new starting point.", "Clear", cat, "Control") {
            ProfitTracker.resetBaseline()
        }
    }

    private fun developer(t: Tree, d: MagicDonutConfig, c: MagicDonutConfig) {
        val cat = "Developer"
        val dc = d.developmentConfig
        val cc = c.developmentConfig
        string(t, "devApiKey", "API key", "Your personal DonutSMP API key. Never share it. Leave empty when using a proxy.", cat, "API", dc.apiKey, { cc.apiKey }, { cc.apiKey = it.trim() })
        string(
            t, "devApiBase", "API base URL", "Only change this to point at a proxy you trust (default: ${DevelopmentConfig.DEFAULT_API_BASE_URL}).",
            cat, "API", dc.apiBaseUrl, { cc.apiBaseUrl }, { cc.apiBaseUrl = it.trim() }
        )
    }

    // ---- builders: create the OneConfig option and remember how to read/write our own value --------------------

    private fun bind(id: String, read: () -> Any, write: (Any) -> Unit) {
        bindings += Binding(id, read, write)
    }

    private fun toggle(t: Tree, id: String, title: String, desc: String, cat: String, sub: String, default: Boolean, get: () -> Boolean, set: (Boolean) -> Unit) {
        t.switch(id, title, default, desc, cat, sub)
        bind(id, get) { set(it as Boolean) }
    }

    private fun string(t: Tree, id: String, title: String, desc: String, cat: String, sub: String, default: String, get: () -> String, set: (String) -> Unit) {
        t.text(id, title, default, description = desc, category = cat, subcategory = sub)
        bind(id, get) { set(it as String) }
    }

    /** A money amount stored as a Long, edited as text so `5m` and `2.5k` work. Invalid input is ignored. */
    private fun amount(t: Tree, id: String, title: String, desc: String, cat: String, sub: String, default: Long, get: () -> Long, set: (Long) -> Unit) {
        t.text(id, title, default.toString(), description = desc, category = cat, subcategory = sub, regex = "[0-9.]*[kKmMbB]?")
        bind(id, { get().toString() }) { value -> AmountParser.parse(value as String)?.let(set) }
    }

    private fun floatSlider(t: Tree, id: String, title: String, desc: String, cat: String, sub: String, default: Float, min: Float, max: Float, step: Float, unit: String?, get: () -> Float, set: (Float) -> Unit) {
        t.slider(id, title, default, min, max, step, unit, desc, cat, sub)
        bind(id, get) { set((it as Number).toFloat()) }
    }

    private fun intSlider(t: Tree, id: String, title: String, desc: String, cat: String, sub: String, default: Int, min: Int, max: Int, step: Int, unit: String?, get: () -> Int, set: (Int) -> Unit) {
        t.slider(id, title, default.toFloat(), min.toFloat(), max.toFloat(), step.toFloat(), unit, desc, cat, sub)
        bind(id, { get().toFloat() }) { set((it as Number).toFloat().roundToInt()) }
    }

    private fun colorOption(t: Tree, id: String, title: String, desc: String, cat: String, sub: String, defaultArgb: Int, alpha: Boolean, get: () -> Int, set: (Int) -> Unit) {
        t.color(id, title, PolyColor(defaultArgb), alpha, false, desc, cat, sub)
        bind(id, { PolyColor(get()) }) { set((it as PolyColor).argb) }
    }

    private fun <E : Enum<E>> enumDropdown(t: Tree, id: String, title: String, desc: String, cat: String, sub: String, default: E, labels: Map<E, String>, get: () -> E, set: (E) -> Unit) {
        t.dropdown(id, title, default, labels, desc, cat, sub)
        @Suppress("UNCHECKED_CAST")
        bind(id, get) { set(it as E) }
    }

    private fun button(t: Tree, id: String, title: String, desc: String, label: String, cat: String, sub: String, action: () -> Unit) {
        t.button(id, title, label, desc, cat, sub, action)
    }
}
