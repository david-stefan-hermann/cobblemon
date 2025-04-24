package com.cobblemon.mod.common.block

import com.cobblemon.mod.common.block.entity.TMShelfBlockEntity
import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.VoxelShape

class TMShelfBlock(properties: Properties) : BaseEntityBlock(properties) {
    companion object {
        val CODEC: MapCodec<TMShelfBlock> = simpleCodec(::TMShelfBlock)
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING
        val SLOT_OCCUPIED_PROPERTIES = (0 until 14).map {
            BooleanProperty.create("slot_${it}_occupied")
        }
    }

    init {
        val baseState = stateDefinition.any().setValue(FACING, Direction.NORTH)
        registerDefaultState(SLOT_OCCUPIED_PROPERTIES.fold(baseState) { state, prop ->
            state.setValue(prop, false)
        })
    }

    override fun codec() = CODEC

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return TMShelfBlockEntity(pos, state)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
        SLOT_OCCUPIED_PROPERTIES.forEach { builder.add(it) }
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(FACING, ctx.horizontalDirection.opposite)
    }

    override fun useWithoutItem(
        state: BlockState, level: Level, pos: BlockPos,
        player: Player, hit: BlockHitResult
    ): InteractionResult {
        val entity = level.getBlockEntity(pos) as? TMShelfBlockEntity ?: return InteractionResult.PASS
        return entity.handleUseWithoutItem(state, level, pos, player, hit)
    }

    override fun useItemOn(
        stack: ItemStack, state: BlockState, level: Level, pos: BlockPos,
        player: Player, hand: InteractionHand, hit: BlockHitResult
    ): ItemInteractionResult {
        val entity = level.getBlockEntity(pos) as? TMShelfBlockEntity ?: return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
        return entity.handleUseItem(stack, state, level, pos, player, hit)
    }

    override fun hasAnalogOutputSignal(state: BlockState) = true

    override fun getAnalogOutputSignal(state: BlockState, level: Level, pos: BlockPos): Int {
        val entity = level.getBlockEntity(pos) as? TMShelfBlockEntity
        return entity?.lastInteractedSlot?.plus(1) ?: 0
    }

    override fun getShape(
        state: BlockState, level: BlockGetter,
        pos: BlockPos, context: net.minecraft.world.phys.shapes.CollisionContext
    ): VoxelShape = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)
}
