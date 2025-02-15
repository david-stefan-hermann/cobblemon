/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.block

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.tms.TechnicalMachine
import com.cobblemon.mod.common.api.tms.TechnicalMachines
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.block.entity.TMBlockEntity
import com.cobblemon.mod.common.item.components.TMMoveComponent
import com.cobblemon.mod.common.util.itemRegistry
import com.cobblemon.mod.common.util.playSoundServer
import com.cobblemon.mod.common.util.toVec3d
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.Containers
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext

class TMBlock(properties: BlockBehaviour.Properties) : BaseEntityBlock(properties), SimpleWaterloggedBlock {

    companion object {
        val ON: BooleanProperty = BooleanProperty.create("on")
        val FACING = BlockStateProperties.HORIZONTAL_FACING
        val TRIGGERED = BlockStateProperties.TRIGGERED
        val WATERLOGGED = BlockStateProperties.WATERLOGGED
        val POWERED: BooleanProperty = BooleanProperty.create("powered")

        private val NORTH_OUTLINE: VoxelShape = Shapes.or(
                Shapes.box(0.0, 0.0, 0.0, 1.0, 0.3125, 0.9375),
                Shapes.box(0.0, 0.3125, 0.75, 1.0, 0.9375, 0.9375),
                Shapes.box(0.0625, 0.3125, 0.0625, 0.9375, 0.875, 0.9375)
        )

        private val SOUTH_OUTLINE: VoxelShape = Shapes.or(
                Shapes.box(0.0, 0.0, 0.0625, 1.0, 0.3125, 1.0),
                Shapes.box(0.0, 0.3125, 0.0625, 1.0, 0.9375, 0.25),
                Shapes.box(0.0625, 0.3125, 0.0625, 0.9375, 0.875, 0.9375)
        )

        private val WEST_OUTLINE: VoxelShape = Shapes.or(
                Shapes.box(0.0, 0.0, 0.0, 0.9375, 0.3125, 1.0),
                Shapes.box(0.75, 0.3125, 0.0, 0.9375, 0.9375, 1.0),
                Shapes.box(0.0625, 0.3125, 0.0625, 0.9375, 0.875, 0.9375)
        )

        private val EAST_OUTLINE: VoxelShape = Shapes.or(
                Shapes.box(0.0625, 0.0, 0.0, 1.0, 0.3125, 1.0),
                Shapes.box(0.0625, 0.3125, 0.0, 0.25, 0.9375, 1.0),
                Shapes.box(0.0625, 0.3125, 0.0625, 0.9375, 0.875, 0.9375)
        )

        val CODEC: MapCodec<TMBlock> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                BlockBehaviour.Properties.CODEC.fieldOf("properties").forGetter { it.properties }
            ).apply(instance, ::TMBlock)
        }
    }

    init {
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false)
                .setValue(ON, false)
                .setValue(POWERED, false)
                .setValue(TRIGGERED, false))
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return TMBlockEntity(pos, state)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState()
                .setValue(FACING, context.horizontalDirection.opposite)
                .setValue(WATERLOGGED, context.level.getFluidState(context.clickedPos).type == Fluids.WATER)
                .setValue(ON, false)
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is TMBlockEntity) {
                player.openMenu(blockEntity)
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, WATERLOGGED, ON, TRIGGERED, POWERED)
    }


    override fun codec(): MapCodec<TMBlock> {
        return CODEC
    }

    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun getFluidState(state: BlockState): FluidState {
        return if (state.getValue(WATERLOGGED)) Fluids.WATER.defaultFluidState() else super.getFluidState(state)
    }

    override fun getShape(
            state: BlockState,
            getter: BlockGetter,
            pos: BlockPos,
            context: CollisionContext
    ): VoxelShape {
        return when (state.getValue(FACING)) {
            Direction.NORTH -> NORTH_OUTLINE
            Direction.SOUTH -> SOUTH_OUTLINE
            Direction.WEST -> WEST_OUTLINE
            Direction.EAST -> EAST_OUTLINE
            else -> Shapes.empty()
        }
    }

    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        movedByPiston: Boolean
    ) {
        Containers.dropContentsOnDestroy(state, newState, level, pos)
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        neighborBlock: Block,
        neighborPos: BlockPos,
        movedByPiston: Boolean
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston)
        checkPoweredState(level, pos, state)
    }

    private fun checkPoweredState(level: Level, pos: BlockPos, state: BlockState) {
        val nearbyPower = level.hasNeighborSignal(pos)
        if (nearbyPower != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, nearbyPower), 2)
            if (nearbyPower) {
                val tmBlockEntity = level.getBlockEntity(pos) as? TMBlockEntity
                tmBlockEntity?.autocraftTM()
            }
        }
    }
}
