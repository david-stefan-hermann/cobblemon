/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.tm

import com.cobblemon.mod.common.gui.TMMScreenHandler
import com.cobblemon.mod.common.util.giveOrDropItemStack
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class TMMOutputSlot(val screen: TMMScreenHandler, index: Int, x: Int, y: Int) : Slot(screen.playerInventory, index, x, y) {

    override fun mayPlace(stack: ItemStack?) = false

    override fun onTake(player: Player, stack: ItemStack) {
        // Give or drop the item stack to the player
        player.giveOrDropItemStack(stack, false)
        // Use the set method to clear the item in the slot
        this.set(ItemStack.EMPTY)
    }
}

