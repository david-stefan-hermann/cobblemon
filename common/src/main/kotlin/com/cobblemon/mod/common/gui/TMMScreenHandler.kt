/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.gui

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.block.entity.TMBlockEntity
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.*
import net.minecraft.world.item.ItemStack

class TMMScreenHandler(menuType: MenuType<*>, syncId: Int) : AbstractContainerMenu(menuType, syncId) {
    var playerInventory: Inventory? = null
    val result = ResultContainer()
    private var tmmEntity: TMBlockEntity? = null
    var inventory: Container? = null

    constructor(syncId: Int, playerInventory: Inventory, inventory: Container, blockEntity: TMBlockEntity?) : this(CobblemonMenuHandlers.TMM_SCREEN, syncId) {
        this.playerInventory = playerInventory
        this.inventory = inventory
        this.tmmEntity = blockEntity

        inventory.startOpen(playerInventory.player)

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

        this.addSlot(Slot(inventory, 0, startX + 167, startY + 9))  // Input slot 1
        this.addSlot(Slot(inventory, 1, startX + 185, startY + 9))  // Input slot 2
        this.addSlot(Slot(inventory, 2, startX + 203, startY + 9))  // Input slot 3

        this.addSlot(TMResultSlot(result, 0, startX + 123, startY - 22))
        //this.addSlot(ResultSlot(
        //    playerInventory.player,
        //    input,
        //    result,
        //    0,
        //    startX + 123,
        //    startY - 22
        //))
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
        val resultItem = result.getItem(0)
        if (!resultItem.isEmpty) { //Ensure player result item is given to player or dropped on
            if (player.isAlive && (player as? ServerPlayer)?.hasDisconnected() != true) {
                player.inventory.placeItemBackInInventory(resultItem)
            }
            else {
                player.drop(resultItem, false)
            }
        }

        super.removed(player)
        tmmEntity?.level?.setBlock(tmmEntity!!.blockPos, tmmEntity!!.blockState, 3)
        inventory?.stopOpen(player)
    }

    fun syncState() {
        broadcastChanges()
    }

    companion object {
        const val SLOT_COUNT = 3
    }

    class TMResultSlot(container: Container, slot: Int, x: Int, y: Int) : Slot(container, slot, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean { return false }
    }
}
