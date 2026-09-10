/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

// PT149: Player class APIs heavily refactored in MC 26.1.x:
//   - Level.isClientSide/random now private/protected,
//   - CompoundTag.getString/Int/Boolean now return Optional,
//   - absMoveTo signature changed and absRotateTo removed,
//   - CompoundTag.getList / getUUID signature changed,
//   - Player.getServer() no longer accessible on mixin target,
//   - Level.getGameRules() relocated.
// Stubbed and disabled in mixins.cobblemon-common.json. Reintroduce in PT15X+.
public abstract class PlayerMixin {
}
