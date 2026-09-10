/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.net.messages.server.behaviour

import com.cobblemon.mod.common.api.net.NetworkPacket
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.world.phys.Vec3

class DamageOnCollisionPacket(
    val impactVec: Vec3
) : NetworkPacket<DamageOnCollisionPacket> {
    companion object {
        val ID = cobblemonResource("c2s_on_collision_damage")
        // PT136: FriendlyByteBuf.read/writeVec3 removed in MC 26.1.x — encode components manually
        fun decode(buffer: RegistryFriendlyByteBuf): DamageOnCollisionPacket = DamageOnCollisionPacket(
            impactVec = Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
        )
    }

    override val id = ID
    override fun encode(buffer: RegistryFriendlyByteBuf) {
        buffer.writeDouble(impactVec.x)
        buffer.writeDouble(impactVec.y)
        buffer.writeDouble(impactVec.z)
    }
}