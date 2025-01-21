/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.net.gui

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.net.ServerNetworkPacketHandler
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.gui.TMMScreenHandler
import com.cobblemon.mod.common.item.TechnicalMachineItem
import com.cobblemon.mod.common.item.components.TMMoveComponent
import com.cobblemon.mod.common.net.messages.client.ui.CraftTMPacket
import com.cobblemon.mod.common.util.itemRegistry
import com.cobblemon.mod.common.util.playSoundServer
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.ItemStack

object CraftTMPacketHandler : ServerNetworkPacketHandler<CraftTMPacket> {
    override fun handle(packet: CraftTMPacket, server: MinecraftServer, player: ServerPlayer) {
        val screen = player.containerMenu as TMMScreenHandler
        val discSlot = screen.input.getItem(0)
        val gemSlot = screen.input.getItem(1)
        val ingredientSlot = screen.input.getItem(2)
        val outputSlot = screen.result.getItem(0)
        val typeGem = player.serverLevel().itemRegistry.get(ElementalTypes.get(packet.tm.type)?.typeGem)

        if (!outputSlot.isEmpty) {
            return
        }

        if (discSlot != null && discSlot.item != CobblemonItems.BLANK_TM) {
            return
        }

        if (gemSlot != null && gemSlot.item != typeGem) {
            return
        }

        if (packet.tm.recipe != null && ingredientSlot != null) {
            if (!player.serverLevel().itemRegistry.get(packet.tm.recipe.item)?.let { ingredientSlot.`is`(it) }!! || ingredientSlot.count < packet.tm.recipe.count) {
                return
            }
        }

        val stack = ItemStack(CobblemonItems.TECHNICAL_MACHINE)
        val moveTemplate = packet.tm.moveName
        TMMoveComponent.setTMMove(stack, moveTemplate)
        screen.result.setItem(0, stack)
        screen.input.getItem(0).shrink(1)
        screen.input.getItem(1).shrink(1)
        if (packet.tm.recipe != null) {
            screen.input.getItem(2).shrink(packet.tm.recipe.count)
        }

        screen.input.setChanged()
        screen.result.setChanged()
        player.containerMenu.broadcastChanges()

        player.serverLevel().playSoundServer(player.position(), CobblemonSounds.TMM_CRAFT, SoundSource.BLOCKS)
    }
}