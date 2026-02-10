package de.magicclutch.magicdonut.client.donation

import de.magicclutch.magicdonut.client.config.MagicDonutConfig
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
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
            val font = Minecraft.getInstance().font
            val textWidth = font.width("${config.customTitle}: ${config.noDonationText}") * config.size
            config.x = (width / 2 - textWidth / 2).toInt()
            MagicDonutConfig.HANDLER.save()
        }.pos(startX, 10).size(buttonWidth, buttonHeight).build())

        // Center Y
        addRenderableWidget(Button.builder(Component.literal("Center Y")) {
            config.y = height / 2 - 5
            MagicDonutConfig.HANDLER.save()
        }.pos(startX, 10 + buttonHeight + spacing).size(buttonWidth, buttonHeight).build())

        // Reset Button
        addRenderableWidget(Button.builder(Component.literal("Reset")) {
            config.x = 10
            config.y = 10
            config.size = 1.0f
            MagicDonutConfig.HANDLER.save()
        }.pos(startX, 10 + (buttonHeight + spacing) * 2).size(buttonWidth, buttonHeight).build())
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val font = Minecraft.getInstance().font
        val textWidth = font.width("${config.customTitle}: ${config.noDonationText}") * config.size
        val textHeight = 9 * config.size
        val padding = 8 * config.size


        if (mouseX >= config.x - padding && mouseX <= config.x + textWidth + padding &&
            mouseY >= config.y - padding && mouseY <= config.y + textHeight + padding) {

            val delta = (verticalAmount * 0.1).toFloat()
            config.size = (config.size + delta).coerceIn(0.5f, 4.0f)

            MagicDonutConfig.HANDLER.save()
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(context, mouseX, mouseY, delta)
        super.render(context, mouseX, mouseY, delta)
        renderEditorElements(context, mouseX, mouseY)
    }

    private fun renderEditorElements(context: GuiGraphics, mouseX: Int, mouseY: Int) {

        DonationRenderer.render(context)


        context.drawString(Minecraft.getInstance().font, "Mausrad: Größe ändern", 10, height - 20, 0xAAAAAA)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            val font = Minecraft.getInstance().font
            val scaledWidth = font.width("${config.customTitle}: ${config.noDonationText}") * config.size
            val padding = 8 * config.size


            if (mouseX >= config.x - padding && mouseX <= config.x + scaledWidth + padding &&
                mouseY >= config.y - padding && mouseY <= config.y + (9 * config.size) + padding) {
                isDragging = true
                dragOffsetX = mouseX - config.x
                dragOffsetY = mouseY - config.y
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        isDragging = false
        MagicDonutConfig.HANDLER.save()
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (isDragging) {
            config.x = (mouseX - dragOffsetX).toInt()
            config.y = (mouseY - dragOffsetY).toInt()
            return true
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun isPauseScreen(): Boolean = false
}