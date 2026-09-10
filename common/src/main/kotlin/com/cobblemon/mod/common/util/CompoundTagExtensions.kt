/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util

import com.cobblemon.mod.common.CobblemonEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import java.util.Optional
import java.util.UUID

fun CompoundTag.isPokemonEntity() : Boolean {
    return this.getStringOr("id", "").equals(CobblemonEntities.POKEMON_KEY.toString())
}

fun CompoundTag.getUUID(key: String): UUID = this.read(key, UUIDUtil.CODEC).orElse(UUID(0L, 0L))

fun CompoundTag.putUUID(key: String, value: UUID) { this.store(key, UUIDUtil.CODEC, value) }

fun CompoundTag.hasUUID(key: String): Boolean = this.read(key, UUIDUtil.CODEC).isPresent

fun CompoundTag.getBlockPos(key: String): Optional<BlockPos> = this.read(key, BlockPos.CODEC)

fun CompoundTag.putBlockPos(key: String, pos: BlockPos) { this.store(key, BlockPos.CODEC, pos) }
