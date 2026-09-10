/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric.permission

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.permission.Permission
import com.cobblemon.mod.common.api.permission.PermissionValidator
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer

/**
 * PT150: fabric-permissions-api 0.3.1 not yet remapped for MC 26.1.x AND
 * ServerPlayer.hasPermissions / CommandSourceStack.hasPermission are not resolvable in this Loom
 * fabric module classpath (B173 confirmed). Fallback to deny-all to allow Fabric jar to compile.
 * Reintroduce real permission checks in PT15X+ once fabric-permissions-api ships a 26.1.x build.
 */
class FabricPermissionValidator : PermissionValidator {
    override fun initialize() {
        Cobblemon.LOGGER.info("Booting FabricPermissionValidator (PT150 deny-all stub — fabric-permissions-api not yet ported)")
    }

    override fun hasPermission(player: ServerPlayer, permission: Permission): Boolean = false

    override fun hasPermission(source: CommandSourceStack, permission: Permission): Boolean = false

    override fun hasPermission(player: ServerPlayer, permission: String, level: Int): Boolean = false

    override fun hasPermission(source: CommandSourceStack, permission: String, level: Int): Boolean = false
}
