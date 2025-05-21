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
import com.cobblemon.mod.common.block.TMShelfBlock
import com.cobblemon.mod.common.item.TechnicalMachineItem
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.ContainerHelper
import net.minecraft.world.Containers
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.level.gameevent.GameEvent
import java.util.OptionalInt

class TMShelfBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(CobblemonBlockEntities.TM_SHELF, pos, state) {
    val items: NonNullList<ItemStack> = NonNullList.withSize(14, ItemStack.EMPTY)
    var lastInteractedSlot: Int = -1

    fun handleUseItem(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): ItemInteractionResult {
        if (stack.isEmpty) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        val slot = getHitSlot(hit, state).orElse(-1)
        if (slot !in 0 until 14) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
        if (!isValidItem(stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION

        val oldStack = items[slot]
        if (!oldStack.isEmpty) {
            if (!player.addItem(oldStack)) {
                Containers.dropItemStack(level, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, oldStack)
            }
        }

        items[slot] = stack.copyWithCount(1)
        stack.shrink(1)
        lastInteractedSlot = slot
        updateBlockState(level, pos)
        markUpdated()

        return ItemInteractionResult.sidedSuccess(level.isClientSide)
    }

    fun handleUseWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult): InteractionResult {
        val slot = getHitSlot(hit, state).orElse(-1)
        if (slot !in 0 until 14 || items[slot].isEmpty) return InteractionResult.PASS

        val removed = items[slot]
        items[slot] = ItemStack.EMPTY
        if (!player.addItem(removed)) {
            Containers.dropItemStack(level, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, removed)
        }

        lastInteractedSlot = slot
        updateBlockState(level, pos)

        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    private fun updateBlockState(level: Level, pos: BlockPos) {
        val newState = TMShelfBlock.SLOT_OCCUPIED_PROPERTIES.fold(blockState) { acc, prop ->
            val index = TMShelfBlock.SLOT_OCCUPIED_PROPERTIES.indexOf(prop)
            acc.setValue(prop, !items[index].isEmpty)
        }
        level.setBlock(pos, newState, 3)
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(newState))
    }

    private fun getHitSlot(hit: BlockHitResult, state: BlockState): OptionalInt {
        val facing = state.getValue(TMShelfBlock.FACING)
        val relative = hit.location.subtract(hit.blockPos.x.toDouble(), hit.blockPos.y.toDouble(), hit.blockPos.z.toDouble())

        val (x, y) = when (facing) {
            Direction.NORTH -> 1.0 - relative.x to relative.y
            Direction.SOUTH -> relative.x to relative.y
            Direction.WEST  ->  1.0 - relative.z to relative.y
            Direction.EAST  -> relative.z to relative.y
            else -> return OptionalInt.empty()
        }

        val col = if (x < 0.5) 1 else 0
        val row = ((1.0 - y) * 7).toInt().coerceIn(0, 6)

        return OptionalInt.of(row * 2 + col)
    }

    fun isValidItem(stack: ItemStack): Boolean {
        return stack.item is TechnicalMachineItem
                || stack.item == CobblemonItems.UPGRADE
                || stack.item == CobblemonItems.DUBIOUS_DISC
                || stack.item == Items.MUSIC_DISC_13
                || stack.item == Items.MUSIC_DISC_CAT
                || stack.item == Items.MUSIC_DISC_BLOCKS
                || stack.item == Items.MUSIC_DISC_CHIRP
                || stack.item == Items.MUSIC_DISC_FAR
                || stack.item == Items.MUSIC_DISC_MALL
                || stack.item == Items.MUSIC_DISC_MELLOHI
                || stack.item == Items.MUSIC_DISC_STAL
                || stack.item == Items.MUSIC_DISC_STRAD
                || stack.item == Items.MUSIC_DISC_WARD
                || stack.item == Items.MUSIC_DISC_11
                || stack.item == Items.MUSIC_DISC_WAIT
                || stack.item == Items.MUSIC_DISC_OTHERSIDE
                || stack.item == Items.MUSIC_DISC_5
                || stack.item == Items.MUSIC_DISC_RELIC
                || stack.item == Items.MUSIC_DISC_PRECIPICE
                || stack.item == Items.MUSIC_DISC_CREATOR
                || stack.item == Items.MUSIC_DISC_CREATOR_MUSIC_BOX
                || stack.item == Items.MUSIC_DISC_PIGSTEP
    }

    fun markUpdated() {
        level?.setBlock(blockPos, blockState, 3)
        level?.sendBlockUpdated(blockPos, blockState, blockState, 3)
        setChanged()
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return saveWithoutMetadata(registries)
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        ContainerHelper.saveAllItems(tag, items, true, registries)
        tag.putInt("LastInteractedSlot", lastInteractedSlot)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        items.clear()
        ContainerHelper.loadAllItems(tag, items, registries)
        lastInteractedSlot = tag.getInt("LastInteractedSlot")
    }

}