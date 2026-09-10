/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

// PT149: RenderLayer is now generic over EntityRenderState (not LivingEntity) in MC 26.1.x.
// renderColoredCutoutModel signature changed; ChatFormatting.stripFormatting removed.
// Stubbed and disabled in mixins.cobblemon-common.json. Reintroduce via state-based layer in PT15X+.
public abstract class VillagerProfessionLayerMixin {
}
