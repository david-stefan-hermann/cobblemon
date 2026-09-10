/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common

import net.minecraft.client.Minecraft
import com.cobblemon.mod.common.client.render.model.BakedModel
import com.cobblemon.mod.common.client.render.ModelResourceLocation
import net.minecraft.resources.Identifier

/**
 * Contains information for forcing a model to be baked
 *
 * @param modelLocation The location of the model
 * @param modelIdentifier The identifier that the BakedModel will be registered to
 */
data class BakingOverride(
    val modelLocation: Identifier,
    val modelIdentifier: ModelResourceLocation
) {
    fun getModel(): BakedModel {
        // modelManager.getModel removed in MC 26.1.x — stub returns empty BakedModel
        return BakedModel()
    }
}
