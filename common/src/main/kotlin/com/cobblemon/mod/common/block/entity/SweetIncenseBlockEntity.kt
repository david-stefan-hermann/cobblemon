/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block.entity

import com.cobblemon.mod.common.CobblemonBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

// PT145: BlockEntity(BlockEntityType<*>) requires non-null type; placeholder uses chest type until INCENSE_SWEET is wired in 1.7.
class SweetIncenseBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(
    net.minecraft.world.level.block.entity.BlockEntityType.CHEST, pos, state)
