/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.block

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.ModAPI

import com.cobblemon.mod.common.block.entity.LecternBlockEntity
import com.cobblemon.mod.common.item.PokedexItem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import com.cobblemon.mod.common.client.render.itemRenderer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.phys.Vec3
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.item.*
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.Unique

/** port/26.2: the displayed stack and the lectern's facing are captured during extract. */
class LecternRenderState : BlockEntityRenderState() {
    var stack: ItemStack = ItemStack.EMPTY
    var yRot: Float = 0F
}

class LecternBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) : BlockEntityRenderer<LecternBlockEntity, LecternRenderState> {

    // TODO port/26.2: upstream draws a Pokedex on the lectern with a flat model variant ("flat" while a
    // viewer is present, "flat_off" otherwise) instead of the normal item model. That went through
    // ItemRenderer.itemModelShaper, which 26.2 removed; the replacement is the extra-model route that
    // BakingOverride also needs and which is not ported yet. Until then a Pokedex renders with its normal
    // model here - visually wrong, but present rather than missing.

    override fun createRenderState(): LecternRenderState = LecternRenderState()

    override fun extractRenderState(
        blockEntity: LecternBlockEntity,
        state: LecternRenderState,
        partialTick: Float,
        cameraPos: Vec3,
        crumbling: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        BlockEntityRenderState.extractBase(blockEntity, state, crumbling)
        state.stack = if (blockEntity.isEmpty()) ItemStack.EMPTY else blockEntity.getItemStack()
        val blockState = if (blockEntity.level != null) blockEntity.blockState
            else (CobblemonBlocks.CAMPFIRE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH) as BlockState)
        state.yRot = blockState.getValue(HorizontalDirectionalBlock.FACING).toYRot()
    }

    override fun submit(
        state: LecternRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        if (state.stack.isEmpty) return

        poseStack.pushPose()
        poseStack.translate(0.5, 1.17, 0.5)
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.yRot))
        poseStack.mulPose(Axis.XP.rotationDegrees(22.5F))
        poseStack.translate(0.0, 0.0, 0.13)

        Minecraft.getInstance().itemRenderer.renderStatic(
            state.stack,
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
}
