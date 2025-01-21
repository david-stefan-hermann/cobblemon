/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.tms

import com.cobblemon.mod.common.tms.obtain.ImpossibleObtainMethod
import com.cobblemon.mod.common.tms.obtain.NoneObtainMethod
import com.cobblemon.mod.common.tms.obtain.PlayerHasAdvancementObtainMethod
import com.cobblemon.mod.common.tms.obtain.PlayerYObtainMethod
import com.cobblemon.mod.common.tms.obtain.PokemonHasMoveObtainMethod
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.server.level.ServerPlayer

interface ObtainMethod {
    val passive: Boolean

    /**
     * Checks if this [ObtainMethod] matches the given player.
     */
    fun matches(player: ServerPlayer): Boolean

    /**
     * Serializes this [ObtainMethod] to the buffer.
     */
    fun writeToBuffer(buffer: RegistryFriendlyByteBuf)

    companion object {
        /**
         * Deserializes an [ObtainMethod] from the buffer.
         */
        fun readFromBuffer(buffer: RegistryFriendlyByteBuf): ObtainMethod {
            val variant = buffer.readUtf()
            return when (variant) {
                "cobblemon:impossible" -> ImpossibleObtainMethod()
                "cobblemon:none" -> NoneObtainMethod()
                "cobblemon:advancement" -> {
                    val advancement = buffer.readResourceLocation()
                    PlayerHasAdvancementObtainMethod(advancement)
                }
                "cobblemon:y_level" -> {
                    val yLevel = buffer.readVarInt()
                    val operator = buffer.readUtf()
                    PlayerYObtainMethod(yLevel, operator)
                }
                "cobblemon:pokemon_knows" -> {
                    val moveId = buffer.readUtf()
                    PokemonHasMoveObtainMethod(moveId)
                }
                else -> throw IllegalArgumentException("Unknown ObtainMethod variant: $variant")
            }
        }
    }
}
