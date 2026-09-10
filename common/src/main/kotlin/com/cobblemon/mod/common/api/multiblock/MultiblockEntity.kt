/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.multiblock

import com.cobblemon.mod.common.util.getBlockPos
import com.cobblemon.mod.common.util.putBlockPos

import com.cobblemon.mod.common.api.multiblock.builder.MultiblockStructureBuilder
import com.cobblemon.mod.common.util.DataKeys
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/**
 * Multiblock entities are kind of complicated. Basically every multiblock entity should have a MultiBlockStructureBuilder
 * that is checked when this block entity is created. The multiblock entity also contains a reference to a MultiblockStructure
 * which is shared between all MultiblockEntities in the structure. Finally, every Multiblock contains the location of the
 * block that controls this blockentities associated structure
 */
abstract class MultiblockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState,
    var multiblockBuilder: MultiblockStructureBuilder?
) : BlockEntity(type, pos, state) {

    abstract var multiblockStructure: MultiblockStructure?
    abstract var masterBlockPos: BlockPos?

    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(registryLookup: HolderLookup.Provider): CompoundTag {
        return saveWithoutMetadata(registryLookup)
    }

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        // TODO PT134-DEFER: multiblock structure NBT migration to ValueOutput — needs writeToValueOutput
        output.putBoolean(DataKeys.FORMED, masterBlockPos != null)
    }

    override fun isValidBlockState(blockState: BlockState) = blockState.block is MultiblockBlock

    // PT137: was `abstract override fun loadAdditional` — abstract members can't be called via super,
    // so subclasses calling super.loadAdditional failed. Concrete body delegates to BlockEntity.
    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
    }

}
