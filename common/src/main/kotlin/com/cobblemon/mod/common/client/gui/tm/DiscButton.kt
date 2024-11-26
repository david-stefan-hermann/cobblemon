package com.cobblemon.mod.common.client.gui.tm

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.sounds.SoundManager
import net.minecraft.network.chat.Component

class DiscButton(
        pX: Int, pY: Int,
        onPress: OnPress
) : Button(pX, pY, WIDTH.toInt(), HEIGHT.toInt(), Component.literal("All Types"), onPress, DEFAULT_NARRATION) {

    companion object {
        private const val WIDTH = 53F
        private const val HEIGHT = 53F
        val DISC_BUTTON = cobblemonResource("textures/gui/tm/disc_button.png")
    }

    override fun renderWidget(graphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTicks: Float) {
        blitk(
                matrixStack = graphics.pose(),
                texture = DISC_BUTTON,
                x = x,
                y = y,
                width = WIDTH,
                height = HEIGHT,
                vOffset = if (isHovered(pMouseX.toDouble(), pMouseY.toDouble())) HEIGHT else 0,
                textureHeight = HEIGHT * 2
        )
    }

    override fun playDownSound(soundManager: SoundManager) {}

    private fun isHovered(mouseX: Double, mouseY: Double): Boolean {
        return mouseX.toFloat() in x.toFloat()..(x.toFloat() + WIDTH) &&
                mouseY.toFloat() in y.toFloat()..(y.toFloat() + HEIGHT)
    }
}
