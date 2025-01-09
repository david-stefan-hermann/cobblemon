package com.cobblemon.mod.common.gui

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.block.entity.TMBlockEntity
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.*
import net.minecraft.world.item.ItemStack

class TMMScreenHandler(menuType: MenuType<*>, syncId: Int) : AbstractContainerMenu(menuType, syncId) {
    var playerInventory: Inventory? = null
    val input = TMMCraftingContainer(this, 3, 1)
    val result = ResultContainer()
    private var tmmEntity: TMBlockEntity? = null
    var inventory: Container? = null

    constructor(syncId: Int, playerInventory: Inventory, inventory: Container, blockEntity: TMBlockEntity?) : this(CobblemonMenuHandlers.TMM_SCREEN, syncId) {
        this.playerInventory = playerInventory
        this.inventory = inventory
        this.tmmEntity = blockEntity

        val startX = 0 - 9
        val startY = 112
        val slotWidth = 18
        val slotHeight = 18

        for (row in 0..2) {
            for (col in 0..8) {
                this.addSlot(Slot(playerInventory, 9 + (row * 9) + col, startX + (slotWidth * col), startY + (slotHeight * row)))
            }
        }
        for (col in 0..8) {
            this.addSlot(Slot(playerInventory, col, startX + (slotWidth * col), startY + 58))
        }

        this.addSlot(ResultSlot(
            playerInventory.player,
            input,
            result,
            0,
            startX + 123,
            startY - 22
        ))

        this.addSlot(Slot(this.inventory, 0, startX + 167, startY + 9))  // Input slot 1
        this.addSlot(Slot(this.inventory, 1, startX + 185, startY + 9))  // Input slot 2
        this.addSlot(Slot(this.inventory, 2, startX + 203, startY + 9))  // Input slot 3
    }

    fun getTMEntity(): TMBlockEntity? {
        return tmmEntity
    }

    override fun quickMoveStack(player: Player, slot: Int): ItemStack {
        return if (moveItemStackTo(getSlot(slot).item, 0, SLOT_COUNT, true)) getSlot(slot).item else ItemStack.EMPTY
    }

    override fun stillValid(player: Player?): Boolean {
        return inventory?.stillValid(player) ?: false
    }

    override fun clicked(slotIndex: Int, button: Int, clickType: ClickType, player: Player) {
        val adjustedType = if (clickType == ClickType.THROW) ClickType.PICKUP else clickType
        if (slotIndex == 36) {
            this.inventory?.setItem(3, ItemStack.EMPTY)
        }
        super.clicked(slotIndex, button, adjustedType, player)
    }

    /*override fun removed(player: Player?) {
        super.removed(player)
        tmmEntity?.stateManager?.playerWillCloseContainer(player, tmmEntity!!.level, tmmEntity!!.blockPos, tmmEntity!!.blockState)
    }*/

    override fun removed(player: Player) {
        super.removed(player)
        tmmEntity?.level?.setBlock(tmmEntity!!.blockPos, tmmEntity!!.blockState, 3)
    }

    fun syncState() {
        // Synchronize the input container slots
        if (inventory is TMBlockEntity.TMBlockInventory) {
            val tmInventory = inventory as TMBlockEntity.TMBlockInventory
            for (i in 0 until input.containerSize) {
                input.setItem(i, tmInventory.getItem(i))
            }

            // Synchronize the result slot if there's a valid TM or blank TM
            if (!tmInventory.itemsList[3].isEmpty) {
                if (ItemStack.isSameItemSameComponents(tmInventory.itemsList[3], CobblemonItems.TECHNICAL_MACHINE.defaultInstance) ||
                    ItemStack.isSameItemSameComponents(tmInventory.itemsList[3], CobblemonItems.BLANK_TM.defaultInstance)
                ) {
                    result.setItem(0, tmInventory.itemsList[3])
                }
            } else {
                tmInventory.itemsList[3] = result.getItem(0)
            }
        }

        // Notify the client and server of changes
        broadcastChanges()
    }


    override fun broadcastChanges() {
        if (inventory is TMBlockEntity.TMBlockInventory) {
            input.setItem(0, (inventory as TMBlockEntity.TMBlockInventory).itemsList[0])
            input.setItem(1, (inventory as TMBlockEntity.TMBlockInventory).itemsList[1])
            input.setItem(2, (inventory as TMBlockEntity.TMBlockInventory).itemsList[2])

            if ((inventory as TMBlockEntity.TMBlockInventory).itemsList[3] != ItemStack.EMPTY) {
                if (ItemStack.isSameItemSameComponents((inventory as TMBlockEntity.TMBlockInventory).itemsList[3], CobblemonItems.TECHNICAL_MACHINE.defaultInstance) ||
                        ItemStack.isSameItemSameComponents((inventory as TMBlockEntity.TMBlockInventory).itemsList[3], CobblemonItems.BLANK_TM.defaultInstance)) {
                    result.setItem(0, (inventory as TMBlockEntity.TMBlockInventory).itemsList[3])
                }
            } else {
                (inventory as TMBlockEntity.TMBlockInventory).itemsList[3] = result.getItem(0)
            }
        }

        super.broadcastChanges()
    }

    companion object {
        const val SLOT_COUNT = 3
    }
}
