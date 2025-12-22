/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block

import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.CobblemonItems
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.item.enchantment.ItemEnchantments
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams

class TypeGemBlock(
    settings: Properties
) : Block(settings) {

    companion object {
        val BLOCK_TO_GEM: Map<Block, Item> by lazy {
            mapOf(
                CobblemonBlocks.TYPE_GEM_BLOCK_NORMAL to CobblemonItems.NORMAL_GEM,
                CobblemonBlocks.TYPE_GEM_BLOCK_FIRE to CobblemonItems.FIRE_GEM,
                CobblemonBlocks.TYPE_GEM_BLOCK_WATER to CobblemonItems.WATER_GEM,
                CobblemonBlocks.TYPE_GEM_BLOCK_GRASS to CobblemonItems.GRASS_GEM,
                CobblemonBlocks.TYPE_GEM_BLOCK_ELECTRIC to CobblemonItems.ELECTRIC_GEM,
                CobblemonBlocks.TYPE_GEM_BLOCK_ICE to CobblemonItems.ICE_GEM,
                CobblemonBlocks.TYPE_GEM_BLOCK_FIGHTING to CobblemonItems.FIGHTING_GEM,
                CobblemonBlocks.TYPE_GEM_BLOCK_POISON to CobblemonItems.POISON_GEM,
                CobblemonBlocks.TYPE_GEM_BLOCK_GROUND to CobblemonItems.GROUND_GEM,
                CobblemonBlocks.TYPE_GEM_BLOCK_FLYING to CobblemonItems.FLYING_GEM,
                CobblemonBlocks.TYPE_GEM_BLOCK_PSYCHIC to CobblemonItems.PSYCHIC_GEM,
                CobblemonBlocks.TYPE_GEM_BLOCK_BUG to CobblemonItems.BUG_GEM,
                CobblemonBlocks.TYPE_GEM_BLOCK_ROCK to CobblemonItems.ROCK_GEM,
                CobblemonBlocks.TYPE_GEM_BLOCK_GHOST to CobblemonItems.GHOST_GEM,
                CobblemonBlocks.TYPE_GEM_BLOCK_DRAGON to CobblemonItems.DRAGON_GEM,
                CobblemonBlocks.TYPE_GEM_BLOCK_DARK to CobblemonItems.DARK_GEM,
                CobblemonBlocks.TYPE_GEM_BLOCK_STEEL to CobblemonItems.STEEL_GEM,
                CobblemonBlocks.TYPE_GEM_BLOCK_FAIRY to CobblemonItems.FAIRY_GEM
            )
        }
    }

    override fun getDrops(state: BlockState, params: LootParams.Builder): List<ItemStack> {
        // grab the gem related to the block
        val item = BLOCK_TO_GEM[state.block] ?: Items.AIR

        // try to figure out Fortune level
        val tool = params.getOptionalParameter(LootContextParams.TOOL)
        var fortuneLevel = 0

        if (tool != null && !tool.isEmpty) {
            val enchantments = tool.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
            for ((holder, level) in enchantments.entrySet()) {
                if (holder.`is`(Enchantments.FORTUNE)) {
                    fortuneLevel = level
                    break
                }
            }
        }

        var count = (1..2).random()

        if (fortuneLevel > 0) {
            count += (0..fortuneLevel).random()
        }

        return listOf(ItemStack(item, count))
    }
}
