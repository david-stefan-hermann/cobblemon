package com.cobblemon.mod.common.block.entity

import com.cobblemon.mod.common.CobblemonBlockEntities
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.tms.TechnicalMachine
import com.cobblemon.mod.common.api.tms.TechnicalMachines
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.block.TMBlock
import com.cobblemon.mod.common.gui.TMMScreenHandler
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.RegistryAccess
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.ContainerHelper
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.nbt.CompoundTag

class TMBlockEntity(pos: BlockPos, state: BlockState) : BaseContainerBlockEntity(CobblemonBlockEntities.TM_BLOCK, pos, state) {

    var tmmInventory = TMBlockInventory(this)
    var automationDelay: Int = AUTOMATION_DELAY

    companion object {
        const val AUTOMATION_DELAY = 4
        const val FILTER_TM_NBT = "FilterTM"
    }

    override fun createMenu(containerId: Int, inventory: Inventory, player: Player): AbstractContainerMenu {
        return TMMScreenHandler(containerId, inventory, this.tmmInventory, this)
    }

    override fun saveAdditional(compound: CompoundTag) {
        super.saveAdditional(compound)
        ContainerHelper.saveAllItems(compound, tmmInventory.items)
        tmmInventory.filterTM?.let { compound.putString(FILTER_TM_NBT, it.name) }
    }

    override fun load(compound: CompoundTag) {
        super.load(compound)
        ContainerHelper.loadAllItems(compound, tmmInventory.items)
        tmmInventory.filterTM = compound.getString(FILTER_TM_NBT)?.let { Moves.getByName(it) }
    }

    override fun getDisplayName(): Component {
        return Component.translatable("container.brewing")
    }

    override fun toClientTag(tag: CompoundTag): CompoundTag {
        saveAdditional(tag)
        return tag
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getContainerSize(): Int {
        return tmmInventory.size
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

    override fun stillValid(player: Player): Boolean {
        return tmmInventory.stillValid(player)
    }

    class TMBlockInventory(private val blockEntity: TMBlockEntity) : SimpleContainer(4) {

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
