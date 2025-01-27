package com.cobblemon.mod.common.block

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.PrimitiveCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.state.BlockState

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
            Block.CODEC.fieldOf("nextStage").forGetter { it.nextStage }
        ).apply(it, ::TypeGemBlock) }

        const val STAGE_0 = 0
        const val STAGE_1 = 1
        const val STAGE_2 = 2
        const val STAGE_3 = 3
        const val STAGE_4 = 4

        const val MAX_STAGE = STAGE_4
        const val MIN_STAGE = STAGE_0
    }

    override val growthChance = 1

    override fun canGrow(pos: BlockPos, world: BlockGetter): Boolean = stage != MAX_STAGE
    override fun isRandomlyTicking(state: BlockState): Boolean = stage < MAX_STAGE
    override fun codec(): MapCodec<out DirectionalBlock?>? = CODEC
}