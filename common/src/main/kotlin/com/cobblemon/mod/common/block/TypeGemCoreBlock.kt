/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

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
import net.minecraft.world.level.WorldGenLevel
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
        grow(level, pos, random)
    }

    fun forceGrow(level: WorldGenLevel, pos: BlockPos, random: RandomSource, percentage: Float) {
        val desiredLength = MAX_CONNECTED_GEMS * percentage

        while (true) {
            val growInfo = grow(level, pos, random, true)
            val grown = growInfo.first
            val clusterSize = growInfo.second

            if (!grown || clusterSize >= desiredLength) break
        }
    }

    private fun grow(
        level: WorldGenLevel,
        pos: BlockPos,
        random: RandomSource,
        forced: Boolean = false
    ): Pair<Boolean, Int> {
        val connectedGems = getConnectedGems(level, pos)
        val gemCount = connectedGems.count { isGem(it.first) }
        val isOverLimit = gemCount >= MAX_CONNECTED_GEMS

        val javaRandom = java.util.Random(random.nextLong())

        for ((gemState, gemPos) in connectedGems.shuffled(javaRandom)) {
            val registryKey = BuiltInRegistries.BLOCK.getKey(gemState.block)
            val clusterBlock = BLOCK_TO_CLUSTER[registryKey]

            for (dir in Direction.entries.shuffled(javaRandom)) {
                val targetPos = gemPos.relative(dir)
                val targetState = level.getBlockState(targetPos)

                // Allow growth into air or existing clusters
                if (!(targetState.isAir || targetState.block is TypeGemClusterBlock)) continue
                if (!isPositionValidForGrowth(level, targetPos)) continue

                // Place gem block
                val gemCopy = gemState.block.defaultBlockState()
                level.setBlock(targetPos, gemCopy, UPDATE_ALL)

                // if forced immediately surround with clusters
                if (forced && clusterBlock != null) {
                    for (clusterDir in Direction.entries) {
                        val clusterPos = targetPos.relative(clusterDir)
                        if (!level.getBlockState(clusterPos).isAir) continue

                        var clusterState = clusterBlock.defaultBlockState()

                        if (clusterState.hasProperty(DirectionalBlock.FACING)) {
                            clusterState = clusterState.setValue(DirectionalBlock.FACING, clusterDir)
                        }
                        if (clusterState.hasProperty(SHOULD_GROW)) {
                            clusterState = clusterState.setValue(SHOULD_GROW, true)
                        }
                        if (clusterState.hasProperty(STUNTED)) {
                            clusterState = clusterState.setValue(STUNTED, isOverLimit)
                        }
                        if (clusterState.hasProperty(TypeGemClusterBlock.STAGE)) {
                            val randomStage = random.nextInt(0, 4)
                            clusterState = clusterState.setValue(TypeGemClusterBlock.STAGE, randomStage)
                        }

                        //println("[TypeGemCoreBlock] Placing ${if (isOverLimit) "STUNTED" else "normal"} cluster at $clusterPos facing ${clusterDir.opposite}")
                        level.setBlock(clusterPos, clusterState, UPDATE_ALL)
                    }
                }

                return Pair(true, connectedGems.size)
            }
        }

        println("[TypeGemCoreBlock] No valid spot found for gem placement.")
        return Pair(false, connectedGems.size)
    }

    private fun getConnectedGems(level: WorldGenLevel, pos: BlockPos): List<Pair<BlockState, BlockPos>> {
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

    private fun isPositionValidForGrowth(level: WorldGenLevel, pos: BlockPos): Boolean {
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
