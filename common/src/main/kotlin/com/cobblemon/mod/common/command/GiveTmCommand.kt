/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.command

import com.cobblemon.mod.common.api.permission.CobblemonPermissions
import com.cobblemon.mod.common.command.argument.MoveArgumentType
import com.cobblemon.mod.common.item.components.TMMoveComponent
import com.cobblemon.mod.common.util.alias
import com.cobblemon.mod.common.util.permission
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

object GiveTmCommand {

    private const val NAME = "givetm"
    private const val ALIAS = "giveTM"
    private const val PLAYER = "player"
    private const val MOVE = "move"

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        val command = Commands.literal(NAME)
            .permission(CobblemonPermissions.TECHNICAL_MACHINE)
            .then(Commands.argument(MOVE, MoveArgumentType.move())
                .executes { ctx ->
                    execute(ctx, ctx.source.playerOrException)
                }
                .then(Commands.argument(PLAYER, EntityArgument.player())
                    .executes { ctx ->
                        execute(ctx, EntityArgument.getPlayer(ctx, PLAYER))
                    }
                )
            )

        val node = dispatcher.register(command)
        dispatcher.register(node.alias(ALIAS))
    }

    private fun execute(context: CommandContext<CommandSourceStack>, player: ServerPlayer): Int {
        val move = MoveArgumentType.getMove(context, MOVE)
        val stack = TMMoveComponent.createStack(move)
        if (!player.addItem(stack)) {
            player.drop(stack, false)
        }

        context.source.sendSuccess(
            { Component.literal("Gave TM ${move.displayName.string} to ${player.name.string}") },
            true
        )
        return Command.SINGLE_SUCCESS
    }
}
