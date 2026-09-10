/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.block

import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.block.entity.HealingMachineBlockEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import com.cobblemon.mod.common.client.render.itemRenderer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

@Suppress("UNUSED_PARAMETER")
/**
 * port/26.2: the poke ball stacks and the machine's facing are captured during extract; submit draws
 * from this state alone.
 */
class HealingMachineRenderState : BlockEntityRenderState() {
    var yRot: Float = 0F
    /** Ball stacks paired with the slot index they sit in, so the offsets still line up. */
    var pokeBalls: List<Pair<Int, ItemStack>> = emptyList()
}

@Suppress("UNUSED_PARAMETER")
class HealingMachineRenderer<T: BlockEntity>(ctx: BlockEntityRendererProvider.Context): BlockEntityRenderer<T, HealingMachineRenderState> {
    companion object {
        private val offsets = listOf(
            0.2 to 0.385,
            -0.2 to 0.385,
            0.2 to 0.0,
            -0.2 to 0.0,
            0.2 to -0.385,
            -0.2 to -0.385
        )
    }

    override fun createRenderState(): HealingMachineRenderState = HealingMachineRenderState()

    override fun extractRenderState(
        blockEntity: T,
        state: HealingMachineRenderState,
        partialTick: Float,
        cameraPos: Vec3,
        crumbling: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        BlockEntityRenderState.extractBase(blockEntity, state, crumbling)
        if (blockEntity !is HealingMachineBlockEntity) {
            state.pokeBalls = emptyList()
            return
        }
        val blockState = if (blockEntity.level != null) blockEntity.blockState
            else (CobblemonBlocks.HEALING_MACHINE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH) as BlockState)
        state.yRot = blockState.getValue(HorizontalDirectionalBlock.FACING).toYRot()
        state.pokeBalls = blockEntity.pokeBalls().map { (index, pokeBall) -> index to pokeBall.stack() }
    }

    override fun submit(
        state: HealingMachineRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        if (state.pokeBalls.isEmpty()) return

        poseStack.pushPose()

        // Position Poke Balls
        poseStack.translate(0.5, 0.5, 0.5)
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot))
        poseStack.scale(0.65F, 0.65F, 0.65F)

        state.pokeBalls.forEach { (index, stack) ->
            poseStack.pushPose()
            val offset = offsets[index]
            poseStack.translate(offset.first, 0.4, offset.second)
            Minecraft.getInstance().itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                collector,
                Minecraft.getInstance().level,
                0
            )
            poseStack.popPose()
        }
        poseStack.popPose()
    }
}
