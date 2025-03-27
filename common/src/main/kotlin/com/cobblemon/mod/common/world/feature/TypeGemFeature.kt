package com.cobblemon.mod.common.world.feature

import com.cobblemon.mod.common.CobblemonBlocks.TYPE_GEM_CORE
import com.cobblemon.mod.common.CobblemonBlocks.typeGemClusters
import com.cobblemon.mod.common.block.TypeGemClusterBlock.Companion.FACING
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
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

        val randomAirBlock = neighborAirBlocks[random.nextInt(0, neighborAirBlocks.size)]
        val randomGemPos = randomAirBlock.first
        val randomGemDirection = randomAirBlock.second

        val typeGemClusters = typeGemClusters().toList()
        val randomGem = typeGemClusters[random.nextInt(0, typeGemClusters.size)].second
        val randomGemBlockState = randomGem.defaultBlockState().setValue(FACING, randomGemDirection)

        worldGenLevel.setBlock(origin, TYPE_GEM_CORE.defaultBlockState(), UPDATE_ALL)
        worldGenLevel.setBlock(randomGemPos, randomGemBlockState, UPDATE_ALL)

        return true
    }

    fun getNeighborAirBlocks(worldGenLevel: WorldGenLevel, origin: BlockPos): List<Pair<BlockPos, Direction>> {
        val airBlocks = mutableListOf<Pair<BlockPos, Direction>>()

        for (direction: Direction in Direction.entries) {
            val pos = origin.offset(direction.normal)
            val blockState = worldGenLevel.getBlockState(pos)
            if (blockState.isAir) {
                airBlocks.add(Pair(pos, direction))
            }
        }

        return airBlocks
    }
}