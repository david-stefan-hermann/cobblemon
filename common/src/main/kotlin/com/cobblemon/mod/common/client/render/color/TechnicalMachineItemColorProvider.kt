package com.cobblemon.mod.common.client.render.color

import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.item.components.TMMoveComponent.Companion.getTMMove
import net.minecraft.client.color.item.ItemColor
import net.minecraft.util.FastColor
import net.minecraft.world.item.ItemStack

object TechnicalMachineItemColorProvider : ItemColor {
    override fun getColor(itemStack: ItemStack, layer: Int): Int {
        val moveType = getTMMove(itemStack)?.elementalType ?: ElementalTypes.NORMAL
        val color = if (layer == 0) moveType.primaryColor else moveType.secondaryColor
        return FastColor.ARGB32.opaque(color)
    }
}