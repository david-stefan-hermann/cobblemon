/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.gui.GuiGraphics

class TypeReturnIcon(
    val x: Number,
    val y: Number,
    val centeredX: Boolean = false,
    val small: Boolean = false,
    val secondaryOffset: Float = 15F,
    val doubleCenteredOffset: Float = 7.5F,
    val opacity: Float = 1F
) {
    companion object {
        private const val TYPE_ICON_DIAMETER = 36
        private const val SCALE = 0.5F

        private val typeReturnResource = cobblemonResource("textures/gui/type_return.png")
        private val smallTypeReturnResource = cobblemonResource("textures/gui/type_return_small.png")
    }

    fun render(context: GuiGraphics) {
        val diameter = if (small) (TYPE_ICON_DIAMETER / 2) else TYPE_ICON_DIAMETER

        blitk(
            matrixStack = context.pose(),
            texture = if (small) smallTypeReturnResource else typeReturnResource,
            x = (x.toFloat()) / SCALE - 9,
            y = y.toFloat() / SCALE,
            height = diameter,
            width = diameter,
            alpha = opacity,
            scale = SCALE
        )
    }
}