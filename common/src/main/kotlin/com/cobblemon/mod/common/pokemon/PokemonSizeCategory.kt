/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.pokemon

import com.cobblemon.mod.common.Cobblemon

enum class PokemonSizeCategory {
    XS,
    S,
    M,
    L,
    XL;
    
    companion object {
        // Find which size category corresponds to the given scale modifier.
        // The scale modifier should be between 1 - Cobblemon.config.pokemonSizeVariation and 1 + Cobblemon.config.pokemonSizeVariation
        // First 20% -> XS, etc.
        fun fromScale(scaleModifier: Float): PokemonSizeCategory {
            val range = Cobblemon.config.pokemonSizeVariation * 2
            val segmentSize = range / PokemonSizeCategory.entries.size
            val adjustedScale = scaleModifier - (1 - Cobblemon.config.pokemonSizeVariation)
            val index = (adjustedScale / segmentSize).toInt().coerceIn(0, PokemonSizeCategory.entries.size - 1)
            return PokemonSizeCategory.entries[index]
        }
    }
}