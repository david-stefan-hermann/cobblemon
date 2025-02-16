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
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.tms.TechnicalMachines
import com.cobblemon.mod.common.block.TMBlock
import com.cobblemon.mod.common.gui.TMMScreenHandler
import com.cobblemon.mod.common.item.components.TMMoveComponent
import com.cobblemon.mod.common.util.itemRegistry
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.core.Position
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
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.Container
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.ContainerOpenersCounter
import net.minecraft.world.level.block.state.properties.BlockStateProperties

class TMBlockEntity(pos: BlockPos, state: BlockState) : BaseContainerBlockEntity(CobblemonBlockEntities.TM_BLOCK, pos, state) {

    var tmmInventory = TMBlockInventory(this)
    var automationDelay: Int = AUTOMATION_DELAY
    var partialTicks: Float = 0f

    private val stateManager: ContainerOpenersCounter = object : ContainerOpenersCounter() {
        override fun onOpen(level: Level, pos: BlockPos, state: BlockState) {
            level.setBlockAndUpdate(pos, state.setValue(TMBlock.ON, true))
            playSound(level, pos, state, CobblemonSounds.TMM_ON)
        }

        override fun onClose(level: Level, pos: BlockPos, state: BlockState) {
            level.setBlockAndUpdate(pos, state.setValue(TMBlock.ON, false))
            playSound(level, pos, state, CobblemonSounds.TMM_OFF)
        }

        override fun openerCountChanged(level: Level, pos: BlockPos, state: BlockState, count: Int, openCount: Int) {

        }

        override fun isOwnContainer(player: Player): Boolean {
            if (player.containerMenu is TMMScreenHandler) {
                val inventory = (player.containerMenu as TMMScreenHandler).inventory
                return inventory === this@TMBlockEntity
            }
            return false
        }

        fun playSound(world: Level, pos: BlockPos, state: BlockState, sound: SoundEvent) {
            var d = pos.x.toDouble() + 0.5
            val e = pos.y.toDouble() + 0.5
            var f = pos.z.toDouble() + 0.5
            val direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING)
            d += direction.stepX.toDouble() * 0.5
            f += direction.stepZ.toDouble() * 0.5
            world.playSound(
                null,
                d,
                e,
                f,
                sound,
                SoundSource.BLOCKS,
                0.5f,
                world.random.nextFloat() * 0.1f + 0.9f
            )
        }
    }

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

        //Update Neighbours
        level?.updateNeighborsAt(blockPos, blockState.block)
    }


    override fun stillValid(player: Player): Boolean {
        return tmmInventory.stillValid(player)
    }

    override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean {
        return tmmInventory.canPlaceItem(slot, stack)
    }

    override fun canTakeItem(target: Container, slot: Int, stack: ItemStack): Boolean { return false }

    override fun startOpen(player: Player) {
        if (!this.remove && !player.isSpectator) {
            stateManager.incrementOpeners(player, level!!, blockPos, blockState)
        }
    }

    override fun stopOpen(player: Player) {
        if (!this.remove && !player.isSpectator) {
            stateManager.decrementOpeners(player, level!!, blockPos, blockState)
        }
    }

    fun autocraftTM() {
        if (this.tmmInventory.filterTM != null) {
            if (this.tmmInventory.getItem(0).item != CobblemonItems.BLANK_TM) return

            if (this.tmmInventory.getItem(1).item != this.level?.itemRegistry?.get(this.tmmInventory.filterTM!!.elementalType.typeGem)) return

            val recipe = TechnicalMachines.moveToTM[this.tmmInventory.filterTM]?.recipe
            if (recipe != null) {
                if (this.tmmInventory.getItem(2).item != this.level?.itemRegistry?.get(recipe.item) || this.tmmInventory.getItem(2).count < recipe.count) return
            }

            val stack = ItemStack(CobblemonItems.TECHNICAL_MACHINE)
            TMMoveComponent.setTMMove(stack, this.tmmInventory.filterTM!!)

            this.tmmInventory.getItem(0).shrink(1)
            this.tmmInventory.getItem(1).shrink(1)
            if (recipe != null) {
                this.tmmInventory.getItem(2).shrink(recipe.count)
            }
            this.tmmInventory.setChanged()

            //TODO The machine is rendered backwards, could not figure how to fix that, this is temporary so it spits the item 'correct' direction
            val direction = this.blockState.getValue(TMBlock.FACING).opposite
            val position = this.blockPos.center.add(direction.stepX * 0.7, 0.1, direction.stepZ * 0.7)

            //TODO Add sound to be played then TM Machine autocrafts - I don't think playing the once that exist already is a good idea for autocraft
            this.ejectItem(stack, direction, position)
            tmmInventory.setChanged()
        }
    }

    fun ejectItem(stack: ItemStack, direction: Direction, position: Position) {
        val itemEntity = ItemEntity(this.level!!, position.x(), position.y() - 0.5, position.z(), stack)
        itemEntity.setDeltaMovement(direction.stepX * 0.05, 0.0, direction.stepZ * 0.05)
        this.level!!.addFreshEntity(itemEntity)
    }

    fun getAnalogOutputSignal() : Int {
        val filterTM = this.tmmInventory.filterTM ?: return 0

        if (this.tmmInventory.getItem(0).item != CobblemonItems.BLANK_TM) return 0

        if (this.tmmInventory.getItem(1).item != this.level?.itemRegistry?.get(filterTM.elementalType.typeGem)) return 0

        val recipe = TechnicalMachines.moveToTM[filterTM]?.recipe
        if (recipe != null) {
            if (this.tmmInventory.getItem(2).item != this.level?.itemRegistry?.get(recipe.item) || this.tmmInventory.getItem(2).count < recipe.count) return 0
        }

        return 15
    }

    class TMBlockInventory(val blockEntity: TMBlockEntity) : SimpleContainer(4) {
        var filterTM: MoveTemplate? = null

        override fun canPlaceItem(slot: Int, stack: ItemStack): Boolean {
            val blockState = blockEntity.blockState
            if (blockState.getValue(TMBlock.ON)) return false

            val filterTM = this.filterTM ?: return false
            val tm = TechnicalMachines.moveToTM[filterTM] ?: return false

            val item = stack.item
            return when (slot) {
                0 -> item == CobblemonItems.BLANK_TM
                1 -> item == blockEntity.level?.itemRegistry?.get(filterTM.elementalType.typeGem)
                2 -> item == blockEntity.level?.itemRegistry?.get(tm.recipe?.item)
                else -> false
            }
        }

        override fun setChanged() {
            blockEntity.level?.updateNeighborsAt(blockEntity.blockPos, blockEntity.blockState.block)
            super.setChanged()
        }

        override fun startOpen(player: Player) {
            blockEntity.startOpen(player)
        }

        override fun stopOpen(player: Player) {
            blockEntity.stopOpen(player)
        }
    }

}
