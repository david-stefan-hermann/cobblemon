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
        player.giveOrDropItemStack(stack, false)
        this.item = Items.AIR.defaultInstance
    }
}
