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
import net.minecraft.server.permissions.PermissionLevel as MinecraftPermissionLevel

/**
 * port/26.2: checks permissions through fabric-api's own permission API (fabric-permission-api-v1), which
 * replaced lucko's fabric-permissions-api v0. Without a permission mod it answers with the vanilla
 * permission level, so this is used unconditionally.
 *
 * Nodes are identifiers now ([Permission.identifier], cobblemon:<node>). LuckPerms joins namespace and path
 * with a dot, so a node still reads cobblemon.<node> there - the same string [Permission.literal] was on v0.
 */
class FabricPermissionValidator : PermissionValidator {
    override fun initialize() {
        Cobblemon.LOGGER.info("Booting FabricPermissionValidator, permissions will be checked using fabric-permission-api-v1 (vanilla permission levels unless a permission mod is installed)")
    }

    override fun hasPermission(player: ServerPlayer, permission: Permission) =
        player.checkPermission(permission.identifier, MinecraftPermissionLevel.byId(permission.level.numericalValue))

    override fun hasPermission(source: CommandSourceStack, permission: Permission) =
        source.checkPermission(permission.identifier, MinecraftPermissionLevel.byId(permission.level.numericalValue))
}
