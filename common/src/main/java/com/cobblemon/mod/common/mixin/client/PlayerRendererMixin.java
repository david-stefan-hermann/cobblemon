/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

// PT149: PlayerRenderer / LivingEntityRenderer now require 3 type-args (E, S extends EntityRenderState, M extends EntityModel<? super S>)
// and PlayerModel no longer accepts a type parameter. render(...) signature is state-based.
// Stubbed and disabled in mixins.cobblemon-common.json. Reintroduce via state-based extension in PT15X+.
public abstract class PlayerRendererMixin {
}
