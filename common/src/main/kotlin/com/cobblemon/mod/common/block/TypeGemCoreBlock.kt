/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block

import com.cobblemon.mod.common.CobblemonBlocks
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock.FACING
import net.minecraft.world.level.block.state.BlockState
import java.util.LinkedList
import java.util.Queue

class TypeGemCoreBlock(properties: Properties) : Block(properties) {

    companion object {
        const val MAX_CONNECTED_GEMS = 30
    }

    override fun randomTick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        val connectedGems = getConnectedGems(level, pos)
        if (connectedGems.size >= MAX_CONNECTED_GEMS) {
            return
        }

        val gemBlocks = connectedGems.filter { isGemBlock(it.first) }
        val chosenBlock = gemBlocks.randomOrNull()
        if (chosenBlock == null) {
            return
        }

        val chosenBlockPosition = chosenBlock.second

        var posToGrow: BlockPos? = null
        var directionToGrow: Direction? = null

        for (direction in Direction.entries) {
            val newPosition = BlockPos(
                chosenBlockPosition.x + direction.stepX,
                chosenBlockPosition.y + direction.stepY,
                chosenBlockPosition.z + direction.stepZ,
            )

            if (level.getBlockState(newPosition).isAir) {
                posToGrow = newPosition
                directionToGrow = direction

                break
            }
        }

        if (posToGrow == null) {
            return
        }

        val newBlockState = CobblemonBlocks.SMALL_TYPE_GEM.defaultBlockState().setValue(FACING, directionToGrow!!)
        level.setBlockAndUpdate(posToGrow, newBlockState)
    }

    private fun getConnectedGems(level: ServerLevel, pos: BlockPos): List<Pair<BlockState, BlockPos>> {
        val connectedGems = mutableListOf<Pair<BlockState, BlockPos>>()
        val visitedPositions = mutableSetOf<BlockPos>(pos)
        val positionsToVisit: Queue<BlockPos> = LinkedList(visitedPositions)

        while (!positionsToVisit.isEmpty()) {
            val position = positionsToVisit.poll()
            val blockState = level.getBlockState(position)

            if (!isGem(blockState)) {
                continue
            }

            connectedGems.add(Pair(blockState, position))
            for (direction in Direction.entries) {
                val newPosition = BlockPos(
                    position.x + direction.stepX,
                    position.y + direction.stepY,
                    position.z + direction.stepZ,
                )

                if (visitedPositions.add(newPosition)) {
                    positionsToVisit.add(newPosition)
                }
            }
        }

        return connectedGems
    }

    private fun isGem(block: BlockState): Boolean =
        block.`is`(CobblemonBlocks.TYPE_GEM_CORE) ||
        block.`is`(CobblemonBlocks.TYPE_GEM_BLOCK) ||
        block.`is`(CobblemonBlocks.SMALL_TYPE_GEM) ||
        block.`is`(CobblemonBlocks.MEDIUM_TYPE_GEM) ||
        block.`is`(CobblemonBlocks.LARGE_TYPE_GEM)

    private fun isGemBlock(block: BlockState): Boolean =
        block.`is`(CobblemonBlocks.TYPE_GEM_CORE) ||
        block.`is`(CobblemonBlocks.TYPE_GEM_BLOCK)

    override fun isRandomlyTicking(state: BlockState): Boolean = true
}