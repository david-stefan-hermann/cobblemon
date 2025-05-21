package com.cobblemon.mod.common.api.tms

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.storage.player.InstancedPlayerData
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreTypes
import com.cobblemon.mod.common.api.storage.player.client.ClientInstancedPlayerData
import com.cobblemon.mod.common.api.tms.AbstractTMMoveManager
import com.cobblemon.mod.common.api.storage.player.client.ClientTMMoveManager
import com.cobblemon.mod.common.pokemon.Pokemon
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.resources.ResourceLocation
import java.util.UUID

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
        val learnableTMs = TechnicalMachines.tmMap.values
                .filter { tm -> pokemon.allAccessibleMoves.contains(tm.moveName) }

        for (tm in learnableTMs) {
            learn(tm.id)
        }
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