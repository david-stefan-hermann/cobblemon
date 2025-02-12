/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block.entity

import com.cobblemon.mod.common.CobblemonBlockEntities
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.tms.TechnicalMachines
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.block.TMBlock
import com.cobblemon.mod.common.gui.CobblemonMenuHandlers
import com.cobblemon.mod.common.gui.TMMScreenHandler
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.ContainerHelper
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.nbt.CompoundTag

class TMBlockEntity(pos: BlockPos, state: BlockState) : BaseContainerBlockEntity(CobblemonBlockEntities.TM_BLOCK, pos, state) {

    var tmmInventory = TMBlockInventory(this)
    var automationDelay: Int = AUTOMATION_DELAY
    var partialTicks: Float = 0f

    companion object {
        const val AUTOMATION_DELAY = 4
        const val FILTER_TM_NBT = "FilterTM"
    }

    override fun createMenu(containerId: Int, inventory: Inventory): AbstractContainerMenu {
        return TMMScreenHandler(containerId, inventory, this.tmmInventory, this)
    }

    override fun saveAdditional(compound: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(compound, registries)
        ContainerHelper.saveAllItems(compound, tmmInventory.items, registries)
        tmmInventory.filterTM?.let { compound.putString(FILTER_TM_NBT, it.name) }
    }

    override fun loadAdditional(compound: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(compound, registries)
        ContainerHelper.loadAllItems(compound, tmmInventory.items, registries)
        tmmInventory.filterTM = compound.getString(FILTER_TM_NBT)?.let { Moves.getByName(it) }
    }

    override fun getDisplayName(): Component {
        return Component.translatable("block.cobblemon.tm_block")
    }

    override fun getDefaultName(): Component {
        return Component.translatable("container.tm_block")
    }

    override fun getItems(): NonNullList<ItemStack> {
        return tmmInventory.items
    }

    override fun setItems(items: NonNullList<ItemStack>) {
        for (i in items.indices) {
            tmmInventory.setItem(i, items[i])
        }
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(registryLookup: HolderLookup.Provider): CompoundTag {
        return saveWithoutMetadata(registryLookup)
    }

    override fun getContainerSize(): Int {
        return tmmInventory.containerSize
    }

    override fun isEmpty(): Boolean {
        return tmmInventory.isEmpty
    }

    override fun getItem(slot: Int): ItemStack {
        return tmmInventory.getItem(slot)
    }

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        return tmmInventory.removeItem(slot, amount)
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack {
        return tmmInventory.removeItemNoUpdate(slot)
    }

    override fun setItem(slot: Int, stack: ItemStack) {
        tmmInventory.setItem(slot, stack)
    }

    override fun setChanged() {
        // Notify the block entity's level that this block entity has changed
        level?.blockEntityChanged(worldPosition)

        // Mark the chunk containing this block entity as dirty, ensuring it is saved
        level?.getChunkAt(worldPosition)?.setUnsaved(true)
    }


    override fun stillValid(player: Player): Boolean {
        return tmmInventory.stillValid(player)
    }

    class TMBlockInventory(val blockEntity: TMBlockEntity) : SimpleContainer(4) {
        var filterTM: MoveTemplate? = null

        override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean {
            val blockState = blockEntity.blockState
            if (blockState.getValue(TMBlock.ON)) return false

            val filterTM = this.filterTM
            val tms = filterTM?.let { TechnicalMachines.moveToTMs[it] } ?: return false

            return tms.any { tm ->
                val item = stack.item
                when (slot) {
                    0 -> item == CobblemonItems.BLANK_TM
                    1 -> item == ElementalTypes.get(tm.type)?.typeGem
                    2 -> item == tm.recipe?.item
                    else -> false
                }
            }
        }
    }

}
