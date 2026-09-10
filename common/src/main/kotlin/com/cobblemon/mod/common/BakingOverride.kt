/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common

import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.resources.Identifier

/**
 * Contains information for forcing a model to be baked, for models that aren't associated with any
 * block state.
 *
 * port/26.2: ModelResourceLocation is gone along with the old model registry, so an override no longer
 * carries a second identity to register the baked model under. Fabric's extra-model API keys a model by
 * an opaque ExtraModelKey object instead, which the platform holds; this stays platform-agnostic by
 * letting the platform install a [modelResolver] that looks the baked model up on demand.
 *
 * Resolution has to be lazy rather than pushed once: models are re-baked on every resource reload, and
 * a value captured at registration time would go stale after the first reload.
 *
 * @param modelLocation The location of the model
 */
data class BakingOverride(
    val modelLocation: Identifier
) {
    /** Installed by the platform. Null before the client has registered its model loading plugin. */
    @Transient
    var modelResolver: (() -> BlockStateModel?)? = null

    /** The baked model, or null if models have not been baked yet. */
    fun getModel(): BlockStateModel? = modelResolver?.invoke()
}
