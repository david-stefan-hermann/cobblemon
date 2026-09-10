/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.ai

import com.cobblemon.mod.common.api.serialization.StringIdentifiedObjectAdapter
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.world.entity.ai.targeting.TargetingConditions
import net.minecraft.world.phys.AABB

/**
 * How deeply a Pokémon sleeps. This takes the current situation and decides if a Pokémon should fall asleep or wake.
 *
 * A depth should be registered by name in [SleepDepth.depths].
 *
 * @author Hiroku
 * @since July 17th, 2022
 */
interface SleepDepth {
    companion object {
        val comatose = object : SleepDepth {
            override fun canSleep(pokemonEntity: PokemonEntity) = true
            override fun shouldWake(pokemonEntity: PokemonEntity) = true
        }

        val normal = object : SleepDepth {
            // PT143: Level.getNearbyPlayers removed in MC 26.1.x; replace with player-list filter on bounding-box.
            private fun nearbyPlayers(pokemonEntity: PokemonEntity): List<net.minecraft.world.entity.player.Player> {
                val box = AABB.ofSize(pokemonEntity.position(), 16.0, 16.0, 16.0)
                return pokemonEntity.level().players().filter { p -> p.boundingBox.intersects(box) }
            }
            override fun canSleep(pokemonEntity: PokemonEntity): Boolean {
                return nearbyPlayers(pokemonEntity).isEmpty()
            }

            override fun shouldWake(pokemonEntity: PokemonEntity): Boolean {
                return nearbyPlayers(pokemonEntity).any { !it.isShiftKeyDown }
            }
        }

        val depths = mutableMapOf(
            "comatose" to comatose,
            "normal" to normal
        )

        val adapter = StringIdentifiedObjectAdapter({ depths[it] ?: throw IllegalArgumentException("Unknown sleep depth: $it") })
    }

    fun canSleep(pokemonEntity: PokemonEntity): Boolean
    fun shouldWake(pokemonEntity: PokemonEntity): Boolean
}