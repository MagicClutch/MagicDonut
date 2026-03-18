package de.magicclutch.magicdonut.client.donation

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class PositionEditScreen : Screen(Component.literal("Donation Editor")) {
    private val config = MagicDonutConfig.instance.donationConfig

    private var isDragging = false
    private var dragOffsetX = 0.0
    private var dragOffsetY = 0.0


    override fun init() {
        val buttonWidth = 60
        val buttonHeight = 20
        val spacing = 5
        val startX = width - buttonWidth - 10

        // Center X
        addRenderableWidget(Button.builder(Component.literal("Center X")) {
            config.x = 0.5f
            MagicDonutConfig.HANDLER.save()
        }.pos(startX, 10).size(buttonWidth, buttonHeight).build())

        // Center Y
        addRenderableWidget(Button.builder(Component.literal("Center Y")) {
            config.y = 0.5f
            MagicDonutConfig.HANDLER.save()
        }.pos(startX, 10 + buttonHeight + spacing).size(buttonWidth, buttonHeight).build())

        // Reset Button
        addRenderableWidget(Button.builder(Component.literal("Reset")) {
            config.x = 0.5f
            config.y = 0.05f
            config.size = 1.0f
            MagicDonutConfig.HANDLER.save()
        }.pos(startX, 10 + (buttonHeight + spacing) * 2).size(buttonWidth, buttonHeight).build())
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val font = Minecraft.getInstance().font
        val textWidth = font.width("${config.customTitle}: ${config.noDonationText}").toFloat()
        val paddingX = 6f
        val paddingY = 2f
        val scaledWidth = (textWidth + paddingX * 2) * config.size
        val scaledHeight = (9 + paddingY * 2) * config.size

        val centerX = width * config.x.coerceIn(0f, 1f)
        val centerY = height * config.y.coerceIn(0f, 1f)
        val left = centerX - scaledWidth / 2f - paddingX * config.size
        val right = centerX + scaledWidth / 2f - paddingX * config.size
        val top = centerY - paddingY * config.size
        val bottom = centerY + scaledHeight - paddingY * config.size

        if (mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom) {

            val delta = (verticalAmount * 0.1).toFloat()
            config.size = (config.size + delta).coerceIn(0.5f, 4.0f)

            MagicDonutConfig.HANDLER.save()
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // super.render handles background; calling it twice in 1.21.11 causes double blur and a crash
        super.render(context, mouseX, mouseY, delta)
        renderEditorElements(context, mouseX, mouseY)
    }

    private fun renderEditorElements(context: GuiGraphics, mouseX: Int, mouseY: Int) {

        DonationRenderer.render(context)


        context.drawString(Minecraft.getInstance().font, "Scolle Weel: Change size", 10, height - 20, 0xAAAAAA)
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        if (event.button() == 0) {
        val font = Minecraft.getInstance().font
        val textWidth = font.width("${config.customTitle}: ${config.noDonationText}").toFloat()
        val paddingX = 6f
        val paddingY = 2f
        val scaledWidth = (textWidth + paddingX * 2) * config.size
        val scaledHeight = (9 + paddingY * 2) * config.size

        val mouseX = event.x
        val mouseY = event.y

        val centerX = width * config.x.coerceIn(0f, 1f)
        val centerY = height * config.y.coerceIn(0f, 1f)
        val left = centerX - scaledWidth / 2f - paddingX * config.size
        val right = centerX + scaledWidth / 2f - paddingX * config.size
        val top = centerY - paddingY * config.size
        val bottom = centerY + scaledHeight - paddingY * config.size

        if (mouseX >= left && mouseX <= right &&
            mouseY >= top && mouseY <= bottom) {
            isDragging = true
            dragOffsetX = mouseX - centerX
            dragOffsetY = mouseY - centerY
            return true
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
        if (isDragging) {
            val mouseX = event.x
            val mouseY = event.y
            config.x = ((mouseX - dragOffsetX) / width).toFloat().coerceIn(0f, 1f)
            config.y = ((mouseY - dragOffsetY) / height).toFloat().coerceIn(0f, 1f)
            return true
        }
        return super.mouseDragged(event, deltaX, deltaY)
    }

    override fun isPauseScreen(): Boolean = false

    override fun onClose() {
        isDragging = false
        MagicDonutConfig.HANDLER.save()
        super.onClose()
    }
}
