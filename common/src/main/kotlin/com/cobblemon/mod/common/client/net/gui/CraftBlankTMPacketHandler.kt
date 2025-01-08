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
        val ingredientSlot = screen.input.getItem(2)

        if (ingredientSlot.`is`(Items.AMETHYST_SHARD) && ingredientSlot.count >= 1) {
            screen.input.removeItem(2, 1)
            screen.result.setItem(0, CobblemonItems.BLANK_TM.defaultInstance)

            screen.input.setChanged()
            screen.result.setChanged()
            player.containerMenu.broadcastChanges()

            player.level().playSoundServer(player.position(), CobblemonSounds.TMM_CRAFT_BLANK, SoundSource.BLOCKS)
        }
    }
}
