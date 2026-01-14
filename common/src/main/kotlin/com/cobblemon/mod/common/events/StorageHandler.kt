/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.events

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents.POKEMON_RELEASED_EVENT_POST
import com.cobblemon.mod.common.api.events.storage.ReleasePokemonEvent
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreTypes
import com.cobblemon.mod.common.util.getPlayer
import java.util.UUID

object StorageHandler : EventHandler {
    override fun registerListeners() {
        POKEMON_RELEASED_EVENT_POST.subscribe(Priority.NORMAL, ::onRelease)
    }

    fun onRelease(event: ReleasePokemonEvent.Post) {
        val player = UUID.fromString(event.pokemon.originalTrainer).getPlayer()
        if (player != null) {
            val playerData = Cobblemon.playerDataManager.getGenericData(player)
            playerData.tradedUUIDs.remove(event.pokemon.uuid)
            Cobblemon.playerDataManager.saveSingle(playerData, PlayerInstancedDataStoreTypes.GENERAL)
            playerData.sendToPlayer(player)
        }
    }
}