/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.block

import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.block.entity.FossilAnalyzerBlockEntity
import com.cobblemon.mod.common.block.multiblock.FossilMultiblockStructure
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
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

/**
 * port/26.2: rendering is two-phase now, so the facing and the fossil inventory are copied out of the
 * block entity during extract - submit never sees the block entity, and the base state keeps its
 * blockState private.
 */
class FossilAnalyzerRenderState : BlockEntityRenderState() {
    var facing: Direction = Direction.SOUTH
    var fossils: List<ItemStack> = emptyList()
}

class FossilAnalyzerRenderer(ctx: BlockEntityRendererProvider.Context) : BlockEntityRenderer<FossilAnalyzerBlockEntity, FossilAnalyzerRenderState> {

    override fun createRenderState(): FossilAnalyzerRenderState = FossilAnalyzerRenderState()

    override fun extractRenderState(
        entity: FossilAnalyzerBlockEntity,
        state: FossilAnalyzerRenderState,
        partialTick: Float,
        cameraPos: Vec3,
        crumbling: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        BlockEntityRenderState.extractBase(entity, state, crumbling)
        val blockState = if (entity.level != null) entity.blockState
            else (CobblemonBlocks.FOSSIL_ANALYZER.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH) as BlockState)
        state.facing = blockState.getValue(HorizontalDirectionalBlock.FACING)
        // We shouldn't have to do any complex rendering when the block isn't a multiblock
        val struct = entity.multiblockStructure as? FossilMultiblockStructure
        state.fossils = struct?.fossilInventory?.toList() ?: emptyList()
    }

    override fun submit(
        state: FossilAnalyzerRenderState,
        matrices: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        if (state.fossils.isEmpty()) return

        val direction = state.facing
        val yRot = direction.toYRot() + if (direction == Direction.WEST || direction == Direction.EAST) 180F else 0F

        state.fossils.forEachIndexed { index, fossilStack ->
            matrices.pushPose()

            val dirOffset = when (direction) {
                Direction.NORTH -> Vec3(0.0, 0.0, 0.05)
                Direction.SOUTH -> Vec3(0.0, 0.0, -0.05)
                Direction.EAST -> Vec3(-0.05, 0.0, 0.0)
                Direction.WEST -> Vec3(0.05, 0.0, 0.0)
                else -> Vec3.ZERO
            }
            matrices.translate(0.5 + dirOffset.x, 0.4 + (index * 0.1) + dirOffset.y, 0.5 + dirOffset.z)
            matrices.mulPose(Axis.YP.rotationDegrees(yRot))
            matrices.mulPose(Axis.ZP.rotationDegrees(180F))
            matrices.mulPose(Axis.XP.rotationDegrees(90F))
            matrices.scale(0.7F, 0.7F, 0.7F)

            Minecraft.getInstance().itemRenderer.renderStatic(
                fossilStack,
                ItemDisplayContext.NONE,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                matrices,
                collector,
                Minecraft.getInstance().level,
                0
            )

            matrices.popPose()
        }
    }
}
