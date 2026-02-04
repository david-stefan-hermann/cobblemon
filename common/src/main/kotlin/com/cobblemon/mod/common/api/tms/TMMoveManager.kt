/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.tms

import com.cobblemon.mod.common.api.storage.player.InstancedPlayerData
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreTypes
import com.cobblemon.mod.common.api.storage.player.client.ClientTMMoveManager
import com.cobblemon.mod.common.api.scheduling.ScheduledTask
import com.cobblemon.mod.common.api.scheduling.ServerTaskTracker
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.pokemon.Pokemon
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.resources.ResourceLocation
import java.util.*

class TMMoveManager(
    override val uuid: UUID,
    override val learnedTMs: MutableSet<ResourceLocation> = mutableSetOf()
) : AbstractTMMoveManager(), InstancedPlayerData {

    override val storeType = PlayerInstancedDataStoreTypes.TM_MOVES

    override fun initialize() {}

    override fun toClientData(): ClientTMMoveManager {
        return ClientTMMoveManager(learnedTMs.toMutableSet())
    }

    override fun toClientDataFrom(set: Set<ResourceLocation>): ClientTMMoveManager {
        return ClientTMMoveManager(set.toMutableSet())
    }

    fun syncTMsFromPokemon(pokemon: Pokemon) {
        val learnableTMs = getLearnableTMsFromPokemon(pokemon)

        learn(learnableTMs)
    }

    fun syncTMFromMove(moveTemplate: MoveTemplate) {
        val tmId = TechnicalMachines.moveToTM[moveTemplate]?.id ?: return
        learn(listOf(tmId))
    }

    fun getLearnableTMsFromPokemon(pokemon: Pokemon): Collection<ResourceLocation> {
        return TechnicalMachines.tmMap.values
            .filter { tm -> pokemon.allAccessibleMoves.contains(tm.moveName) }
            .map { tm -> tm.id }
    }

    fun scheduleFullSyncFromStores(
        party: Iterable<Pokemon?>,
        pc: Iterable<Pokemon?>,
        batchSize: Int = 25
    ) {
        val iterator = sequence {
            for (pokemon in party) {
                if (pokemon != null) yield(pokemon)
            }
            for (pokemon in pc) {
                if (pokemon != null) yield(pokemon)
            }
        }.iterator()

        if (!iterator.hasNext()) return

        val tmIds = mutableSetOf<ResourceLocation>()

        ScheduledTask.Builder()
            .tracker(ServerTaskTracker)
            .interval(0f)
            .infiniteIterations()
            .execute { task ->
                var processed = 0
                while (processed < batchSize && iterator.hasNext()) {
                    val pokemon = iterator.next()
                    for (move in pokemon.allAccessibleMoves) {
                        TechnicalMachines.moveToTM[move]?.id?.let { tmIds.add(it) }
                    }
                    processed++
                }

                if (!iterator.hasNext()) {
                    learn(tmIds)
                    task.expire()
                }
            }
            .build()
    }

    companion object {
        val CODEC: Codec<TMMoveManager> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.xmap(UUID::fromString, UUID::toString)
                    .fieldOf("uuid")
                    .forGetter { it.uuid },
                Codec.list(ResourceLocation.CODEC)
                    .xmap({ it.toMutableSet() }, { it.toList() })
                    .fieldOf("learnedTMs")
                    .forGetter { it.learnedTMs }
            ).apply(instance) { uuid, learnedTMs ->
                TMMoveManager(uuid, learnedTMs)
            }
        }
    }
}
