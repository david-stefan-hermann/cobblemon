package com.cobblemon.mod.common.block

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.PrimitiveCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock

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
    }

    override fun canGrow(pos: BlockPos, world: BlockGetter): Boolean = stage != MAX_STAGE
    override fun codec(): MapCodec<out DirectionalBlock?>? = CODEC
}