package com.cobblemon.mod.common.api.tms

import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreType
import com.cobblemon.mod.common.api.storage.player.client.ClientInstancedPlayerData
import com.cobblemon.mod.common.net.messages.client.SetClientPlayerDataPacket
import com.cobblemon.mod.common.CobblemonNetwork.sendPacket
import com.cobblemon.mod.common.util.getPlayer
import net.minecraft.resources.ResourceLocation
import java.util.*

abstract class AbstractTMMoveManager {
    abstract val uuid: UUID
    abstract val learnedTMs: MutableSet<ResourceLocation>
    abstract val storeType: PlayerInstancedDataStoreType
    abstract fun toClientData(): ClientInstancedPlayerData
    abstract fun toClientDataFrom(set: Set<ResourceLocation>): ClientInstancedPlayerData

    open fun learn(tmId: ResourceLocation): Boolean {
        if (learnedTMs.add(tmId)) {
            syncClient(setOf(tmId))
            return true
        }
        return false
    }

    protected fun syncClient(updateSet: Set<ResourceLocation> = learnedTMs) {
        uuid.getPlayer()?.sendPacket(
            SetClientPlayerDataPacket(
                type = storeType,
                playerData = toClientDataFrom(updateSet),
                isIncremental = true
            )
        )
    }

    open fun markDirty() {}
}