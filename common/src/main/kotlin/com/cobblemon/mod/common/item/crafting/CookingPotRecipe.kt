/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.item.crafting

import com.cobblemon.mod.common.CobblemonRecipeSerializers
import com.cobblemon.mod.common.CobblemonRecipeTypes
import com.cobblemon.mod.common.api.tags.CobblemonItemTags
import com.cobblemon.mod.common.util.codec.CodecUtils.createByStringCodec
import com.cobblemon.mod.common.util.readIdentifier
import com.cobblemon.mod.common.util.readString
import com.cobblemon.mod.common.util.writeIdentifier
import com.cobblemon.mod.common.util.writeString
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.NonNullList
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.TagEntry
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.ShapedRecipePattern
import net.minecraft.world.level.Level

class CookingPotRecipe(
    val pattern: ShapedRecipePattern,
    override val result: ItemStackTemplate,
    override val groupName: String,
    override val category: CookingPotBookCategory,
    override val seasoningTag: TagKey<Item>,
    override val seasoningProcessors: List<SeasoningProcessor>
) : CookingPotRecipeBase {
    override fun getType() = CobblemonRecipeTypes.COOKING_POT_COOKING
    // PT138: canCraftInDimensions/getIngredients removed from Recipe interface; RecipeSerializer is final record (cannot extend)
    @Suppress("UNCHECKED_CAST")
    override fun getSerializer(): RecipeSerializer<out net.minecraft.world.item.crafting.Recipe<CraftingInput>> =
        Serializer.INSTANCE as RecipeSerializer<out net.minecraft.world.item.crafting.Recipe<CraftingInput>>
    // Non-override: ShapedRecipePattern.ingredients() now returns List<Optional<Ingredient>>
    fun ingredients(): List<Ingredient> = this.pattern.ingredients().mapNotNull { it.orElse(null) }
    override fun matches(input: CraftingInput, level: Level): Boolean {
        // Create a filtered CraftingInput with only slots 1-9
        val filteredItems = (0..8).mapNotNull { index ->
            if (index < input.size()) input.getItem(index) else ItemStack.EMPTY
        }
        val filteredInput = CraftingInput.of(3, 3, filteredItems)

        // Perform pattern matching on the filtered input
        val matches = this.pattern.matches(filteredInput)
        return matches
    }

    // PT138: RecipeSerializer is now a final record class (cannot be extended).
    // Wrapper converted to object holding the canonical INSTANCE built via direct record constructor.
    object Serializer {
        val CODEC: MapCodec<CookingPotRecipe> = RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    ShapedRecipePattern.MAP_CODEC.forGetter { recipe -> recipe.pattern },
                    ItemStackTemplate.CODEC.fieldOf("result").forGetter { recipe -> recipe.result },
                    Codec.STRING.optionalFieldOf("group", "").forGetter { recipe -> recipe.groupName },
                    CookingPotBookCategory.CODEC.fieldOf("category").orElse(CookingPotBookCategory.MISC).forGetter { recipe -> recipe.category },
                    TagKey.codec(Registries.ITEM).fieldOf("seasoningTag").orElse(CobblemonItemTags.EMPTY).forGetter { recipe -> recipe.seasoningTag },
                    createByStringCodec<SeasoningProcessor>(
                        { SeasoningProcessor.processors[it] },
                        { it.type },
                        { "Unknown seasoning processor: $it" }
                    ).listOf().fieldOf("seasoningProcessors").forGetter { recipe -> recipe.seasoningProcessors }
                ).apply(instance, ::CookingPotRecipe)
            }

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, CookingPotRecipe> = StreamCodec.of(::toNetwork, ::fromNetwork)

            private fun fromNetwork(buffer: RegistryFriendlyByteBuf): CookingPotRecipe {
                val group = buffer.readUtf()
                val category = buffer.readEnum(CookingPotBookCategory::class.java)
                val seasoningTag = TagKey.create(Registries.ITEM, buffer.readIdentifier())
                val pattern = ShapedRecipePattern.STREAM_CODEC.decode(buffer)
                val result = ItemStackTemplate.STREAM_CODEC.decode(buffer)
                val seasoningProcessors = buffer.readList {
                    val type = buffer.readString()
                    SeasoningProcessor.processors[type] ?: error("Unknown seasoning processor: $type")
                }
                return CookingPotRecipe(pattern, result, group, category, seasoningTag, seasoningProcessors)
            }

            private fun toNetwork(buffer: RegistryFriendlyByteBuf, recipe: CookingPotRecipe) {
                buffer.writeUtf(recipe.groupName)
                buffer.writeEnum(recipe.category)
                buffer.writeIdentifier(recipe.seasoningTag.location)
                ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern)
                ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.result)
                buffer.writeCollection(recipe.seasoningProcessors) { _, it ->
                    buffer.writeString(it.type)
                }
            }

        // PT138: direct record-constructor — RecipeSerializer(MapCodec, StreamCodec)
        val INSTANCE: RecipeSerializer<CookingPotRecipe> = RecipeSerializer(CODEC, STREAM_CODEC)
    }
}
