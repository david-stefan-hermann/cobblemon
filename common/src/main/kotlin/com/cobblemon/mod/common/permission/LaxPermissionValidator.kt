/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.permission

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.permission.Permission
import com.cobblemon.mod.common.api.permission.PermissionValidator
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permission as MinecraftPermission
import net.minecraft.server.permissions.PermissionLevel as MinecraftPermissionLevel

/**
 * A [PermissionValidator] that uses the permission level vanilla system.
 * This is only used when the platform has no concept of permissions.
 */
class LaxPermissionValidator : PermissionValidator {

    override fun initialize() {
        Cobblemon.LOGGER.info("Booting LaxPermissionValidator, permissions will be checked using Minecrafts permission level system, see https://minecraft.wiki/w/Permission_level")
    }

    // PT143: Player.hasPermissions(int) removed in MC 26.1.x → use permissions().hasPermission(HasCommandLevel).
    override fun hasPermission(player: ServerPlayer, permission: Permission) = player.permissions().hasPermission(MinecraftPermission.HasCommandLevel(MinecraftPermissionLevel.byId(permission.level.numericalValue)))
    override fun hasPermission(source: CommandSourceStack, permission: Permission) = source.permissions().hasPermission(MinecraftPermission.HasCommandLevel(MinecraftPermissionLevel.byId(permission.level.numericalValue)))
    override fun hasPermission(player: ServerPlayer, permission: String, level: Int) = player.permissions().hasPermission(MinecraftPermission.HasCommandLevel(MinecraftPermissionLevel.byId(level)))
    override fun hasPermission(source: CommandSourceStack, permission: String, level: Int) = source.permissions().hasPermission(MinecraftPermission.HasCommandLevel(MinecraftPermissionLevel.byId(level)))
}