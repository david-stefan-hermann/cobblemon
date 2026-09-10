/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.item

// ItemProperties.register was removed in MC 26.1.x.
// The new model system uses ClientItem JSON definitions (item model JSONs with
// "model" -> "type: condition/select" properties) for what these predicates
// used to do at runtime. Migration deferred — this object now compiles as a
// no-op so the rest of the build can proceed.
object CobblemonModelPredicateRegistry {
    fun registerPredicates() {
        // Predicates handled via ClientItem JSON in MC 26.1.x. Runtime API removed.
    }
}
