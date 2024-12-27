/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/..
 */

package com.cobblemon.mod.common.api.tms

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.tms.obtain.NoneObtainMethod
import com.cobblemon.mod.common.util.lang
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component

class TechnicalMachine(
        val move: MoveTemplate,
        val recipe: TechnicalMachineRecipe?,
        val obtainMethods: List<ObtainMethod> = emptyList(),
        val type: String
) {
    lateinit var id: ResourceLocation

    companion object {
        /**
         * Filters all available [TechnicalMachines] based on three optional arguments
         *
         * @param search A [String] to check against move names
         * @param type An [ElementalType] to check for
         * @param pokemon A [Pokemon] to check the [Learnset] of
         * @return A [MutableSet] of [TechnicalMachine] that passed the filter
         */
        fun filterTms(search: String?, type: ElementalType?, pokemon: Pokemon?): MutableSet<TechnicalMachine> {
            val tms = TechnicalMachines.tmMap.values.toMutableSet()

            type?.let {
                val iterator = tms.iterator()
                while (iterator.hasNext()) {
                    val tm = iterator.next()
                    if (ElementalTypes.get(tm.type) != type) {
                        iterator.remove()
                    }
                }
            }

            pokemon?.let {
                val iterator = tms.iterator()
                while (iterator.hasNext()) {
                    val tm = iterator.next()
                    if (!pokemon.species.moves.tmLearnableMoves().contains(tm.move)) {
                        iterator.remove()
                    }
                }
            }

            search?.let {
                val iterator = tms.iterator()
                while (iterator.hasNext()) {
                    val tm = iterator.next()
                    if (!tm.translatedMoveName().string.contains(search, ignoreCase = true)) {
                        iterator.remove()
                    }
                }
            }

            return tms
        }
    }

    /**
     * Unlocks this [TechnicalMachine] for the player.
     *
     * @param player The [ServerPlayer] to give this [TechnicalMachine] to.
     * @return Whether the player was successfully granted the [TechnicalMachine]
     */
    fun unlock(player: ServerPlayer): Boolean {
        Cobblemon.playerDataManager.getGenericData(player) // .get(player, ).tmSet.add(id)// .playerData.get(player).tmSet.add(id)
        if (!obtainMethods.any { it is NoneObtainMethod }) {
            player.sendSystemMessage(lang("tms.unlock_tm", move.displayName))
        }
        return true
    }

    /**
     * Returns a [MutableComponent] of the translated move name of this [TechnicalMachine]
     *
     * @return This [TechnicalMachine]'s move name, translated
     */
    fun translatedMoveName(): MutableComponent = move.displayName
}
