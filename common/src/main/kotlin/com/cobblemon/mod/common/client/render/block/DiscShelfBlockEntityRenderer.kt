/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.block

import com.cobblemon.mod.common.block.entity.DiscShelfBlockEntity
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

class DiscShelfBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) :
        BlockEntityRenderer<DiscShelfBlockEntity, net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState> {

    private val slotWidthPixels = 6
    private val slotHeightPixels = 2
    private val textureSize = 16f

    private val leftMargin = 1f
    private val rightMargin = 1f
    private val topMargin = 1f
    private val bottomMargin = 1f
    private val interRowSpacing = 0f
    private val tmOverlayDepthOffset = -0.0002f
    private val slotBrightnessMultiplier = 0.65f

    private val tmBaseTexture = cobblemonResource("textures/block/disc_shelf/technical_machine_base.png")
    private val tmOverlayTexture = cobblemonResource("textures/block/disc_shelf/technical_machine_overlay.png")

    // PT137: render_DEFER_NO_OVERRIDE removed (BlockAndTintGetter/TechnicalMachineItemColorProvider.getColor APIs removed in 26.1.x — function was not called)

    override fun createRenderState(): net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState =
        net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState()

    override fun submit(
        state: net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState,
        poseStack: com.mojang.blaze3d.vertex.PoseStack,
        collector: net.minecraft.client.renderer.SubmitNodeCollector,
        camera: net.minecraft.client.renderer.state.level.CameraRenderState
    ) { /* PT129-DEFER */ }
}
