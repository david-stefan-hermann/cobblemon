package com.cobblemon.mod.common.api.storage.player.adapter

import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreTypes
import com.cobblemon.mod.common.api.tms.TMMoveManager
import java.util.UUID

class TMMoveNbtBackend : NbtBackedPlayerData<TMMoveManager>("tm_moves", PlayerInstancedDataStoreTypes.TM_MOVES) {
    override val codec = TMMoveManager.CODEC
    override val defaultData = TMMoveJsonBackend.defaultDataFunc
}