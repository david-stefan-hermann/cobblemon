/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.tms

import com.cobblemon.mod.common.CobblemonNetwork.sendPacket
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreType
import com.cobblemon.mod.common.api.storage.player.client.ClientInstancedPlayerData
import com.cobblemon.mod.common.net.messages.client.SetClientPlayerDataPacket
import com.cobblemon.mod.common.net.messages.client.toast.ToastPacket
import com.cobblemon.mod.common.util.getPlayer
import com.cobblemon.mod.common.util.lang
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import java.util.*

abstract class AbstractTMMoveManager {
    abstract val uuid: UUID
    abstract val learnedTMs: MutableSet<ResourceLocation>
    abstract val storeType: PlayerInstancedDataStoreType
    abstract fun toClientData(): ClientInstancedPlayerData
    abstract fun toClientDataFrom(set: Set<ResourceLocation>): ClientInstancedPlayerData

    open fun learn(tmIds: Collection<ResourceLocation>): Boolean {
        val newLearnedTms = mutableListOf<ResourceLocation>()
        for (tmId in tmIds) {
            if (learnedTMs.add(tmId)) {
                newLearnedTms.add(tmId)
            }
        }

        if (newLearnedTms.isEmpty()) return false

        syncClient(newLearnedTms.toSet())
        sendTMToast(newLearnedTms)

        return true
    }

    open fun unlearn(tmIds: Collection<ResourceLocation>): Boolean {
        val removedTms = mutableListOf<ResourceLocation>()

        for (tmId in tmIds) {
            if (learnedTMs.remove(tmId)) {
                removedTms.add(tmId)
            }
        }

        if (removedTms.isEmpty()) return false

        syncClient(learnedTMs, isIncremental = false)
        markDirty()

        return true
    }

    private fun sendTMToast(tmIds: List<ResourceLocation>) {
        val player = uuid.getPlayer() ?: return
        var moveName: Component = Component.empty()
        val icons = tmIds.mapNotNull {
            val tm = TechnicalMachines.tmMap[it] ?: return@mapNotNull null
            moveName = tm.translatedMoveName()

            return@mapNotNull tm.createItemStack()
        }

        val description = if (icons.size == 1) moveName else lang("tms.check_tmm")

        val packet = ToastPacket(
                title = lang("tms.new_tms_learned"),
                description = description,
                icons = icons,
                frameTexture = ResourceLocation.parse("minecraft:toast/advancement"),
                progress = -1F,
                progressColor = 0x00FF00,
                // Keeping it a fixed UUID makes it so the ToastTracker merges different toasts on the client
                uuid = UUID.nameUUIDFromBytes(("tm_toast").toByteArray()),
                behaviour = ToastPacket.Behaviour.SHOW_OR_UPDATE,
                durationMs = 4000L
        )

        player.sendPacket(packet)
    }

    protected fun syncClient(updateSet: Set<ResourceLocation> = learnedTMs, isIncremental: Boolean = true) {
        uuid.getPlayer()?.sendPacket(
            SetClientPlayerDataPacket(
                type = storeType,
                playerData = toClientDataFrom(updateSet),
                isIncremental = isIncremental
            )
        )
    }

    open fun markDirty() {}
}