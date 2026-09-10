/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.keybind

import com.cobblemon.mod.common.Cobblemon
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier

// PT142: KeyMapping.Category replaced String-based categories in MC 26.1.x; register Cobblemon categories
object KeybindCategories {
    val COBBLEMON_CATEGORY: KeyMapping.Category by lazy {
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Cobblemon.MODID, "cobblemon"))
    }
    val COBBLEMON_DEBUG_CATEGORY: KeyMapping.Category by lazy {
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Cobblemon.MODID, "cobblemon_debug"))
    }
}
