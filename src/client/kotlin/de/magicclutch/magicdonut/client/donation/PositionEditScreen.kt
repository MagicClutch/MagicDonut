package de.magicclutch.magicdonut.client.donation

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import kotlin.math.ceil
import kotlin.math.floor

class PositionEditScreen : Screen(Component.literal("Donation Editor")) {
    private val config = MagicDonutConfig.instance.donationConfig

    private var isDragging = false
    private var dragOffsetX = 0.0
    private var dragOffsetY = 0.0

    private data class RectD(
        val left: Double,
        val top: Double,
        val right: Double,
        val bottom: Double
    ) {
        fun contains(x: Double, y: Double): Boolean =
            x >= left && x <= right && y >= top && y <= bottom
    }

    private data class LocalRect(
        val x: Double,
        val y: Double,
        val w: Double,
        val h: Double
    )

    override fun init() {
        val buttonWidth = 60
        val buttonHeight = 20
        val spacing = 5
        val startX = (width - buttonWidth - 10).coerceAtLeast(0)

        addRenderableWidget(
            Button.builder(Component.literal("Center X")) {
                config.x = 0.5f
                MagicDonutConfig.HANDLER.save()
            }.pos(startX, 10).size(buttonWidth, buttonHeight).build()
        )

        addRenderableWidget(
            Button.builder(Component.literal("Center Y")) {
                config.y = 0.5f
                MagicDonutConfig.HANDLER.save()
            }.pos(startX, 10 + buttonHeight + spacing).size(buttonWidth, buttonHeight).build()
        )

        addRenderableWidget(
            Button.builder(Component.literal("Reset")) {
                config.x = 0.5f
                config.y = 0.05f
                config.size = 1.0f
                MagicDonutConfig.HANDLER.save()
            }.pos(startX, 10 + (buttonHeight + spacing) * 2).size(buttonWidth, buttonHeight).build()
        )
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractRenderState(context, mouseX, mouseY, delta)

        // Render the overlay preview even while editing (DonationRenderer already checks enabled + world).
        val mc = Minecraft.getInstance()
        val screen = mc.gui.screen()
        if (screen == null || screen is ChatScreen || screen is InventoryScreen || screen is PositionEditScreen) {
            DonationRenderer.render(context)
        }

        val hintY = (height - 20).coerceAtLeast(0)
        context.text(mc.font, "Scroll Wheel: Change size", 10, hintY, 0xAAAAAA)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val rect = computeDragRect()?.second
        if (rect != null && rect.contains(mouseX, mouseY)) {
            val delta = (verticalAmount * 0.1).toFloat()
            config.size = (config.size + delta).coerceIn(0.5f, 4.0f)
            MagicDonutConfig.HANDLER.save()
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        if (event.button() == 0) {
            val res = computeDragRect()
            if (res != null) {
                val (_, rect) = res
                if (rect.contains(event.x, event.y)) {
                    val center = currentCenter()
                    isDragging = true
                    dragOffsetX = event.x - center.first
                    dragOffsetY = event.y - center.second
                    return true
                }
            }
        }
        return super.mouseClicked(event, isDoubleClick)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        isDragging = false
        MagicDonutConfig.HANDLER.save()
        return super.mouseReleased(event)
    }

    override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        if (!isDragging) return super.mouseDragged(event, deltaX, deltaY)
        if (width <= 0 || height <= 0) return true

        val res = computeDragRect() ?: return true
        val (local, _) = res

        val scale = (config.size.takeIf { it.isFinite() } ?: 1.0f).coerceIn(0.5f, 4.0f).toDouble()

        var centerX = event.x - dragOffsetX
        var centerY = event.y - dragOffsetY

        // Clamp by the actual overlay bounds so you can drag to the edges but not beyond
        // (i.e., the "movable area" matches the overlay's current size).
        val minCenterX = -local.x * scale
        val maxCenterX = width.toDouble() - (local.x + local.w) * scale
        val minCenterY = -local.y * scale
        val maxCenterY = height.toDouble() - (local.y + local.h) * scale

        if (minCenterX <= maxCenterX) centerX = centerX.coerceIn(minCenterX, maxCenterX)
        if (minCenterY <= maxCenterY) centerY = centerY.coerceIn(minCenterY, maxCenterY)

        config.x = (centerX / width.toDouble()).toFloat().coerceIn(0f, 1f)
        config.y = (centerY / height.toDouble()).toFloat().coerceIn(0f, 1f)
        return true
    }

    override fun isPauseScreen(): Boolean = false

    override fun onClose() {
        isDragging = false
        MagicDonutConfig.HANDLER.save()
        super.onClose()
    }

    private fun currentCenter(): Pair<Double, Double> {
        val xNorm = config.x.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f
        val yNorm = config.y.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.05f
        return (width.toDouble() * xNorm) to (height.toDouble() * yNorm)
    }

    private fun computeText(): String {
        val donation = config.getHighestDonation()
        val contentText = if (donation != null) {
            "${donation.playerName} : ${formatMoney(donation.amount)}"
        } else {
            config.noDonationText
        }
        return "${config.customTitle}: $contentText"
    }

    private fun formatMoney(value: Float): String {
        return when {
            value >= 1_000_000_000 -> String.format("%.2fB$", value / 1_000_000_000.0)
            value >= 1_000_000 -> String.format("%.2fM$", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.2fK$", value / 1_000.0)
            else -> String.format("%.2f$", value)
        }
    }

    private fun computeLocalBounds(textWidth: Double): LocalRect {
        val paddingX = 6.0
        val paddingY = 2.0
        val textH = 9.0

        return if (config.showBox) {
            val x = floor(-(textWidth / 2.0) - paddingX)
            val y = floor(-paddingY)
            val w = ceil(textWidth + paddingX * 2.0)
            val h = ceil(textH + paddingY * 2.0)
            LocalRect(x, y, w, h)
        } else {
            LocalRect(-(textWidth / 2.0), 0.0, textWidth, textH)
        }
    }

    private fun localToScreenRect(centerX: Double, centerY: Double, scale: Double, r: LocalRect): RectD {
        val left = centerX + r.x * scale
        val top = centerY + r.y * scale
        val right = centerX + (r.x + r.w) * scale
        val bottom = centerY + (r.y + r.h) * scale
        return RectD(left, top, right, bottom)
    }

    private fun computeDragRect(): Pair<LocalRect, RectD>? {
        if (width <= 0 || height <= 0) return null

        val mc = Minecraft.getInstance()
        val font = mc.font
        val text = computeText()
        val textWidth = font.width(text).toDouble()

        val center = currentCenter()
        val scale = (config.size.takeIf { it.isFinite() } ?: 1.0f).coerceIn(0.5f, 4.0f).toDouble()
        val local = computeLocalBounds(textWidth)
        val screen = localToScreenRect(center.first, center.second, scale, local)
        return local to screen
    }
}
