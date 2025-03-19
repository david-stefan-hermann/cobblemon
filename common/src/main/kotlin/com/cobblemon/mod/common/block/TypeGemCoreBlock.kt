package com.cobblemon.mod.common.block

import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.block.TypeGemClusterBlock.Companion.SHOULD_GROW
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import java.util.*

class TypeGemCoreBlock(properties: Properties) : Block(properties) {

    companion object {
        const val MAX_CONNECTED_GEMS = 30
        const val MIN_DISTANCE_BETWEEN_GEMS = 2
        const val GROWTH_CONTINUATION_BONUS = 3 // Higher weight for same direction growth

        val GEM_CLUSTERS: Set<Block> = setOf(
            CobblemonBlocks.TYPE_GEM_CLUSTER_NORMAL,
            CobblemonBlocks.TYPE_GEM_CLUSTER_FIRE,
            CobblemonBlocks.TYPE_GEM_CLUSTER_WATER,
            CobblemonBlocks.TYPE_GEM_CLUSTER_ELECTRIC,
            CobblemonBlocks.TYPE_GEM_CLUSTER_GRASS,
            CobblemonBlocks.TYPE_GEM_CLUSTER_ICE,
            CobblemonBlocks.TYPE_GEM_CLUSTER_FIGHTING,
            CobblemonBlocks.TYPE_GEM_CLUSTER_POISON,
            CobblemonBlocks.TYPE_GEM_CLUSTER_GROUND,
            CobblemonBlocks.TYPE_GEM_CLUSTER_FLYING,
            CobblemonBlocks.TYPE_GEM_CLUSTER_PSYCHIC,
            CobblemonBlocks.TYPE_GEM_CLUSTER_BUG,
            CobblemonBlocks.TYPE_GEM_CLUSTER_ROCK,
            CobblemonBlocks.TYPE_GEM_CLUSTER_GHOST,
            CobblemonBlocks.TYPE_GEM_CLUSTER_DRAGON,
            CobblemonBlocks.TYPE_GEM_CLUSTER_DARK,
            CobblemonBlocks.TYPE_GEM_CLUSTER_STEEL,
            CobblemonBlocks.TYPE_GEM_CLUSTER_FAIRY
        )
    }

    override fun randomTick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        val connectedGems = getConnectedGems(level, pos)
        if (connectedGems.size >= MAX_CONNECTED_GEMS) {
            return
        }

        val gemBlocks = connectedGems.filter { isGemBlock(it.first) }
        val chosenBlock = gemBlocks.randomOrNull() ?: return

        val chosenBlockPosition = chosenBlock.second
        val chosenDirection = if (chosenBlock.first.hasProperty(DirectionalBlock.FACING)) {
            chosenBlock.first.getValue(DirectionalBlock.FACING)
        } else {
            null // If no FACING property exists, allow random growth
        }

        val possibleGrowthPositions = mutableListOf<Pair<BlockPos, Direction>>()

        for (direction in Direction.entries) {
            val newPosition = chosenBlockPosition.relative(direction)

            if (level.getBlockState(newPosition).isAir && isPositionValidForGrowth(level, newPosition)) {
                possibleGrowthPositions.add(Pair(newPosition, direction))
            }
        }

        if (possibleGrowthPositions.isEmpty()) return

        // **Weighting Growth Towards the Same Direction**
        val weightedGrowthPositions = possibleGrowthPositions.flatMap { (pos, dir) ->
            if (dir == chosenDirection) List(GROWTH_CONTINUATION_BONUS) { Pair(pos, dir) }
            else listOf(Pair(pos, dir))
        }

        val (posToGrow, directionToGrow) = weightedGrowthPositions[random.nextInt(weightedGrowthPositions.size)]

        val newBlockState = chosenBlock.first.block.defaultBlockState()
            .setValue(DirectionalBlock.FACING, directionToGrow)
            .setValue(SHOULD_GROW, true)

        level.setBlockAndUpdate(posToGrow, newBlockState)
    }

    private fun getConnectedGems(level: ServerLevel, pos: BlockPos): List<Pair<BlockState, BlockPos>> {
        val connectedGems = mutableListOf<Pair<BlockState, BlockPos>>()
        val visitedPositions = mutableSetOf(pos)
        val positionsToVisit: Queue<BlockPos> = LinkedList(listOf(pos))

        while (positionsToVisit.isNotEmpty()) {
            val position = positionsToVisit.poll()
            val blockState = level.getBlockState(position)

            if (!isGem(blockState)) continue

            connectedGems.add(Pair(blockState, position))
            for (direction in Direction.entries) {
                val newPosition = position.relative(direction)
                if (visitedPositions.add(newPosition)) {
                    positionsToVisit.add(newPosition)
                }
            }
        }

        return connectedGems
    }

    private fun isPositionValidForGrowth(level: ServerLevel, pos: BlockPos): Boolean {
        // Ensuring a minimum distance between gems
        for (direction in Direction.entries) {
            val nearbyPos = pos.relative(direction)
            val nearbyState = level.getBlockState(nearbyPos)

            if (isGem(nearbyState)) {
                if (pos.distManhattan(nearbyPos) < MIN_DISTANCE_BETWEEN_GEMS) {
                    return false // Prevents clusters from forming too closely
                }
            }
        }
        return true
    }

    private fun isGem(block: BlockState): Boolean = GEM_CLUSTERS.any { block.`is`(it) }
    private fun isGemBlock(block: BlockState): Boolean = isGem(block)

    override fun isRandomlyTicking(state: BlockState): Boolean = true
}
