package com.cobblemon.mod.common.api.storage.player.adapter

import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreTypes
import com.cobblemon.mod.common.api.tms.TMMoveManager
import com.cobblemon.mod.common.util.adapters.CodecBackedAdapter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mongodb.client.MongoClient
import java.util.UUID

class TMMoveMongoBackend(
    mongoClient: MongoClient,
    databaseName: String,
    collectionName: String
) : MongoBackedPlayerDataStoreBackend<TMMoveManager>(
    mongoClient,
    databaseName,
    collectionName,
    PlayerInstancedDataStoreTypes.TM_MOVES
) {
    override val gson = GsonBuilder()
        .registerTypeAdapter(TMMoveManager::class.java, CodecBackedAdapter(TMMoveManager.CODEC))
        .create()
    override val classToken = TypeToken.get(TMMoveManager::class.java)
    override val defaultData = defaultDataFunc

    override fun initialize(store: TMMoveManager) {
        store.initialize()
    }

    companion object {
        val defaultDataFunc = { uuid: UUID -> TMMoveManager(uuid) }
    }
}