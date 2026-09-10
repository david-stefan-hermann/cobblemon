/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.net.data

import com.cobblemon.mod.common.api.net.ClientNetworkPacketHandler
import com.cobblemon.mod.common.net.messages.client.data.DataRegistrySyncPacket
import net.minecraft.client.Minecraft
import net.minecraft.network.RegistryFriendlyByteBuf

class DataRegistrySyncPacketHandler<P : Any, T : DataRegistrySyncPacket<P, T>> : ClientNetworkPacketHandler<T> {
    override fun handle(packet: T, client: Minecraft) {
        val buffer = requireNotNull(packet.buffer) { "Buffer missing on DataRegistrySyncPacket" }

        packet.entries.clear()
        // PT143: readList<T> requires non-null T — wrap decodeEntry result with non-null guard.
        packet.entries.addAll(buffer.readList<P> { buf ->
            packet.decodeEntry(buf as RegistryFriendlyByteBuf) ?: error("decodeEntry returned null in non-null collection")
        })
        buffer.release()
        packet.synchronizeDecoded(packet.entries)
    }
}