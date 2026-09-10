/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.integration.jei.cooking

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.berry.Berries
import com.cobblemon.mod.common.api.cooking.Seasonings
import com.cobblemon.mod.common.block.entity.CampfireBlockEntity
import com.cobblemon.mod.common.block.entity.CampfireBlockEntity.Companion.CRAFTING_GRID_WIDTH
import com.cobblemon.mod.common.block.entity.CampfireBlockEntity.Companion.SEASONING_SLOTS
import com.cobblemon.mod.common.client.gui.cookingpot.CookingPotScreen.Companion.COOK_PROGRESS_HEIGHT
import com.cobblemon.mod.common.client.gui.cookingpot.CookingPotScreen.Companion.COOK_PROGRESS_SPRITE
import com.cobblemon.mod.common.client.gui.cookingpot.CookingPotScreen.Companion.COOK_PROGRESS_WIDTH
import com.cobblemon.mod.common.item.crafting.CookingPotRecipeBase
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.drawable.IDrawableAnimated
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.types.IRecipeType
import mezz.jei.api.recipe.category.IRecipeCategory
import mezz.jei.api.registration.IRecipeCategoryRegistration
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.crafting.Ingredient

class CampfirePotRecipeCategory(registration: IRecipeCategoryRegistration) : IRecipeCategory<CookingPotRecipeBase> {

    companion object {
        // port/26.2: JEI 30.32 replaced RecipeType with IRecipeType; the factory signature is unchanged.
        val RECIPE_TYPE: IRecipeType<CookingPotRecipeBase> =
            IRecipeType.create("cobblemon", "campfire_pot_recipe", CookingPotRecipeBase::class.java)
        val CAMPFIRE_POT_TEXTURE: Identifier = cobblemonResource("textures/gui/jei/campfire_pot.png")

        const val TEXTURE_WIDTH = 146
        const val TEXTURE_HEIGHT = 59
        const val WIDTH = 146
        const val HEIGHT = 59

        val EXAMPLE_SEASONINGS = BuiltInRegistries.ITEM.map { it.defaultInstance }.filter { Seasonings.isSeasoning(it) }
    }

    val guiHelper: IGuiHelper = registration.jeiHelpers.guiHelper
    // port/26.2: the real textures are back. They had been swapped for blank drawables on the grounds
    // that JEI referenced ResourceLocation, which 26.2 renamed to Identifier - the JEI build for 26.2
    // uses Identifier, so createDrawable/drawableBuilder work again.
    val campfirePotBackground: IDrawable = guiHelper.drawableBuilder(CAMPFIRE_POT_TEXTURE, 0, 0, WIDTH, HEIGHT)
        .setTextureSize(TEXTURE_WIDTH, TEXTURE_HEIGHT)
        .build()
    val cookProgressSprite: IDrawableAnimated =
        guiHelper.drawableBuilder(COOK_PROGRESS_SPRITE, 0, 0, COOK_PROGRESS_WIDTH, COOK_PROGRESS_HEIGHT)
            .setTextureSize(22, COOK_PROGRESS_HEIGHT)
            .buildAnimated(CampfireBlockEntity.COOKING_TOTAL_TIME, IDrawableAnimated.StartDirection.LEFT, false)
    val campfirePotIcon: IDrawable = guiHelper.createDrawableItemStack(CobblemonItems.CAMPFIRE_POT_BLACK.defaultInstance)

    override fun getRecipeType(): IRecipeType<CookingPotRecipeBase> = RECIPE_TYPE
    override fun getTitle(): Component = lang("container.campfire_pot")
    override fun getIcon(): IDrawable = campfirePotIcon

    // port/26.2: IRecipeCategory no longer takes a background drawable - it asks for the category's size
    // and the background is drawn in draw() instead.
    override fun getWidth(): Int = WIDTH
    override fun getHeight(): Int = HEIGHT

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: CookingPotRecipeBase,
        focuses: IFocusGroup
    ) {
        // PT138: Recipe.getIngredients() removed → fetch via concrete subtype helpers
        val ingredients: List<Ingredient> = when (recipe) {
            is com.cobblemon.mod.common.item.crafting.CookingPotRecipe -> recipe.ingredients()
            is com.cobblemon.mod.common.item.crafting.CookingPotShapelessRecipe -> recipe.ingredients()
            else -> emptyList()
        }
        ingredients.forEachIndexed { index, ingredient ->
            val column = index / CRAFTING_GRID_WIDTH
            val row = index % CRAFTING_GRID_WIDTH

            val x = 16 + row * 18
            val y = 1 + column * 18

            builder.addSlot(RecipeIngredientRole.INPUT, x, y).addIngredients(ingredient)
        }

        if (recipe.seasoningProcessors.isNotEmpty()) {
            val processedSeasonings = EXAMPLE_SEASONINGS.filter { item ->
                var valid = true
                if (!item.`is`(recipe.seasoningTag)) return@filter false
                recipe.seasoningProcessors.forEach {
                    if (!it.consumesItem(item)) {
                        valid = false
                        return@forEach
                    }
                }
                valid
            }

            for ((index, _) in SEASONING_SLOTS.withIndex()) {
                val x = 93 + index * 18
                val y = 1

                builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                    .addIngredients(VanillaTypes.ITEM_STACK, processedSeasonings.shuffled())
            }
        }

        // PT138: Recipe.getResultItem(HolderLookup.Provider) removed → resultItem() helper on CookingPotRecipeBase
        // PT138: Ingredient.of(ItemStack) removed → Ingredient.of(ItemLike...) only — use Item directly
        builder.addSlot(RecipeIngredientRole.OUTPUT, 111, 38).addIngredients(Ingredient.of(recipe.resultItem().item))
    }

    // port/26.2: draw takes a GuiGraphicsExtractor now, and since the category no longer supplies a
    // background drawable this is where the pot texture is painted, ahead of the cook progress sprite.
    override fun draw(
        recipe: CookingPotRecipeBase,
        recipeSlotsView: IRecipeSlotsView,
        guiGraphics: GuiGraphicsExtractor,
        mouseX: Double,
        mouseY: Double
    ) {
        campfirePotBackground.draw(guiGraphics, 0, 0)
        super.draw(recipe, recipeSlotsView, guiGraphics, mouseX, mouseY)
        cookProgressSprite.draw(guiGraphics, 79, 22)
    }
}