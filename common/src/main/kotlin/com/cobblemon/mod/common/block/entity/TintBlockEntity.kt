/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block.entity

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntTag
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity

interface TintBlockEntity {
    companion object {
        const val TINT = "tint"
    }

    var tint: Int?

    fun getTint(): Int = tint ?: 0xFFFFFF

    fun setTint(tintValue: Int) {
        val entity = this as? BlockEntity ?: return
        val level = entity.level ?: return

        tint = tintValue
        entity.setChanged()
        level.blockEntityChanged(entity.blockPos)
        level.sendBlockUpdated(entity.blockPos, entity.blockState, entity.blockState, Block.UPDATE_ALL)
    }

    fun saveTint(tag: CompoundTag) {
        tint?.let { tag.put(TINT, IntTag.valueOf(it)) }
    }

    fun loadTint(tag: CompoundTag) {
        if (tag.contains(TINT)) tint = tag.getInt(TINT)
    }
}
