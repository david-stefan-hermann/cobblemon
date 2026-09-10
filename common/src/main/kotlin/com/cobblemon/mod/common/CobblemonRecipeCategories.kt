/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common

import net.minecraft.world.item.crafting.RecipeBookCategory
import net.minecraft.world.inventory.RecipeBookType

// PT144: RecipeBookCategories no longer enum in MC 26.1.x — each category is an RecipeBookCategory instance.
enum class CobblemonRecipeCategories {

    COOKING_POT_SEARCH("COBBLEMON_COOKING_POT_SEARCH"),
    COOKING_POT_FOODS("COBBLEMON_COOKING_POT_FOODS"),
    COOKING_POT_MEDICINES("COBBLEMON_COOKING_POT_MEDICINES"),
    COOKING_POT_COMPLEX_DISHES("COBBLEMON_COOKING_POT_COMPLEX_DISHES"),
    COOKING_POT_MISC("COBBLEMON_COOKING_POT_MISC");

    companion object {
        private val categoryByName: MutableMap<String, RecipeBookCategory> = mutableMapOf()
        val customAggregateCategories: Map<RecipeBookCategory, List<RecipeBookCategory>> by lazy {
            mapOf(
                COOKING_POT_SEARCH.toVanillaCategory() to listOf(
                    COOKING_POT_FOODS.toVanillaCategory(), COOKING_POT_MISC.toVanillaCategory(), COOKING_POT_MEDICINES.toVanillaCategory(), COOKING_POT_COMPLEX_DISHES.toVanillaCategory()
                )
            )
        }
    }

    var id: String

    constructor(id: String) {
        this.id = id
    }

    fun toVanillaCategory(): RecipeBookCategory {
        return categoryByName.getOrPut(this.id) { RecipeBookCategory() }
    }
}

object CobblemonRecipeBookTypes {
    const val COOKING_POT_NAME = "COBBLEMON_COOKING_POT"
    // PT144: RecipeBookType enum no longer has cobblemon entry; fall back to CRAFTING until vanilla extension API surfaces.
    val COOKING_POT: RecipeBookType = RecipeBookType.CRAFTING
}