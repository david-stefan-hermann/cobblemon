/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

// PT149: ItemRenderer / BakedModel / ItemModelShaper / ModelResourceLocation API rewritten in MC 26.1.x.
// Item model overrides now flow through the ClientItem JSON system (item/<name>.json with select/condition).
// This mixin is stubbed and disabled in mixins.cobblemon-common.json. Reintroduce via JSON in PT15X+.
public abstract class ItemRendererMixin {
}
