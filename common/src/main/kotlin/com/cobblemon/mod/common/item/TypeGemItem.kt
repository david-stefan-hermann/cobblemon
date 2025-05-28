package com.cobblemon.mod.common.item

import com.cobblemon.mod.common.block.TypeGemBlock
import com.cobblemon.mod.common.block.TypeGemClusterBlock
import com.cobblemon.mod.common.block.TypeGemCoreBlock
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock

class TypeGemItem(
    private val clusterBlock: Block,
    properties: Properties
) : Item(properties) {

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val clickedPos = context.clickedPos
        val clickedBlock = level.getBlockState(clickedPos).block

        // Only place the cluster on TypeGemCore or TypeGemBlock
        if (clickedBlock is TypeGemCoreBlock || clickedBlock is TypeGemBlock) {
            val placePos = clickedPos.relative(context.clickedFace)

            if (level.isEmptyBlock(placePos) || level.getBlockState(placePos).canBeReplaced()) {
                val player = context.player
                val itemInHand = context.itemInHand

                if (!level.isClientSide) {
                    val facing = context.clickedFace
                    val state = clusterBlock.defaultBlockState()
                        .setValue(DirectionalBlock.FACING, facing)
                        .setValue(TypeGemClusterBlock.STAGE, 0)
                        .setValue(TypeGemClusterBlock.SHOULD_GROW, true)

                    level.setBlock(placePos, state, 3)

                    if (player == null || !player.isCreative) {
                        itemInHand.shrink(1)
                    }
                }

                return InteractionResult.sidedSuccess(level.isClientSide)
            }
        }

        return InteractionResult.PASS
    }
}
