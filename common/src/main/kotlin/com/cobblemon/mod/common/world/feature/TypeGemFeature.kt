package com.cobblemon.mod.common.world.feature

import com.cobblemon.mod.common.CobblemonBlocks.TYPE_GEM_CORE
import com.cobblemon.mod.common.CobblemonBlocks.typeGemBlocks
import com.cobblemon.mod.common.block.TypeGemCoreBlock
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

        val randomGemPos = neighborAirBlocks[random.nextInt(0, neighborAirBlocks.size)]
        val typeGemBlocks = typeGemBlocks().toList()
        val randomGemBlock = typeGemBlocks[random.nextInt(0, typeGemBlocks.size)].second
        val randomGemBlockBlockState = randomGemBlock.defaultBlockState()

        val typeGemCoreBlockState = TYPE_GEM_CORE.defaultBlockState()

        worldGenLevel.setBlock(origin, typeGemCoreBlockState, UPDATE_ALL)
        worldGenLevel.setBlock(randomGemPos, randomGemBlockBlockState, UPDATE_ALL)

        val typeGemCoreBlock = typeGemCoreBlockState.block as TypeGemCoreBlock
        typeGemCoreBlock.forceGrow(worldGenLevel, origin, random, 0.5f)

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