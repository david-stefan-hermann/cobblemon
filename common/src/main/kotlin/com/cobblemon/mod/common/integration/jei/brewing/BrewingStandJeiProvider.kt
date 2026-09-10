/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.integration.jei.brewing

import com.cobblemon.mod.common.CobblemonRecipeTypes
import com.cobblemon.mod.common.integration.jei.CobblemonJeiProvider
import mezz.jei.api.constants.RecipeTypes
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import net.minecraft.client.Minecraft

class BrewingStandJeiProvider : CobblemonJeiProvider {
    override fun registerCategory(registration: IRecipeCategoryRegistration) {
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        // PT135-DEFER: ClientLevel.recipeManager removed in MC 26.1.x — JEI integration needs ClientRecipeAccess refactor.
        val jeiRecipes = emptyList<JeiBrewingStandRecipe>()

        registration.addRecipes(RecipeTypes.BREWING, jeiRecipes)
    }

    override fun registerRecipeCatalsysts(registration: IRecipeCatalystRegistration) { }
}