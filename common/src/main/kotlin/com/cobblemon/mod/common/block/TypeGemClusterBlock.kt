package com.cobblemon.mod.common.block

import com.cobblemon.mod.common.api.tags.CobblemonBlockTags
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

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
        val STAGE: IntegerProperty = IntegerProperty.create("stage", 0, 3)
        val STUNTED: BooleanProperty = BooleanProperty.create("stunted")
    }

    init {
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(SHOULD_GROW, true)
                .setValue(STAGE, 0)
                .setValue(STUNTED, false)
        )
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val facing = context.clickedFace
        val world = context.level
        val pos = context.clickedPos.relative(facing.opposite)
        val blockBelow = world.getBlockState(pos).block
        val shouldGrow = blockBelow is TypeGemCoreBlock

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(SHOULD_GROW, shouldGrow)
                .setValue(STAGE, 0)
                .setValue(STUNTED, false)
    }

    override fun isRandomlyTicking(state: BlockState): Boolean = state.getValue(SHOULD_GROW)

    override fun randomTick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        if (!state.getValue(SHOULD_GROW)) return

        val currentStage = state.getValue(STAGE)

        if (currentStage < 3) {
            // Advance stage
            level.setBlockAndUpdate(pos, state.setValue(STAGE, currentStage + 1))
        } else {
            if (state.getValue(STUNTED)) {
                println("[TypeGemClusterBlock] Cluster at $pos is already stunted. Growth halted.")
                level.setBlockAndUpdate(pos, state.setValue(SHOULD_GROW, false))
                return
            }

            val facing = state.getValue(FACING)

            // Log type of cluster based on nextStage
            val clusterType = level.registryAccess()
                    .registryOrThrow(BuiltInRegistries.BLOCK.key())
                    .getKey(nextStage)
                    .toString()

            println("[TypeGemClusterBlock] Attempting to grow into TypeGemBlock at $pos")
            println("[TypeGemClusterBlock] Cluster type: $clusterType")

            // Scan neighboring blocks (excluding where it grew from)
            val neighbors = Direction.entries
                    .filter { it != facing.opposite }
                    .map { dir ->
                        val neighborPos = pos.relative(dir)
                        val neighborState = level.getBlockState(neighborPos)
                        val name = BuiltInRegistries.BLOCK.getKey(neighborState.block)
                        println("[TypeGemClusterBlock] Neighbor at $dir -> $name")
                        neighborState.block
                    }

            val hasConflict = Direction.entries
                    .filter { it != facing.opposite }
                    .any { dir ->
                        val neighborPos = pos.relative(dir)
                        val neighborState = level.getBlockState(neighborPos)
                        val neighborBlockId = BuiltInRegistries.BLOCK.getKey(neighborState.block)
                        println("[TypeGemClusterBlock] Neighbor at $dir -> $neighborBlockId")
                        isGem(neighborState)
                    }

            if (hasConflict) {
                println("[TypeGemClusterBlock] Found nearby TypeGemBlock(s). Stunting cluster at $pos.")
                level.setBlockAndUpdate(
                        pos,
                        state.setValue(STUNTED, true).setValue(SHOULD_GROW, false)
                )
            } else {
                println("[TypeGemClusterBlock] No conflicts. Converting cluster to TypeGemBlock.")
                var nextState = nextStage.defaultBlockState()
                if (nextState.hasProperty(FACING)) {
                    nextState = nextState.setValue(FACING, facing)
                }
                level.setBlockAndUpdate(pos, nextState)
            }
        }
    }

    private fun isGem(state: BlockState): Boolean {
        return state.`is`(CobblemonBlockTags.TYPE_GEM_BLOCKS)
    }


    override fun getShape(
            state: BlockState,
            level: net.minecraft.world.level.BlockGetter,
            pos: BlockPos,
            context: net.minecraft.world.phys.shapes.CollisionContext
    ): VoxelShape {
        return when (state.getValue(STAGE)) {
            0 -> box(5.0, 0.0, 5.0, 11.0, 6.0, 11.0)  // small bud
            1 -> box(4.0, 0.0, 4.0, 12.0, 9.0, 12.0)  // medium bud
            2 -> box(3.0, 0.0, 3.0, 13.0, 13.0, 13.0) // large bud
            3 -> box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0) // full cluster
            else -> Shapes.empty()
        }
    }

    override fun codec(): MapCodec<out DirectionalBlock> = CODEC

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(FACING, SHOULD_GROW, STAGE, STUNTED)
    }
}
