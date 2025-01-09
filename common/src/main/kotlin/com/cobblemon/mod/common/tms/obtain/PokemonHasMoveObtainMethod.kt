/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.tms.obtain

import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.tms.ObtainMethod
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.party
import com.google.gson.annotations.SerializedName
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.server.level.ServerPlayer

/**
 * An [ObtainMethod] that triggers when a [Pokemon] in the player's party knows a certain [Move]
 *
 * @author whatsy
 */
class PokemonHasMoveObtainMethod(val moveId: String = "splash") : ObtainMethod {
    override val passive = true

    companion object {
        val ID = cobblemonResource("pokemon_knows")

        fun readFromBuffer(buffer: RegistryFriendlyByteBuf): PokemonHasMoveObtainMethod {
            val moveId = buffer.readUtf()
            return PokemonHasMoveObtainMethod(moveId)
        }
    }

    override fun matches(player: ServerPlayer): Boolean {
        player.party().forEach { pokemon ->
            pokemon.allAccessibleMoves.forEach { move ->
                if (move == Moves.getByName(moveId)) return true
            }
        }
        return false
    }

    override fun writeToBuffer(buffer: RegistryFriendlyByteBuf) {
        buffer.writeUtf("cobblemon:pokemon_knows")
        buffer.writeUtf(moveId)
    }
}
