/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.tm

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.sounds.SoundManager
import net.minecraft.network.chat.Component

class EjectButton(
        pX: Int, pY: Int,
        private val small: Boolean = false,
        onPress: OnPress
) : Button(
        pX,
        pY,
        if (small) SMALL_WIDTH.toInt() else LARGE_WIDTH.toInt(),
        if (small) SMALL_HEIGHT.toInt() else LARGE_HEIGHT.toInt(),
        Component.literal("Eject"),
        onPress,
        DEFAULT_NARRATION
) {

    companion object {
        private const val LARGE_WIDTH = 54F
        private const val LARGE_HEIGHT = 14F
        private const val SMALL_WIDTH = 14F
        private const val SMALL_HEIGHT = 14F
        val SMALL_BUTTON = cobblemonResource("textures/gui/tm/eject_button_small.png")
        val LARGE_BUTTON = cobblemonResource("textures/gui/tm/eject_button_large.png")
    }

    private val height = if (small) SMALL_HEIGHT else LARGE_HEIGHT
    private val width = if (small) SMALL_WIDTH else LARGE_WIDTH
    private val texture = if (small) SMALL_BUTTON else LARGE_BUTTON

    override fun renderWidget(graphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTicks: Float) {
        blitk(
                matrixStack = graphics.pose(),
                texture = texture,
                x = x,
                y = y,
                width = width,
                height = height,
                vOffset = if (isHovered(pMouseX.toDouble(), pMouseY.toDouble())) height else 0,
                textureHeight = height * 2
        )
    }

    override fun playDownSound(soundManager: SoundManager) {}

    private fun isHovered(mouseX: Double, mouseY: Double): Boolean {
        return mouseX.toFloat() in x.toFloat()..(x.toFloat() + width) &&
                mouseY.toFloat() in y.toFloat()..(y.toFloat() + height)
    }
}
