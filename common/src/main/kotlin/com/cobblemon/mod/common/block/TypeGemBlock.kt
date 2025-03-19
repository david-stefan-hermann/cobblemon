package com.cobblemon.mod.common.block

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.PrimitiveCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty

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
        const val MAX_STAGE = STAGE_1

        val SHOULD_GROW: BooleanProperty = BooleanProperty.create("should_grow")
        val FACING: DirectionProperty = DirectionalBlock.FACING
    }

    override val growthChance = 1

    init {
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.DOWN) // Ensure facing is properly registered
            .setValue(SHOULD_GROW, true)
        )
    }

    override fun canGrow(state: BlockState, pos: BlockPos, world: BlockGetter): Boolean {
        return state.getValue(SHOULD_GROW) && stage < MAX_STAGE
    }

    override fun isRandomlyTicking(state: BlockState): Boolean {
        return state.getValue(SHOULD_GROW) && stage < MAX_STAGE
    }

    override fun randomTick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        if (canGrow(state, pos, level)) {
            var nextState = nextStage?.defaultBlockState()
            if (nextState != null) {
                if (nextState.hasProperty(FACING)) {
                    nextState = nextState.setValue(FACING, state.getValue(FACING)) // Preserve FACING for core
                }
                level.setBlockAndUpdate(pos, nextState)
            }
        }
    }

    override fun codec(): MapCodec<out DirectionalBlock?>? = CODEC

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(FACING, SHOULD_GROW) // Ensure FACING is registered
    }
}
