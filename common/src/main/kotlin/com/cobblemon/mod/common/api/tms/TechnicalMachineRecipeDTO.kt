/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.tms

import com.cobblemon.mod.common.util.DeferredItemTagHolderSet
import com.google.gson.annotations.SerializedName
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.item.crafting.Ingredient

/**
 * DTO used for [TechnicalMachineRecipe] JSON loading because Gson cannot deserialize Minecraft Ingredients.
 */
data class TechnicalMachineRecipeDTO(
    val item: Identifier?,
    val tag: Identifier?,
    @SerializedName("count") private val _count: Int?,
) {
    private val count: Int
        get() = _count ?: 1

    fun toTechnicalMachineRecipe(): TechnicalMachineRecipe {
        val ingredient = when {
            item != null ->
                Ingredient.of(
                    BuiltInRegistries.ITEM.get(item).orElse(null)?.value()
                        ?: error("Unknown item $item in a technical machine recipe")
                )

            // port/26.2: Ingredient.of(TagKey) is gone and the HolderSet overload replaces it. Resolving
            // the tag here would throw, because technical machines are read before the tag manager has
            // run - so the set looks the tag up when the ingredient is matched instead.
            tag != null -> Ingredient.of(DeferredItemTagHolderSet(TagKey.create(Registries.ITEM, tag)))

            else ->
                error("Recipe entry must define either 'item' or 'tag'")
        }

        return TechnicalMachineRecipe(
            ingredient = ingredient,
            count = count
        )
    }
}