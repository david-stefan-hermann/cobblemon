/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric

import com.cobblemon.mod.common.Cobblemon
import net.fabricmc.api.ModInitializer

/**
 * PT150: fabric-api modules required by the original CobblemonFabric implementation are not yet
 * remapped for MC 26.1.x (BiomeModifications, ArgumentTypeRegistry, CommandRegistrationCallback,
 * EntitySleepEvents, ServerEntityWorldChangeEvents, ServerLivingEntityEvents, ServerLifecycleEvents,
 * ServerTickEvents, UseBlockCallback, UseEntityCallback, GameRuleRegistry, FabricItemGroup,
 * FabricItemGroupEntries, ItemGroupEvents, LootTableEvents, ServerPlayConnectionEvents,
 * FabricDefaultAttributeRegistry, TradeOfferHelper, PointOfInterestHelper, CompostingChanceRegistry,
 * StrippableBlockRegistry, IdentifiableResourceReloadListener, ResourceManagerHelper, FabricLoader).
 *
 * Stubbed pending upstream port — onInitialize is a no-op so the Fabric jar can compile. The mod will
 * declare itself but register nothing at runtime. Reintroduce full initialization in PT15X+ when
 * fabric-api ships a 26.1.x build.
 */
class CobblemonFabric : ModInitializer {
    override fun onInitialize() {
        Cobblemon.LOGGER.warn("CobblemonFabric.onInitialize stubbed (PT150) — fabric-api 26.1.x not yet shipped, no events registered.")
    }
}
