/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.storage.player.adapter

import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreTypes
import com.cobblemon.mod.common.api.storage.player.SpeciesLevelManager
import com.cobblemon.mod.common.util.adapters.CodecBackedAdapter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mongodb.client.MongoClient
import java.util.UUID

class SpeciesLevelMongoBackend(
    mongoClient: MongoClient,
    databaseName: String,
    collectionName: String
) : MongoBackedPlayerDataStoreBackend<SpeciesLevelManager>(
    mongoClient,
    databaseName,
    collectionName,
    PlayerInstancedDataStoreTypes.SPECIES_LEVELS
) {
    override val gson = GsonBuilder()
        .registerTypeAdapter(SpeciesLevelManager::class.java, CodecBackedAdapter(SpeciesLevelManager.CODEC))
        .create()
    override val classToken = TypeToken.get(SpeciesLevelManager::class.java)
    override val defaultData = defaultDataFunc

    override fun initialize(store: SpeciesLevelManager) {
        store.initialize()
    }

    companion object {
        val defaultDataFunc = { uuid: UUID -> SpeciesLevelManager(uuid) }
    }
}
