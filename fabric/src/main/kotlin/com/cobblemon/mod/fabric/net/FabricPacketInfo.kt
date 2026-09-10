/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric.net

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.net.PacketRegisterInfo

/**
 * PT150: fabric-api networking v1 (PayloadTypeRegistry, ClientPlayNetworking, ServerPlayNetworking)
 * not yet remapped for MC 26.1.x. Stubbed pending upstream port. Calls become no-ops so build can complete.
 * Reintroduce real packet registration in PT15X+.
 */
class FabricPacketInfo<T : NetworkPacket<T>>(val info: PacketRegisterInfo<T>) {
    fun registerPacket(client: Boolean) {}
    fun registerClientHandler() {}
    fun registerServerHandler() {}
}
