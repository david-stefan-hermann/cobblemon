/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.tms

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.data.JsonDataRegistry
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.moves.adapters.MoveTemplateAdapter
import com.cobblemon.mod.common.api.reactive.SimpleObservable
import com.cobblemon.mod.common.item.TechnicalMachineItem
import com.cobblemon.mod.common.item.components.TMMoveComponent
import com.cobblemon.mod.common.registry.ItemTagCondition
import com.cobblemon.mod.common.util.adapters.CobblemonObtainMethodAdapter
import com.cobblemon.mod.common.util.adapters.IdentifierAdapter
import com.cobblemon.mod.common.util.cobblemonResource
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.world.item.ItemStack
import net.minecraft.server.level.ServerPlayer
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType

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
    val moveToTMs = mutableMapOf<MoveTemplate, MutableList<TechnicalMachine>>()
    val tagMap = mutableMapOf<ItemTagCondition, TechnicalMachine>()
    val passiveTms = mutableMapOf<ResourceLocation, TechnicalMachine>()

    override fun reload(data: Map<ResourceLocation, TechnicalMachine>) {
        data.forEach { (id, tm) ->
            tmMap[id] = tm
            tm.id = id
            moveToTMs.getOrPut(tm.move, ::ArrayList).add(tm)
            if (tm.obtainMethods.any { it.passive }) passiveTms[id] = tm
        }
    }

    override fun sync(player: ServerPlayer) { }

    fun checkPassives(player: ServerPlayer) {
        val playerTms = Cobblemon.playerDataManager.getGenericData(player).tmSet
        passiveTms.forEach { (id, tm) ->
            if (tm.obtainMethods.all { it.matches(player) } && !playerTms.contains(id)) tm.unlock(player)
        }
    }
}
