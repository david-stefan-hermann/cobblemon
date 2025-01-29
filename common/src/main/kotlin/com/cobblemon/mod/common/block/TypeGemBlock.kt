/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.PrimitiveCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty

class TypeGemBlock(
    settings: Properties,
    stage: Int,
    height: Int,
    xzOffset: Int,
    nextStage: Block?
) : GrowableStoneBlock(settings, stage, height, xzOffset, nextStage) {

    companion object {
        val CODEC: MapCodec<TypeGemBlock> = RecordCodecBuilder.mapCodec { it.group(
            propertiesCodec(),
            PrimitiveCodec.INT.fieldOf("stage").forGetter { it.stage },
            PrimitiveCodec.INT.fieldOf("height").forGetter { it.height },
            PrimitiveCodec.INT.fieldOf("xzOffset").forGetter { it.xzOffset },
            Block.CODEC.fieldOf("nextStage").forGetter { it.nextStage },
        ).apply(it, ::TypeGemBlock) }

        const val STAGE_0 = 0
        const val STAGE_1 = 1
        const val STAGE_2 = 2
        const val STAGE_3 = 3
        const val STAGE_4 = 4

        const val MAX_STAGE = STAGE_4

        val SHOULD_GROW: BooleanProperty = BooleanProperty.create("should_grow")
    }

    override val growthChance = 1

    init {
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.DOWN)
            .setValue(SHOULD_GROW, true)
        )
    }

    override fun canGrow(state: BlockState, pos: BlockPos, world: BlockGetter): Boolean = (state.getValue(SHOULD_GROW) && stage < MAX_STAGE) || stage < STAGE_3
    override fun isRandomlyTicking(state: BlockState): Boolean = (state.getValue(SHOULD_GROW) && stage < MAX_STAGE) || stage < STAGE_3
    override fun codec(): MapCodec<out DirectionalBlock?>? = CODEC

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(SHOULD_GROW)
    }
}