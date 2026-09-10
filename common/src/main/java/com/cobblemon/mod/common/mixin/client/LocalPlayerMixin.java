/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

// PT149: Input package moved + record-ified (forward()/backward()/jump()/shift() methods rather than jumping field) in MC 26.1.x.
// LocalPlayer.input field type changed. Stubbed and disabled in mixins.cobblemon-common.json.
// Reintroduce after Input record migration in PT15X+.
public abstract class LocalPlayerMixin {
}
