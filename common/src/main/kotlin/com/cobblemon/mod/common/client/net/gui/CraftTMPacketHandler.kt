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
import com.cobblemon.mod.common.api.net.ServerNetworkPacketHandler
import com.cobblemon.mod.common.block.tm.TMMScreenHandler
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
        val screen = player.containerMenu as? TMMScreenHandler ?: return
        val inventory = screen.inventory ?: return

        val outputSlot = screen.result.getItem(0)
        if (!outputSlot.isEmpty) return

        val discSlot = inventory.getItem(0)
        if (discSlot.item != CobblemonItems.BLANK_TM) return

        val recipes = packet.tm.recipe ?: emptyList()
        for ((index, recipe) in recipes.withIndex()) {
            val slot = 1 + index
            val item = inventory.getItem(slot)
            val expected = player.serverLevel().itemRegistry.get(recipe.item) ?: return
            if (!item.`is`(expected) || item.count < recipe.count) return
        }

        // Craft the TM
        val stack = ItemStack(CobblemonItems.TECHNICAL_MACHINE)
        TMMoveComponent.setTMMove(stack, packet.tm.moveName)
        screen.result.setItem(0, stack)

        // Consume ingredients
        inventory.getItem(0).shrink(1)
        for ((index, recipe) in recipes.withIndex()) {
            inventory.getItem(1 + index).shrink(recipe.count)
        }

        inventory.setChanged()
        screen.result.setChanged()
        player.containerMenu.broadcastChanges()

        player.serverLevel().playSoundServer(player.position(), CobblemonSounds.TMM_CRAFT, SoundSource.BLOCKS)
    }
}