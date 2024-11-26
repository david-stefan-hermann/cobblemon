package com.cobblemon.mod.common.client.gui.tm

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.client.gui.TypeIcon
import com.cobblemon.mod.common.client.gui.TypeReturnIcon
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.sounds.SoundManager
import net.minecraft.network.chat.Component

class TypeButton(
        pX: Int,
        pY: Int,
        onPress: OnPress,
        val type: ElementalType?
) : Button(
        pX,
        pY,
        WIDTH.toInt(),
        HEIGHT.toInt(),
        type?.displayName ?: Component.empty(),
        onPress,
        DEFAULT_NARRATION
) {

    companion object {
        private const val WIDTH = 24F
        private const val HEIGHT = 24F
        val TYPE_BUTTON = cobblemonResource("textures/gui/tm/type_button.png")
    }

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        blitk(
                matrixStack = graphics.pose(),
                texture = TYPE_BUTTON,
                x = x,
                y = y,
                width = WIDTH,
                height = HEIGHT,
                vOffset = if (isHovered(mouseX.toDouble(), mouseY.toDouble())) HEIGHT else 0,
                textureHeight = HEIGHT * 2
        )

        graphics.pose().pushPose()
        graphics.pose().scale(2f, 2f, 2f)

        if (type != null) {
            TypeIcon(
                    x = (x + 12.125) / 2,
                    y = (y + 3) / 2,
                    type = type,
                    centeredX = true,
                    small = true
            ).render(graphics)
        } else {
            TypeReturnIcon(
                    x = (x + 12.125) / 2,
                    y = (y + 3) / 2,
                    centeredX = true,
                    small = true
            ).render(graphics)
        }

        graphics.pose().popPose()
    }

    override fun playDownSound(soundManager: SoundManager) {
        // Override with no sound
    }

    fun isHovered(mouseX: Double, mouseY: Double): Boolean {
        return mouseX.toFloat() in (x.toFloat()..(x.toFloat() + WIDTH)) &&
                mouseY.toFloat() in (y.toFloat()..(y.toFloat() + HEIGHT))
    }
}
