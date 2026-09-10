/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.integration.jei.brewing

import com.cobblemon.mod.common.item.crafting.brewingstand.BrewingStandRecipe
import mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe
import net.minecraft.world.item.ItemStack

class JeiBrewingStandRecipe(
    private val recipe: BrewingStandRecipe,
    private val id: net.minecraft.resources.Identifier
) : IJeiBrewingRecipe {

    override fun getPotionInputs(): List<ItemStack> {
        // PT137: Ingredient.items() now returns Stream<Holder<Item>>
        return recipe.bottle.items().map { it.value().defaultInstance }.toList()
    }

    override fun getIngredients(): List<ItemStack> {
        return recipe.input.items().map { it.value().defaultInstance }.toList()
    }

    override fun getPotionOutput(): ItemStack {
        return recipe.result.create()
    }

    override fun getBrewingSteps(): Int {
        return 1
    }

    // port/26.2: getUid is non-null on IJeiBrewingRecipe, so the id is typed as an Identifier rather
    // than being loosely typed and cast.
    override fun getUid(): net.minecraft.resources.Identifier = id
}
