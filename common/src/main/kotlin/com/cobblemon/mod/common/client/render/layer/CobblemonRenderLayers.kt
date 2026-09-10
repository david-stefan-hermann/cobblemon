/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.layer

import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.resources.Identifier
import java.util.function.BiFunction
import java.util.function.Function

// PT128: RenderStateShard/CompositeState.builder removed in MC 26.1 — fallback to RenderTypes static methods. Atlas berries layer maps to entityCutout(berries) until pipeline-aware rebuild.
object CobblemonRenderLayers {
    val BERRY_LAYER: RenderType = RenderTypes.entityCutout(cobblemonResource("textures/atlas/berries.png"))

    val ENTITY_TRANSLUCENT: BiFunction<Identifier, Boolean, RenderType> = BiFunction { texture, affectsOutline ->
        RenderTypes.entityTranslucent(texture, affectsOutline)
    }

    val ENTITY_CUTOUT: Function<Identifier, RenderType> = Function { texture ->
        RenderTypes.entityCutout(texture)
    }
}