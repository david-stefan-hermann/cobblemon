/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.block

import com.cobblemon.mod.common.block.TMBlock
import com.cobblemon.mod.common.block.entity.TMBlockEntity
import com.cobblemon.mod.common.client.render.models.blockbench.repository.MiscModelRepository
import com.cobblemon.mod.common.util.cobblemonResource
import java.awt.Color
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher
import net.minecraft.client.renderer.MultiBufferSource
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.core.Direction
import net.minecraft.util.Mth

class TMBlockRenderer(context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<TMBlockEntity> {

    override fun render(
        entity: TMBlockEntity,
        tickDelta: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val diskModel = MiscModelRepository.modelOf(MODEL_ID) ?: return
        poseStack.pushPose()
        try {
            val tm = entity.tmmInventory.filterTM ?: return
            entity.partialTicks += tickDelta

            val color = Color(tm.elementalType.hue)
            when (entity.blockState.getValue(TMBlock.FACING)) {
                Direction.SOUTH -> poseStack.translate(15F / 16F, 5.5F / 16F, 1F / 16F)
                Direction.WEST -> poseStack.translate(14F / 16F, 5.5F / 16F, 0F)
                Direction.EAST -> poseStack.translate(1F, 5.5F / 16F, 0F)
                else -> poseStack.translate(15F / 16F, 5.5F / 16F, -1F / 16F)
            }

            poseStack.translate(-7.0 / 16f, 0.0, 8.0 / 16f)
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(entity.partialTicks / 0.5f))
            poseStack.translate(7.0 / 16f, 0.0, -8.0 / 16f)

            val colour = (255 shl 24) or (color.red shl 16) or (color.green shl 8) or color.blue

            val renderLayer = RenderType.entityCutout(cobblemonResource("textures/block/tm_machine.png"))
            diskModel.render(
                poseStack,
                bufferSource.getBuffer(renderLayer),
                packedLight,
                packedOverlay,
                colour
            )
        } finally {
            poseStack.popPose()
        }
    }

    companion object {
        val MODEL_ID = cobblemonResource("tm_disk.geo")
    }
}
