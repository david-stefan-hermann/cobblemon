package com.cobblemon.mod.common.client.gui.tm

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.tms.TechnicalMachine
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.renderScaledGuiItemIcon
import com.cobblemon.mod.common.item.components.TMMoveComponent
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.sounds.SoundManager
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

class TMListingButton(
        pX: Int, pY: Int,
        onPress: OnPress,
        val tm: TechnicalMachine
) : Button(
        pX,
        pY,
        WIDTH.toInt(),
        HEIGHT.toInt(),
        Component.literal("All Types"),
        onPress,
        DEFAULT_NARRATION
) {

    companion object {
        private const val HEIGHT = 20F
        private const val WIDTH = 140F
        val TM_LISTING_BUTTON = cobblemonResource("textures/gui/tm/tm_selection_listing.png")
    }

    val stack = TMMoveComponent.createStack(tm.moveName)

    override fun renderWidget(graphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTicks: Float) {
        blitk(
                matrixStack = graphics.pose(),
                texture = TM_LISTING_BUTTON,
                x = x,
                y = y,
                width = WIDTH,
                height = HEIGHT,
                vOffset = if (isHovered(pMouseX.toDouble(), pMouseY.toDouble())) HEIGHT else 0,
                textureHeight = HEIGHT * 2
        )

        renderScaledGuiItemIcon(
                itemStack = stack,
                x = x.toDouble() + 2,
                y = y.toDouble() + 2
        )

        drawScaledText(
            context = graphics,
            text = Component.literal(tm.translatedMoveName().getString()), // Convert to MutableComponent
            x = x + 25,
            y = y + 7.5,
            scale = 0.75f
        )
    }

    override fun playDownSound(soundManager: SoundManager) {}

    private fun isHovered(mouseX: Double, mouseY: Double): Boolean {
        return mouseX.toFloat() in x.toFloat()..(x.toFloat() + WIDTH) &&
                mouseY.toFloat() in y.toFloat()..(y.toFloat() + HEIGHT)
    }
}
