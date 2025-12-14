/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.npc

import com.cobblemon.mod.common.api.moves.MovesetBuilder
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.weightedSelection
import kotlin.random.Random

/**
 * A pool of Pokémon to choose from, typically for generating a party for an NPC. The entries
 * of the pool work on a weight and label system which is used as part of a [PartyComposition].
 *
 * @author Hiroku
 * @since December 7th, 2025
 */
class PartyPool {
    var id = cobblemonResource("dummy")
    val displayName = "cobblemon.party_pool.$id"
    val entries = mutableListOf<PoolEntry>()

    class PoolEntry(
        val pokemon: PokemonProperties,
        val labels: List<String> = emptyList(),
        val required: List<String> = emptyList(),
        val excluded: List<String> = emptyList(),
        val maxSelectableTimes: Int = 6,
        val levelVariation: IntRange = 0..0,
        val npcLevels: IntRange = 1..100,
        val movesetBuilders: List<MovesetBuilder> = emptyList(),
        val weight: Int = 50
    )

    /**
     * Goes through each of the labels in order, trying to find a valid choice. Once it finds one, it skips the later
     * labels and adds the value to [chosenEntries] and returns. If none of the labels could find a valid entry, nothing is added.
     */
    fun tryChoosingEntry(
        labels: List<String>,
        random: Random,
        availableEntries: List<PoolEntry>,
        chosenEntries: MutableList<PoolEntry>,
    ) {
        for (label in labels) {
            val entries = availableEntries
                .filter { label in it.labels || label.lowercase() == "any" }
                .filter { entry ->
                    // Check required labels
                    entry.required.all { it in labels } &&
                            // Check excluded labels
                            entry.excluded.none { it in labels } &&
                            // Check max selectable times
                            chosenEntries.count { it == entry } < entry.maxSelectableTimes
                }

            if (entries.isNotEmpty()) {
                val selectedEntry = entries.weightedSelection(random) { it.weight } ?: return
                chosenEntries.add(selectedEntry)
                return
            }
        }
    }
}