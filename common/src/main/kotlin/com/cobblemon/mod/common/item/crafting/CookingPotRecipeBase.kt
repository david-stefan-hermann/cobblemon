/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.item.crafting

import com.cobblemon.mod.common.CobblemonItems
import net.minecraft.core.HolderLookup
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.level.Level

interface CookingPotRecipeBase : Recipe<CraftingInput> {
    val result: ItemStack
    val groupName: String
    val category: CookingPotBookCategory
    val seasoningTag: TagKey<Item>
    val seasoningProcessors: List<SeasoningProcessor>

    // PT137: Recipe.getGroup()/getResultItem()/getIngredients()/getToastSymbol()/canCraftInDimensions() removed
    // New API: group():String / assemble(input):ItemStack 1-arg / showNotification():Boolean /
    //          placementInfo():PlacementInfo / recipeBookCategory():RecipeBookCategory
    override fun group(): String = groupName
    override fun matches(input: CraftingInput, level: Level): Boolean
    fun category() = category

    // Non-override convenience accessor (Recipe no longer requires getResultItem)
    fun resultItem(): ItemStack = this.result

    override fun assemble(input: CraftingInput): ItemStack {
        return result.copy()
    }

    override fun showNotification(): Boolean = true

    override fun placementInfo(): net.minecraft.world.item.crafting.PlacementInfo =
        net.minecraft.world.item.crafting.PlacementInfo.NOT_PLACEABLE

    override fun recipeBookCategory(): net.minecraft.world.item.crafting.RecipeBookCategory =
        net.minecraft.world.item.crafting.RecipeBookCategories.CRAFTING_MISC

    fun applySeasoning(stack: ItemStack, seasoning: List<ItemStack>) {
        for (processor in seasoningProcessors) {
            processor.apply(stack, seasoning)
        }
    }

    // PT137: getToastSymbol removed from Recipe — kept as non-override helper for legacy callers
    fun toastSymbol(): ItemStack = ItemStack(CobblemonItems.CAMPFIRE_POT_RED)
}
