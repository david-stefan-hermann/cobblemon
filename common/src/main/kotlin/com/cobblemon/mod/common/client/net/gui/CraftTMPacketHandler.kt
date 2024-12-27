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
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory

object CraftTMPacketHandler : ServerNetworkPacketHandler<CraftTMPacket> {
    override fun handle(packet: CraftTMPacket, server: MinecraftServer, player: ServerPlayerEntity) {
        val screen = player.currentScreenHandler as TMMScreenHandler
        val discSlot = screen.inventory?.getStack(0)
        val gemSlot = screen.inventory?.getStack(1)
        val ingredientSlot = screen.inventory?.getStack(2)
        val outputSlot = screen.result.getStack(0)
        val typeGem = player.world.itemRegistry.get(ElementalTypes.get(packet.tm.type)?.typeGem)

        if (!outputSlot.isEmpty) {
            return
        }

        if (discSlot != null && !discSlot.isOf(CobblemonItems.BLANK_TM)) {
            return
        }

        if (gemSlot != null && !gemSlot.isOf(typeGem)) {
            return
        }

        if (packet.tm.recipe != null && ingredientSlot != null) {
            if (!ingredientSlot.isOf(player.world.itemRegistry.get(packet.tm.recipe.item)) || ingredientSlot.count < packet.tm.recipe.count) {
                return
            }
        }

        val stack = ItemStack(CobblemonItems.TECHNICAL_MACHINE)
        val moveTemplate = packet.tm.move
        TMMoveComponent.setTMMove(stack, moveTemplate)
        screen.result.setStack(0, stack)
        screen.inventory?.removeStack(0, 1)
        screen.inventory?.removeStack(1, 1)
        if (packet.tm.recipe != null) {
            screen.inventory?.removeStack(2, packet.tm.recipe.count)
        }

        screen.input.markDirty()
        screen.result.markDirty()
        player.currentScreenHandler.syncState()

        player.world.playSoundServer(player.pos, CobblemonSounds.TMM_CRAFT, SoundCategory.BLOCKS)
    }
}