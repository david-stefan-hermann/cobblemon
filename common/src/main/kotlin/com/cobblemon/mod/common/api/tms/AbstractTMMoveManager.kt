package com.cobblemon.mod.common.api.tms

import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreType
import com.cobblemon.mod.common.api.storage.player.client.ClientInstancedPlayerData
import com.cobblemon.mod.common.net.messages.client.SetClientPlayerDataPacket
import com.cobblemon.mod.common.CobblemonNetwork.sendPacket
import com.cobblemon.mod.common.net.messages.client.toast.ToastPacket
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.getPlayer
import net.minecraft.network.chat.Component
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
            sendTMToast(tmId)
            return true
        }
        return false
    }

    private fun sendTMToast(tmId: ResourceLocation) {
        val player = uuid.getPlayer() ?: return
        val tm = TechnicalMachines.tmMap[tmId] ?: return

        val packet = ToastPacket(
                title = Component.literal("New TM Learned"),
                description = Component.literal(tm.moveName.name ?: tm.id.toString()),
                icon = tm.createItemStack(),
                frameTexture = ResourceLocation.parse("minecraft:toast/advancement"),
                progress = -1F,
                progressColor = 0x00FF00,
                uuid = UUID.nameUUIDFromBytes(("tm_toast:${tmId}").toByteArray()),
                behaviour = ToastPacket.Behaviour.SHOW_OR_UPDATE
        )

        player.sendPacket(packet)
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