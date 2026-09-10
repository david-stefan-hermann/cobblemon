/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.accessor;

// PT149: RecipeBookComponent removed in MC 26.1.x. New recipe book uses StackedItemContents + RecipeBookCategory.
// Stubbed and disabled in mixins.cobblemon-common.json. Reintroduce as state-aware accessor in PT15X+.
public interface RecipeBookComponentAccessor {
}
