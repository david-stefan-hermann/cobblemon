package com.cobblemon.mod.common.block

import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.api.tags.CobblemonBlockTags
import com.cobblemon.mod.common.block.TypeGemClusterBlock.Companion.SHOULD_GROW
import com.cobblemon.mod.common.block.TypeGemClusterBlock.Companion.STUNTED
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import java.util.*

class TypeGemCoreBlock(properties: Properties) : Block(properties) {

    companion object {
        const val MAX_CONNECTED_GEMS = 30
        const val MIN_DISTANCE_BETWEEN_GEMS = 1
        const val CONTINUATION_CHANCE = 0.8f

        val BLOCK_TO_CLUSTER: Map<ResourceLocation, Block> by lazy {
            mapOf(
                    ResourceLocation.parse("cobblemon:type_gem_block_normal") to CobblemonBlocks.TYPE_GEM_CLUSTER_NORMAL,
                    ResourceLocation.parse("cobblemon:type_gem_block_fire") to CobblemonBlocks.TYPE_GEM_CLUSTER_FIRE,
                    ResourceLocation.parse("cobblemon:type_gem_block_water") to CobblemonBlocks.TYPE_GEM_CLUSTER_WATER,
                    ResourceLocation.parse("cobblemon:type_gem_block_electric") to CobblemonBlocks.TYPE_GEM_CLUSTER_ELECTRIC,
                    ResourceLocation.parse("cobblemon:type_gem_block_grass") to CobblemonBlocks.TYPE_GEM_CLUSTER_GRASS,
                    ResourceLocation.parse("cobblemon:type_gem_block_ice") to CobblemonBlocks.TYPE_GEM_CLUSTER_ICE,
                    ResourceLocation.parse("cobblemon:type_gem_block_fighting") to CobblemonBlocks.TYPE_GEM_CLUSTER_FIGHTING,
                    ResourceLocation.parse("cobblemon:type_gem_block_poison") to CobblemonBlocks.TYPE_GEM_CLUSTER_POISON,
                    ResourceLocation.parse("cobblemon:type_gem_block_ground") to CobblemonBlocks.TYPE_GEM_CLUSTER_GROUND,
                    ResourceLocation.parse("cobblemon:type_gem_block_flying") to CobblemonBlocks.TYPE_GEM_CLUSTER_FLYING,
                    ResourceLocation.parse("cobblemon:type_gem_block_psychic") to CobblemonBlocks.TYPE_GEM_CLUSTER_PSYCHIC,
                    ResourceLocation.parse("cobblemon:type_gem_block_bug") to CobblemonBlocks.TYPE_GEM_CLUSTER_BUG,
                    ResourceLocation.parse("cobblemon:type_gem_block_rock") to CobblemonBlocks.TYPE_GEM_CLUSTER_ROCK,
                    ResourceLocation.parse("cobblemon:type_gem_block_ghost") to CobblemonBlocks.TYPE_GEM_CLUSTER_GHOST,
                    ResourceLocation.parse("cobblemon:type_gem_block_dragon") to CobblemonBlocks.TYPE_GEM_CLUSTER_DRAGON,
                    ResourceLocation.parse("cobblemon:type_gem_block_dark") to CobblemonBlocks.TYPE_GEM_CLUSTER_DARK,
                    ResourceLocation.parse("cobblemon:type_gem_block_steel") to CobblemonBlocks.TYPE_GEM_CLUSTER_STEEL,
                    ResourceLocation.parse("cobblemon:type_gem_block_fairy") to CobblemonBlocks.TYPE_GEM_CLUSTER_FAIRY
            )
        }
    }

    override fun randomTick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        val connectedGems = getConnectedGems(level, pos)

        val gemBlocks = connectedGems.filter { isGem(it.first) }
        val chosenBlock = gemBlocks.randomOrNull() ?: return

        val chosenBlockState = chosenBlock.first
        val chosenBlockPosition = chosenBlock.second
        val sourceBlock = chosenBlockState.block
        val chosenDirection = if (chosenBlockState.hasProperty(DirectionalBlock.FACING))
            chosenBlockState.getValue(DirectionalBlock.FACING)
        else null

        val registryKey = BuiltInRegistries.BLOCK.getKey(sourceBlock) ?: return
        val blockToGrow = BLOCK_TO_CLUSTER[registryKey] ?: return

        val possibleGrowthPositions = Direction.entries
                .map { it to chosenBlockPosition.relative(it) }
                .filter { (_, pos) ->
                    level.getBlockState(pos).isAir && isPositionValidForGrowth(level, pos)
                }
                .map { (dir, pos) -> pos to dir }

        if (possibleGrowthPositions.isEmpty()) return

        val (preferred, fallback) = if (chosenDirection != null)
            possibleGrowthPositions.partition { it.second == chosenDirection }
        else
            Pair(emptyList(), possibleGrowthPositions)

        val chosenPair = when {
            preferred.isNotEmpty() && random.nextFloat() < CONTINUATION_CHANCE -> preferred[random.nextInt(preferred.size)]
            fallback.isNotEmpty() -> fallback[random.nextInt(fallback.size)]
            else -> null
        } ?: return

        val (posToGrow, directionToGrow) = chosenPair

        val overLimit = connectedGems.size >= MAX_CONNECTED_GEMS

        var newBlockState = blockToGrow.defaultBlockState()

        if (newBlockState.hasProperty(DirectionalBlock.FACING)) {
            newBlockState = newBlockState.setValue(DirectionalBlock.FACING, directionToGrow)
        }

        if (newBlockState.hasProperty(SHOULD_GROW)) {
            newBlockState = newBlockState.setValue(SHOULD_GROW, !overLimit)
        }

        if (newBlockState.hasProperty(STUNTED)) {
            newBlockState = newBlockState.setValue(STUNTED, overLimit)
        }

        level.setBlockAndUpdate(posToGrow, newBlockState)
    }

    private fun getConnectedGems(level: ServerLevel, pos: BlockPos): List<Pair<BlockState, BlockPos>> {
        val connectedGems = mutableListOf<Pair<BlockState, BlockPos>>()
        val visited = mutableSetOf<BlockPos>()
        val queue: Queue<BlockPos> = LinkedList()

        for (direction in Direction.entries) {
            val adjacent = pos.relative(direction)
            visited.add(adjacent)
            queue.add(adjacent)
        }

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            val state = level.getBlockState(current)

            if (!isGem(state)) continue
            connectedGems.add(state to current)

            for (direction in Direction.entries) {
                val neighbor = current.relative(direction)
                if (visited.add(neighbor)) queue.add(neighbor)
            }
        }

        return connectedGems
    }

    private fun isPositionValidForGrowth(level: ServerLevel, pos: BlockPos): Boolean {
        return Direction.entries.none { dir ->
            val nearby = pos.relative(dir)
            isGem(level.getBlockState(nearby)) &&
                    pos.distManhattan(nearby) < MIN_DISTANCE_BETWEEN_GEMS
        }
    }

    private fun isGem(state: BlockState): Boolean {
        return state.`is`(CobblemonBlockTags.TYPE_GEM_BLOCKS)
    }

    override fun isRandomlyTicking(state: BlockState): Boolean = true
}
