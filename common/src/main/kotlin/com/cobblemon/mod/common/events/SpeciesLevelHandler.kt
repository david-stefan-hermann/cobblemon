/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.events

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.pokemon.LevelUpEvent
import com.cobblemon.mod.common.api.events.pokemon.PokemonGainedEvent

object SpeciesLevelHandler : EventHandler {
    override fun registerListeners() {
        CobblemonEvents.POKEMON_GAINED.subscribe(Priority.NORMAL, ::onPokemonGained)
        CobblemonEvents.LEVEL_UP_EVENT.subscribe(Priority.NORMAL, ::onLevelUp)
    }

    fun onPokemonGained(event: PokemonGainedEvent) {
        Cobblemon.playerDataManager.getSpeciesLevelData(event.playerId).updateFromPokemon(event.pokemon)
    }

    fun onLevelUp(event: LevelUpEvent) {
        val ownerId = event.pokemon.getOwnerUUID() ?: return
        Cobblemon.playerDataManager.getSpeciesLevelData(ownerId).updateLevel(
            event.pokemon.species.resourceIdentifier,
            event.newLevel
        )
    }
}
