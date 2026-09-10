/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric.mixin;

// PT150: Gui hud overlay mixin depends on fabric-api HudRenderCallback APIs unavailable for MC 26.1.x.
// Stubbed and disabled in mixins.cobblemon-fabric.json. Reintroduce in PT15X+ when fabric-api ships.
public abstract class GuiMixin {
}
