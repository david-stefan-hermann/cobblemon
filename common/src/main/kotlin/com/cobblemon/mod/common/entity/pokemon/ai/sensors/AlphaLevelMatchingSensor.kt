/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.pokemon.ai.sensors

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormEntityParticlePacket
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.getMemorySafely
import com.cobblemon.mod.common.util.party
import java.util.UUID
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.ai.sensing.Sensor

class AlphaLevelMatchingSensor : Sensor<PokemonEntity>(20) {
    private var lastMatchingPlayerId: UUID? = null

    override fun requires() = setOf(MemoryModuleType.NEAREST_PLAYERS)

    override fun doTick(world: ServerLevel, entity: PokemonEntity) {
        if (!entity.pokemon.isAlpha || !entity.pokemon.isWild() || entity.isBattling) {
            lastMatchingPlayerId = null
            return
        }

        val nearestPlayer = entity.brain.getMemorySafely(MemoryModuleType.NEAREST_PLAYERS)
            .orElse(emptyList())
            .filterIsInstance<ServerPlayer>()
            .minByOrNull { it.distanceToSqr(entity) }

        if (nearestPlayer == null || nearestPlayer.distanceToSqr(entity) > RADIUS_SQR) {
            lastMatchingPlayerId = null
            return
        }

        if (lastMatchingPlayerId != nearestPlayer.uuid) {
            lastMatchingPlayerId = nearestPlayer.uuid
            entity.cry()

            // test particles to make sure the level matching is working
            SpawnSnowstormEntityParticlePacket(cobblemonResource("shiny_ring"), entity.id, listOf("shiny_particles", "middle"))
                .sendToPlayersAround(entity.x, entity.y, entity.z, 64.0, world.dimension())
        }

        val strongestPartyLevel = nearestPlayer.party().maxOfOrNull { it.level } ?: return
        val targetLevel = when {
            strongestPartyLevel < 20 -> strongestPartyLevel + 5
            strongestPartyLevel in 31..40 -> strongestPartyLevel + 10
            strongestPartyLevel in 51..65 -> strongestPartyLevel + 15
            strongestPartyLevel >= 66 -> strongestPartyLevel + 20
            else -> strongestPartyLevel
        }.coerceAtMost(100)

        if (entity.pokemon.level != targetLevel) {
            entity.pokemon.level = targetLevel
        }
    }

    companion object {
        private const val RADIUS_SQR = 32.0 * 32.0
    }
}
