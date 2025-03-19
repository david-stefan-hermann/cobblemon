package com.cobblemon.mod.common.block

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty

class TypeGemClusterBlock(
    settings: Properties,
    val nextStage: Block
) : DirectionalBlock(settings) {

    companion object {
        val CODEC: MapCodec<TypeGemClusterBlock> = RecordCodecBuilder.mapCodec { it.group(
            propertiesCodec(),
            Block.CODEC.fieldOf("nextStage").forGetter { it.nextStage }
        ).apply(it, ::TypeGemClusterBlock) }

        val SHOULD_GROW: BooleanProperty = BooleanProperty.create("should_grow")
        val FACING: DirectionProperty = DirectionalBlock.FACING
    }

    init {
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.UP) // Default to UP if no face is provided
            .setValue(SHOULD_GROW, true)
        )
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        // Ensure correct attachment direction
        //val facing = context.clickedFace.opposite
        val facing = context.clickedFace
        val world = context.level
        val pos = context.clickedPos.relative(facing.opposite) // todo maybe we don't need opposite
        val blockBelow = world.getBlockState(pos).block

        val shouldGrow = blockBelow is TypeGemCoreBlock
        return defaultBlockState()
            .setValue(FACING, facing) // Set facing to attach properly
            .setValue(SHOULD_GROW, shouldGrow)
    }

    override fun isRandomlyTicking(state: BlockState): Boolean = state.getValue(SHOULD_GROW)

    override fun randomTick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        if (state.getValue(SHOULD_GROW)) {
            var nextState = nextStage.defaultBlockState()

            // Only apply FACING if the next stage block supports it
            if (nextState.hasProperty(FACING)) {
                nextState = nextState.setValue(FACING, state.getValue(FACING))
            }

            level.setBlockAndUpdate(pos, nextState)
        }
    }


    override fun codec(): MapCodec<out DirectionalBlock?>? = CODEC

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(FACING, SHOULD_GROW)
    }
}
