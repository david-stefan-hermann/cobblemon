/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.storage.player

import com.cobblemon.mod.common.CobblemonNetwork.sendPacket
import com.cobblemon.mod.common.api.scheduling.ScheduledTask
import com.cobblemon.mod.common.api.scheduling.ServerTaskTracker
import com.cobblemon.mod.common.api.storage.player.client.ClientSpeciesLevelManager
import com.cobblemon.mod.common.net.messages.client.SetClientPlayerDataPacket
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.getPlayer
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import java.util.UUID
import net.minecraft.resources.ResourceLocation

class SpeciesLevelManager(
    override val uuid: UUID,
    val speciesLevels: MutableMap<ResourceLocation, Int> = mutableMapOf()
) : InstancedPlayerData {

    override fun initialize() {}

    override fun toClientData(): ClientSpeciesLevelManager {
        return ClientSpeciesLevelManager(speciesLevels.toMutableMap())
    }

    fun getHighestLevel(speciesId: ResourceLocation): Int {
        return speciesLevels[speciesId] ?: 0
    }

    fun updateFromPokemon(pokemon: Pokemon): Boolean {
        return updateLevel(pokemon.species.resourceIdentifier, pokemon.level)
    }

    fun updateLevel(speciesId: ResourceLocation, level: Int): Boolean {
        if (level <= 0) return false
        val current = speciesLevels[speciesId] ?: 0
        if (level <= current) return false
        speciesLevels[speciesId] = level
        syncClient(mapOf(speciesId to level), isIncremental = true)
        return true
    }

    fun replaceLevels(levels: Map<ResourceLocation, Int>) {
        speciesLevels.clear()
        speciesLevels.putAll(levels)
        syncClient(speciesLevels, isIncremental = false)
    }

    fun scheduleFullSyncFromStores(
        party: Iterable<Pokemon?>,
        pc: Iterable<Pokemon?>,
        batchSize: Int = 50
    ) {
        val iterator = sequence {
            for (pokemon in party) {
                if (pokemon != null) yield(pokemon)
            }
            for (pokemon in pc) {
                if (pokemon != null) yield(pokemon)
            }
        }.iterator()

        if (!iterator.hasNext()) {
            replaceLevels(emptyMap())
            return
        }

        val levels = mutableMapOf<ResourceLocation, Int>()

        ScheduledTask.Builder()
            .tracker(ServerTaskTracker)
            .interval(0F)
            .infiniteIterations()
            .execute { task ->
                var processed = 0
                while (processed < batchSize && iterator.hasNext()) {
                    val pokemon = iterator.next()
                    val speciesId = pokemon.species.resourceIdentifier
                    val level = pokemon.level
                    val current = levels[speciesId] ?: 0
                    if (level > current) {
                        levels[speciesId] = level
                    }
                    processed++
                }

                if (!iterator.hasNext()) {
                    replaceLevels(levels)
                    task.expire()
                }
            }
            .build()
    }

    private fun syncClient(data: Map<ResourceLocation, Int>, isIncremental: Boolean) {
        uuid.getPlayer()?.sendPacket(
            SetClientPlayerDataPacket(
                type = PlayerInstancedDataStoreTypes.SPECIES_LEVELS,
                playerData = ClientSpeciesLevelManager(data.toMutableMap()),
                isIncremental = isIncremental
            )
        )
    }

    companion object {
        val CODEC: Codec<SpeciesLevelManager> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.xmap(UUID::fromString, UUID::toString)
                    .fieldOf("uuid")
                    .forGetter { it.uuid },
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT)
                    .xmap({ it.toMutableMap() }, { it })
                    .fieldOf("speciesLevels")
                    .forGetter { it.speciesLevels }
            ).apply(instance) { uuid, levels ->
                SpeciesLevelManager(uuid, levels)
            }
        }
    }
}
