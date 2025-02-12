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
import com.cobblemon.mod.common.gui.TMMScreenHandler
import com.cobblemon.mod.common.net.messages.client.ui.CraftBlankTMPacket
import com.cobblemon.mod.common.util.playSoundServer
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.Items

object CraftBlankTMPacketHandler : ServerNetworkPacketHandler<CraftBlankTMPacket> {
    override fun handle(packet: CraftBlankTMPacket, server: MinecraftServer, player: ServerPlayer) {
        val screen = player.containerMenu as TMMScreenHandler
        if (screen.inventory == null) return
        val ingredientSlot = screen.inventory!!.getItem(2)

        if (ingredientSlot.`is`(Items.AMETHYST_SHARD) && ingredientSlot.count >= 1) {
            screen.inventory!!.removeItem(2, 1)
            screen.result.setItem(0, CobblemonItems.BLANK_TM.defaultInstance)

            screen.inventory!!.setChanged()
            screen.result.setChanged()
            player.containerMenu.broadcastChanges()

            player.level().playSoundServer(player.position(), CobblemonSounds.TMM_CRAFT_BLANK, SoundSource.BLOCKS)
        }
    }
}
