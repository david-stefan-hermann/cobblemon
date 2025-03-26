package com.cobblemon.mod.common.block

import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.api.tags.CobblemonBlockTags
import com.cobblemon.mod.common.block.TypeGemClusterBlock.Companion.SHOULD_GROW
import com.cobblemon.mod.common.util.cobblemonResource
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
        const val GROWTH_CONTINUATION_BONUS = 3

        // todo clean this shit up. The lack of lazy made this not work and it drove me crazy trying to debug what was happening >:C
        val BLOCK_TO_CLUSTER: Map<ResourceLocation, Block> by lazy {  mapOf(
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
        println("[TypeGemCoreBlock] Tick at $pos")

        val connectedGems = getConnectedGems(level, pos)
        println("[TypeGemCoreBlock] Found ${connectedGems.size} connected gems")

        if (connectedGems.size >= MAX_CONNECTED_GEMS) {
            println("[TypeGemCoreBlock] Max connected gems reached. No growth.")
            return
        }

        val gemBlocks = connectedGems.filter { isGem(it.first) }
        val chosenBlock = gemBlocks.randomOrNull() ?: run {
            println("[TypeGemCoreBlock] No gem blocks found to grow from.")
            return
        }

        val chosenBlockState = chosenBlock.first
        val chosenBlockPosition = chosenBlock.second
        val sourceBlock = chosenBlockState.block
        val chosenDirection = if (chosenBlockState.hasProperty(DirectionalBlock.FACING)) {
            chosenBlockState.getValue(DirectionalBlock.FACING)
        } else null

        val registryKey = BuiltInRegistries.BLOCK.getKey(sourceBlock) ?: run {
            println("[TypeGemCoreBlock] Source block has no registry name!")
            return
        }

        val blockToGrow = BLOCK_TO_CLUSTER[registryKey] ?: run {
            println("[TypeGemCoreBlock] No cluster mapped for $registryKey")
            return
        }

        println("[TypeGemCoreBlock] Chose block at $chosenBlockPosition facing $chosenDirection")
        println("[TypeGemCoreBlock] Growing cluster block: ${blockToGrow.name}")

        val possibleGrowthPositions = mutableListOf<Pair<BlockPos, Direction>>()
        for (direction in Direction.entries) {
            val newPosition = chosenBlockPosition.relative(direction)
            if (level.getBlockState(newPosition).isAir && isPositionValidForGrowth(level, newPosition)) {
                possibleGrowthPositions.add(Pair(newPosition, direction))
            }
        }

        if (possibleGrowthPositions.isEmpty()) {
            println("[TypeGemCoreBlock] No valid positions for growth.")
            return
        }

        val weightedGrowthPositions = possibleGrowthPositions.flatMap { (pos, dir) ->
            if (dir == chosenDirection) List(GROWTH_CONTINUATION_BONUS) { Pair(pos, dir) }
            else listOf(Pair(pos, dir))
        }

        val (posToGrow, directionToGrow) = weightedGrowthPositions[random.nextInt(weightedGrowthPositions.size)]
        println("[TypeGemCoreBlock] Growing new block at $posToGrow facing $directionToGrow")

        var newBlockState = blockToGrow.defaultBlockState()

        if (newBlockState.hasProperty(DirectionalBlock.FACING)) {
            newBlockState = newBlockState.setValue(DirectionalBlock.FACING, directionToGrow)
        }
        if (newBlockState.hasProperty(SHOULD_GROW)) {
            newBlockState = newBlockState.setValue(SHOULD_GROW, true)
        }

        level.setBlockAndUpdate(posToGrow, newBlockState)
        val placed = level.getBlockState(posToGrow)
        println("[TypeGemCoreBlock] Block now at $posToGrow is ${placed.block.name}")
    }

    private fun getConnectedGems(level: ServerLevel, pos: BlockPos): List<Pair<BlockState, BlockPos>> {
        println("[TypeGemCoreBlock] Scanning for connected gems around core at $pos")

        val connectedGems = mutableListOf<Pair<BlockState, BlockPos>>()
        val visitedPositions = mutableSetOf<BlockPos>()
        val positionsToVisit: Queue<BlockPos> = LinkedList()

        for (direction in Direction.entries) {
            val adjacentPos = pos.relative(direction)
            visitedPositions.add(adjacentPos)
            positionsToVisit.add(adjacentPos)
            println("[TypeGemCoreBlock] Checking adjacent block at $adjacentPos")
        }

        while (positionsToVisit.isNotEmpty()) {
            val currentPos = positionsToVisit.poll()
            val blockState = level.getBlockState(currentPos)
            val block = blockState.block

            println("[TypeGemCoreBlock] Visiting $currentPos - ${block.name}")

            if (!isGem(blockState)) {
                println("[TypeGemCoreBlock] Skipping $currentPos - not a gem block")
                continue
            }

            println("[TypeGemCoreBlock] Connected gem found at $currentPos (${block.name})")
            connectedGems.add(Pair(blockState, currentPos))

            for (direction in Direction.entries) {
                val neighborPos = currentPos.relative(direction)
                if (visitedPositions.add(neighborPos)) {
                    positionsToVisit.add(neighborPos)
                }
            }
        }

        println("[TypeGemCoreBlock] Total connected gems found: ${connectedGems.size}")
        return connectedGems
    }

    private fun isPositionValidForGrowth(level: ServerLevel, pos: BlockPos): Boolean {
        for (direction in Direction.entries) {
            val nearbyPos = pos.relative(direction)
            val nearbyState = level.getBlockState(nearbyPos)

            if (isGem(nearbyState)) {
                if (pos.distManhattan(nearbyPos) < MIN_DISTANCE_BETWEEN_GEMS) {
                    return false
                }
            }
        }
        return true
    }

    private fun isGem(block: BlockState): Boolean =
            block.`is`(CobblemonBlockTags.TYPE_GEM_BLOCKS)

    override fun isRandomlyTicking(state: BlockState): Boolean = true
}
