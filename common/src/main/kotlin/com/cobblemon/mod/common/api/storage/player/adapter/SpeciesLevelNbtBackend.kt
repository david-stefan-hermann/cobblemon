/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.storage.player.adapter

import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreTypes
import com.cobblemon.mod.common.api.storage.player.SpeciesLevelManager

class SpeciesLevelNbtBackend : NbtBackedPlayerData<SpeciesLevelManager>("species_levels", PlayerInstancedDataStoreTypes.SPECIES_LEVELS) {
    override val codec = SpeciesLevelManager.CODEC
    override val defaultData = SpeciesLevelJsonBackend.defaultDataFunc
}
