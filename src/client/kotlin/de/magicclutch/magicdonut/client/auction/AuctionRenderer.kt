package de.magicclutch.magicdonut.client.auction

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import de.magicclutch.magicdonut.client.util.AmountParser
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier
import kotlin.math.roundToInt
import kotlin.math.sin

/** Draws the "hologram" panel of the running auction: the held item floating above the current bid. */
object AuctionRenderer {
    private const val PANEL_WIDTH = 150
    private const val ITEM_SCALE = 3f
    private const val ITEM_BOX = (16 * ITEM_SCALE).toInt()

    private const val COLOR_PANEL = 0x99101018.toInt()
    private const val COLOR_BORDER = 0xFFFFAA00.toInt()
    private const val COLOR_TEXT = 0xFFFFFFFF.toInt()
    private const val COLOR_MUTED = 0xFFAAAAAA.toInt()
    private const val COLOR_GOOD = 0xFF55FF55.toInt()

    fun register() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("magicdonut", "auction_hud")
        ) { context, _ -> render(context) }
    }

    fun render(context: GuiGraphicsExtractor) {
        val config = MagicDonutConfig.instance.auctionConfig
        if (!config.enabled) return

        val mc = Minecraft.getInstance()
        if (mc.level == null) return

        val running = AuctionManager.current
        val auction = running ?: AuctionManager.visibleFinished() ?: return
        val sold = running == null

        val screenW = mc.window.guiScaledWidth
        val screenH = mc.window.guiScaledHeight
        if (screenW <= 0 || screenH <= 0) return

        val font = mc.font
        val top = auction.highest
        val lines = buildList {
            add(auction.item.hoverName.string.let { "${auction.item.count}x $it" } to COLOR_TEXT)
            if (top != null) {
                val label = if (sold) "SOLD: " else "Top bid: "
                add("$label$${AmountParser.formatShort(top.amount)}" to COLOR_GOOD)
                add("by ${top.player}" to COLOR_MUTED)
            } else {
                add("Min bid: $${AmountParser.formatShort(auction.minimumBid)}" to COLOR_MUTED)
            }
            if (!sold) {
                val left = auction.secondsLeft()
                add("%d:%02d left".format(left / 60, left % 60) to COLOR_MUTED)
            } else if (top == null) {
                add("No bids" to COLOR_MUTED)
            }
        }

        val lineHeight = 10
        val panelH = 8 + ITEM_BOX + 6 + lines.size * lineHeight + 6
        val scale = config.scale.coerceIn(0.5f, 3f)

        val pose = context.pose()
        pose.pushMatrix()
        try {
            pose.translate(screenW * config.x.coerceIn(0f, 1f), screenH * config.y.coerceIn(0f, 1f))
            pose.scale(scale, scale)

            val left = -PANEL_WIDTH / 2
            context.fill(left, 0, left + PANEL_WIDTH, panelH, COLOR_PANEL)
            context.fill(left, 0, left + PANEL_WIDTH, 1, COLOR_BORDER)
            context.fill(left, panelH - 1, left + PANEL_WIDTH, panelH, COLOR_BORDER)

            // Floating item: gentle bobbing so it reads as a hologram.
            val bob = if (config.bobbingItem && !sold) (sin(System.currentTimeMillis() / 350.0) * 2.0).roundToInt() else 0
            pose.pushMatrix()
            pose.translate(-ITEM_BOX / 2f, 8f + bob)
            pose.scale(ITEM_SCALE, ITEM_SCALE)
            context.item(auction.item, 0, 0)
            pose.popMatrix()

            var y = 8 + ITEM_BOX + 6
            for ((text, color) in lines) {
                context.centeredText(font, text, 0, y, color)
                y += lineHeight
            }
        } finally {
            pose.popMatrix()
        }
    }
}
