package com.cobblemon.mod.common.gui

import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.StackedContents
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.inventory.StackedContentsCompatible

class TMMCraftingContainer(
    private val menu: TMMScreenHandler, // Link to the parent screen handler
    private val width: Int,
    private val height: Int
) : CraftingContainer, StackedContentsCompatible {

    private val items = MutableList(width * height) { ItemStack.EMPTY }

    override fun getWidth(): Int = width
    override fun getHeight(): Int = height
    override fun getItems(): List<ItemStack> = items

    override fun getContainerSize(): Int = items.size

    override fun getItem(slot: Int): ItemStack = if (slot in items.indices) items[slot] else ItemStack.EMPTY
    override fun setItem(slot: Int, stack: ItemStack) {
        if (slot in items.indices) items[slot] = stack
    }

    override fun setChanged() {
        // Notify the screen handler about the change
        menu.slotsChanged(this)
    }

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        if (slot in items.indices) {
            val item = items[slot].split(amount)
            if (items[slot].isEmpty) items[slot] = ItemStack.EMPTY
            return item
        }
        return ItemStack.EMPTY
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack {
        if (slot in items.indices) {
            val item = items[slot]
            items[slot] = ItemStack.EMPTY
            return item
        }
        return ItemStack.EMPTY
    }

    override fun stillValid(player: Player): Boolean = true
    override fun clearContent() {
        items.fill(ItemStack.EMPTY)
    }

    override fun isEmpty(): Boolean = items.all { it.isEmpty }
    override fun fillStackedContents(contents: StackedContents) {
        // Populates the given StackedContents with the items in this container
        for (item in items) {
            contents.accountStack(item)
        }
    }
}
