/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.storage.player.client

import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreTypes
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.net.messages.client.SetClientPlayerDataPacket
import com.cobblemon.mod.common.util.readIdentifier
import com.cobblemon.mod.common.util.writeIdentifier
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.resources.ResourceLocation

class ClientSpeciesLevelManager(
    val speciesLevels: MutableMap<ResourceLocation, Int>
) : ClientInstancedPlayerData {

    override fun encode(buf: RegistryFriendlyByteBuf) {
        buf.writeMap(
            speciesLevels,
            { _, key -> buf.writeIdentifier(key) },
            { _, value -> buf.writeInt(value) }
        )
    }

    companion object {
        fun decode(buf: RegistryFriendlyByteBuf): SetClientPlayerDataPacket {
            val levels = buf.readMap(
                { buf.readIdentifier() },
                { buf.readInt() }
            )
            return SetClientPlayerDataPacket(
                PlayerInstancedDataStoreTypes.SPECIES_LEVELS,
                ClientSpeciesLevelManager(levels.toMutableMap())
            )
        }

        fun runAction(data: ClientInstancedPlayerData) {
            if (data !is ClientSpeciesLevelManager) return
            CobblemonClient.clientSpeciesLevelData = data
        }

        fun runIncremental(data: ClientInstancedPlayerData) {
            if (data !is ClientSpeciesLevelManager) return
            CobblemonClient.clientSpeciesLevelData.speciesLevels.putAll(data.speciesLevels)
        }
    }
}
