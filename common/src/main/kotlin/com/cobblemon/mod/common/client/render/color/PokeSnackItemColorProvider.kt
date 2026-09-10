/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.color

import com.cobblemon.mod.common.CobblemonItemComponents
import com.cobblemon.mod.common.api.cooking.getColourMixFromColors
import net.minecraft.client.color.item.ItemTintSource
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

object PokeSnackItemColorProvider : ItemTintSource {
    override fun calculate(stack: ItemStack, level: ClientLevel?, entity: LivingEntity?): Int {
        val colorComponent = stack.get(CobblemonItemComponents.FOOD_COLOUR) ?: return -1
        val colorMix = getColourMixFromColors(colorComponent.getColoursAsARGB())
        if (colorMix != null) return colorMix
        return -1
    }

    // PT144: ItemTintSource.type() abstract member added in MC 26.1.x.
    override fun type(): com.mojang.serialization.MapCodec<out ItemTintSource> =
        com.mojang.serialization.MapCodec.unit(this)
}
