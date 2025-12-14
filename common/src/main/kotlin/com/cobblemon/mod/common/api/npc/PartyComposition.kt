/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.npc

import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.cobblemonResource
import kotlin.random.Random

/**
 * A kind of definition for how to compose a party of Pokémon given a [PartyPool].
 *
 * @author Hiroku
 * @since December 7th, 2025
 */
class PartyComposition {
    var id = cobblemonResource("dummy_composition")
    val displayName = "cobblemon.party_composition.$id" // dunno if i will use this yet
    val scrambleOrder: Boolean = false
    val slot1 = listOf<String>()
    val slot2 = listOf<String>()
    val slot3 = listOf<String>()
    val slot4 = listOf<String>()
    val slot5 = listOf<String>()
    val slot6 = listOf<String>()

    fun compose(pool: PartyPool, level: Int, desiredPokemonCount: Int, random: Random = Random.Default): List<Pokemon> {
        val chosenEntries = mutableListOf<PartyPool.PoolEntry>()
        val availableEntries = pool.entries.filter { level in it.npcLevels }

        for (labelSet in listOf(slot1, slot2, slot3, slot4, slot5, slot6).take(desiredPokemonCount)) {
            pool.tryChoosingEntry(labelSet, random, availableEntries, chosenEntries)
        }

        val finalEntries = if (scrambleOrder) {
            chosenEntries.shuffled(random)
        } else {
            chosenEntries
        }

        val pokemon = finalEntries.map { entry ->
            val lvlVariation = if (entry.levelVariation.first == entry.levelVariation.last) {
                entry.levelVariation.first
            } else {
                entry.levelVariation.random(random)
            }

            val finalLevel = level + lvlVariation
            val properties = entry.pokemon.copy()
            properties.level = properties.level ?: finalLevel
            val pokemon = properties.create()
            entry.movesetBuilders.randomOrNull()?.let(pokemon::initializeMovesetFrom)
            pokemon
        }

        return pokemon
    }
}