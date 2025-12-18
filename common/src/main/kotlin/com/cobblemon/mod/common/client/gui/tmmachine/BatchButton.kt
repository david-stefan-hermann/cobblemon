package com.cobblemon.mod.common.client.gui.tmmachine

import com.cobblemon.mod.common.CobblemonNetwork
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.gui.CobblemonRenderable
import com.cobblemon.mod.common.net.messages.client.ui.SetTMMachineContainerDataPacket
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.sounds.SoundManager

class BatchButton(
    var buttonX: Float,
    var buttonY: Float,
    val clickAction: OnPress
) : Button(buttonX.toInt(), buttonY.toInt(), WIDTH, HEIGHT, "".text(), clickAction, DEFAULT_NARRATION), CobblemonRenderable {
    companion object {
        const val WIDTH = 22
        const val HEIGHT = 12

        // Reuse existing start button backgrounds
        val baseOn = cobblemonResource("textures/gui/tmmachine/button_start.png")
        val baseOff = cobblemonResource("textures/gui/tmmachine/button_start_disabled.png")

        // Reuse the repeat icon
        val iconRepeat = cobblemonResource("textures/gui/tmmachine/button_icon_repeat.png")
    }

    /** Whether batch mode is enabled on the block entity (repeatProcess). */
    var enabledState: Boolean = false

    override fun mouseDragged(d: Double, e: Double, i: Int, f: Double, g: Double) = false
    override fun defaultButtonNarrationText(builder: NarrationElementOutput) {}

    override fun renderWidget(context: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val bg = if (enabledState) baseOff else baseOn

        blitk(
            matrixStack = context.pose(),
            texture = bg,
            x = buttonX,
            y = buttonY,
            width = WIDTH,
            height = HEIGHT,
            vOffset = if (isMouseOver(mouseX.toDouble(), mouseY.toDouble())) HEIGHT else 0,
            textureHeight = HEIGHT * 2
        )

        // Dim the icon slightly when off
        blitk(
            matrixStack = context.pose(),
            texture = iconRepeat,
            x = buttonX,
            y = buttonY,
            width = WIDTH,
            height = HEIGHT,
            alpha = if (enabledState) 0.55F else 1F
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!active || !visible) return false
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun playDownSound(soundManager: SoundManager) {
        if (visible) {
            soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.GUI_CLICK, 1.0F))
        }
    }
}