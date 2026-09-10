/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.item.crafting.brewingstand

import com.cobblemon.mod.common.CobblemonRecipeSerializers
import com.cobblemon.mod.common.CobblemonRecipeTypes
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe
import net.minecraft.client.Minecraft
import net.minecraft.core.HolderLookup
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level

class BrewingStandRecipe(
    val groupName: String,
    val input: Ingredient,
    val bottle: Ingredient,
    val result: ItemStack
) : Recipe<BrewingStandInput> {

    override fun getType() = CobblemonRecipeTypes.BREWING_STAND
    // PT138: canCraftInDimensions/getResultItem removed; assemble takes only input; RecipeSerializer is final record
    @Suppress("UNCHECKED_CAST")
    override fun getSerializer(): RecipeSerializer<out Recipe<BrewingStandInput>> =
        Serializer.INSTANCE as RecipeSerializer<out Recipe<BrewingStandInput>>
    override fun assemble(input: BrewingStandInput): ItemStack = result.copy()
    // Non-override convenience accessor (Recipe no longer requires getResultItem)
    fun resultItem(): ItemStack = result.copy()
    override fun showNotification(): Boolean = true
    override fun placementInfo(): net.minecraft.world.item.crafting.PlacementInfo =
        net.minecraft.world.item.crafting.PlacementInfo.NOT_PLACEABLE
    override fun recipeBookCategory(): net.minecraft.world.item.crafting.RecipeBookCategory =
        net.minecraft.world.item.crafting.RecipeBookCategories.CRAFTING_MISC
    override fun group(): String = groupName

    override fun matches(input: BrewingStandInput, level: Level): Boolean {
        val ingredientMatches = this.input.test(input.getIngredient())
        val validBottles = input.getBottles()
            .filter { !it.isEmpty }
            .all { this.bottle.test(it) }

        return ingredientMatches && validBottles
    }

    companion object {
        // PT139: RecipeManager API drastically changed in MC 26.1.x. Stubbed pending API survey.
        @Suppress("UNUSED_PARAMETER")
        fun isBottle(itemStack: ItemStack, recipeManager: RecipeManager): Boolean = false

        @Suppress("UNUSED_PARAMETER")
        fun isInput(itemStack: ItemStack, recipeManager: RecipeManager): Boolean = false
    }

    // PT138: RecipeSerializer is now a final record class (cannot be extended). Wrapper → object with INSTANCE.
    object Serializer {
            val CODEC: MapCodec<BrewingStandRecipe> = RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter { recipe -> recipe.groupName },
                    Ingredient.CODEC.fieldOf("input").forGetter { recipe -> recipe.input },
                    Ingredient.CODEC.fieldOf("bottle").forGetter { recipe -> recipe.bottle },
                    ItemStack.CODEC.fieldOf("result").forGetter { recipe -> recipe.result }
                ).apply(instance, ::BrewingStandRecipe)
            }

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, BrewingStandRecipe> =
                StreamCodec.of(::toNetwork, ::fromNetwork)

            private fun fromNetwork(buffer: RegistryFriendlyByteBuf): BrewingStandRecipe {
                val group = buffer.readUtf(32767)
                val input = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)
                val bottle = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)
                val result = ItemStack.STREAM_CODEC.decode(buffer)
                return BrewingStandRecipe(group, input, bottle, result)
            }

            private fun toNetwork(buffer: RegistryFriendlyByteBuf, recipe: BrewingStandRecipe) {
                buffer.writeUtf(recipe.groupName)
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input)
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.bottle)
                ItemStack.STREAM_CODEC.encode(buffer, recipe.result)
            }

        // PT138: direct record-constructor — RecipeSerializer(MapCodec, StreamCodec)
        val INSTANCE: RecipeSerializer<BrewingStandRecipe> = RecipeSerializer(CODEC, STREAM_CODEC)
    }
}