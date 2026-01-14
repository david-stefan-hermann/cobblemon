/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.tms

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.data.JsonDataRegistry
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.moves.adapters.MoveTemplateAdapter
import com.cobblemon.mod.common.api.reactive.SimpleObservable
import com.cobblemon.mod.common.registry.ItemTagCondition
import com.cobblemon.mod.common.util.adapters.CobblemonObtainMethodAdapter
import com.cobblemon.mod.common.util.adapters.IdentifierAdapter
import com.cobblemon.mod.common.util.cobblemonResource
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.server.level.ServerPlayer
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import java.io.BufferedReader
import java.util.concurrent.ExecutionException

object TechnicalMachines : JsonDataRegistry<TechnicalMachine> {
    override val gson = GsonBuilder()
            .registerTypeAdapter(ResourceLocation::class.java, IdentifierAdapter)
            .registerTypeAdapter(ObtainMethod::class.java, CobblemonObtainMethodAdapter)
            .registerTypeAdapter(MoveTemplate::class.java, MoveTemplateAdapter)
            .create()

    override val typeToken = TypeToken.get(TechnicalMachine::class.java)
    override val resourcePath = "tms"
    override val id = cobblemonResource("technical_machines")
    override val type = PackType.SERVER_DATA
    override val observable = SimpleObservable<TechnicalMachines>()

    val tmMap = mutableMapOf<ResourceLocation, TechnicalMachine>()
    val moveToTM = mutableMapOf<MoveTemplate, TechnicalMachine>()
    val tagMap = mutableMapOf<ItemTagCondition, TechnicalMachine>()
    val passiveTms = mutableMapOf<ResourceLocation, TechnicalMachine>()

    /**
     * Overridden to deserialize a DTO first and convert it to a runtime
     * [TechnicalMachine], since Gson cannot deserialize Minecraft Ingredients
     * directly.
     */
    override fun parse(
        reader: BufferedReader,
        identifier: ResourceLocation
    ): TechnicalMachine {
        try {
            val technicalMachineDto = gson.fromJson(reader, TechnicalMachineDTO::class.java)
            val technicalMachine = technicalMachineDto.toTechnicalMachine()
            return technicalMachine
        } catch (exception: Exception) {
            throw ExecutionException(
                "Error loading Technical Machine JSON: $identifier",
                exception
            )
        }
    }

    override fun reload(data: Map<ResourceLocation, TechnicalMachine>) {
        data.forEach { (id, tm) ->
            //TODO remove the logging
            Cobblemon.LOGGER.info("Processing TM: $id with moveName: ${tm.moveName}")
            if (tm.moveName == null) {
                Cobblemon.LOGGER.error("Failed to resolve move for TM: $id with moveName: ${tm.moveName}")
            } else {
                Cobblemon.LOGGER.info("Resolved Move for TM: $id -> ${tm.moveName.name}")
            }
            tmMap[id] = tm
            tm.id = id

            moveToTM[tm.moveName] = tm

            // Check for passive ObtainMethods
            //if (tm.obtainMethods.any { it.passive }) passiveTms[id] = tm
        }
    }

    fun getByResourceLocation(resourceLocation: ResourceLocation): TechnicalMachine? {
        return tmMap[resourceLocation]
    }

    fun getAllResourceLocations(): Set<ResourceLocation> {
        return tmMap.keys.toSet()
    }

    // TODO we'll probably have to sync this
    override fun sync(player: ServerPlayer) { }

    // TODO delete this?
    fun checkPassives(player: ServerPlayer) {
        val playerTms = Cobblemon.playerDataManager.getGenericData(player).tmSet
        passiveTms.forEach { (id, tm) ->
            if (tm.obtainMethods.all { it.matches(player) } && !playerTms.contains(id)) tm.unlock(player)
        }
    }
}
