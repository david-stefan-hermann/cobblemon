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
import net.minecraft.server.level.ServerPlayer

/**
 * An [ObtainMethod] that triggers when a [Pokemon] in the player's party knows a certain [Move]
 *
 * @author whatsy
 */
class PokemonHasMoveObtainMethod : ObtainMethod {

    companion object {
        val ID = cobblemonResource("pokemon_knows")
    }

    @SerializedName("move")
    val moveId: String = "splash"
    override val passive = true

    override fun matches(player: ServerPlayer): Boolean {
        player.party().forEach {
            it.allAccessibleMoves.forEach {
                if (it.equals(Moves.getByName(moveId))) return true
            }
        }

        return false
    }
}