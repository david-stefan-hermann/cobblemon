package com.cobblemon.mod.common.world.feature

import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.CobblemonBlocks.TYPE_GEM_CORE
import com.cobblemon.mod.common.CobblemonBlocks.typeGemBlocks
import com.cobblemon.mod.common.CobblemonBlocks.typeGemClusters
import com.cobblemon.mod.common.block.TypeGemClusterBlock
import com.cobblemon.mod.common.block.TypeGemCoreBlock
import com.cobblemon.mod.common.util.random
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.Block.UPDATE_ALL
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration

class TypeGemFeature : Feature<BlockStateConfiguration>(BlockStateConfiguration.CODEC) {
    override fun place(context: FeaturePlaceContext<BlockStateConfiguration?>): Boolean {
        val worldGenLevel: WorldGenLevel = context.level()
        val random = context.random()
        val origin = context.origin()

        val originBlock = worldGenLevel.getBlockState(origin)
        if (!originBlock.`is`(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
            return false
        }

        val neighborAirBlocks = getNeighborAirBlocks(worldGenLevel, origin)
        if (neighborAirBlocks.isEmpty()) {
            return false
        }

        // Pick a random air block for the gem
        val chosenPos = neighborAirBlocks[random.nextInt(neighborAirBlocks.size)]

        val typeGemBlocks = typeGemBlocks().toList()
        val randomGemEntry = typeGemBlocks[random.nextInt(0, typeGemBlocks.size)]
        val randomGemId = randomGemEntry.first
        val randomGemBlock = randomGemEntry.second
        val randomGemBlockState = randomGemBlock.defaultBlockState()

        val typeGemCoreBlockState = TYPE_GEM_CORE.defaultBlockState()
        worldGenLevel.setBlock(origin, typeGemCoreBlockState, UPDATE_ALL)
        worldGenLevel.setBlock(chosenPos, randomGemBlockState, UPDATE_ALL)

        val typeGemCoreBlock = typeGemCoreBlockState.block as TypeGemCoreBlock
        typeGemCoreBlock.forceGrow(worldGenLevel, origin, random, 0.5f)

        val clusterBlock = TypeGemClusterBlock.clusterFromGemBlock(randomGemBlock)
        if (clusterBlock != null) {
            for (dir in Direction.entries) {
                val clusterPos = origin.relative(dir)
                if (clusterPos == chosenPos) continue // skip the gem block's position
                if (!worldGenLevel.getBlockState(clusterPos).isAir) continue
                if (random.nextFloat() > 0.95f) continue // 80% chance

                val clusterState = clusterBlock.defaultBlockState()
                    .setValue(TypeGemClusterBlock.FACING, dir)
                    .setValue(TypeGemClusterBlock.STAGE, 1)
                    .setValue(TypeGemClusterBlock.SHOULD_GROW, true)
                    .setValue(TypeGemClusterBlock.STUNTED, false)

                println("[TypeGemCoreBlock] Placing ${randomGemId.path} cluster at $clusterPos facing ${dir.opposite}")
                worldGenLevel.setBlock(clusterPos, clusterState, UPDATE_ALL)
            }
        } else {
            println("[TypeGemFeature] Could not find cluster block for gem: $randomGemId")
        }

        return true
    }

    fun getNeighborAirBlocks(worldGenLevel: WorldGenLevel, origin: BlockPos): List<BlockPos> {
        val airBlocks = mutableListOf<BlockPos>()

        for (direction: Direction in Direction.entries) {
            val pos = origin.offset(direction.normal)
            val blockState = worldGenLevel.getBlockState(pos)
            if (blockState.isAir) {
                airBlocks.add(pos)
            }
        }

        return airBlocks
    }
}