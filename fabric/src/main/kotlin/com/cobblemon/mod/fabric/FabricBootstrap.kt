/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric

import net.fabricmc.api.ModInitializer

/**
 * PT150: CobblemonFabric was reduced to a no-op stub because fabric-api 26.1.x is not yet shipped.
 * FabricBootstrap delegates to the instance onInitialize() to retain the fabric.mod.json entrypoint
 * contract. Reintroduce CobblemonFabric.initialize() companion in PT15X+ once full Fabric wiring returns.
 */
class FabricBootstrap : ModInitializer {
    override fun onInitialize() {
        CobblemonFabric().onInitialize()
    }
}