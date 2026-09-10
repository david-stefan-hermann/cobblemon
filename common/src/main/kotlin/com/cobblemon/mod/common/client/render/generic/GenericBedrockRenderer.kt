/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.generic

import net.minecraft.client.renderer.rendertype.RenderTypes

import com.cobblemon.mod.common.client.entity.GenericBedrockClientDelegate
import com.cobblemon.mod.common.client.render.models.blockbench.generic.PosableGenericEntityModel
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.entity.generic.GenericBedrockEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture

// PT144: EntityRenderer<T,S> 2-type-args in MC 26.1.x; render→createRenderState/extractRenderState/submit flow.
// getTextureLocation removed from EntityRenderer abstract — texture retrieval now state-driven via submitNode.
class GenericBedrockRenderer(context: EntityRendererProvider.Context) : EntityRenderer<GenericBedrockEntity, net.minecraft.client.renderer.entity.state.EntityRenderState>(context) {
    val model = PosableGenericEntityModel()
    fun getTextureLocation(entity: GenericBedrockEntity) = VaryingModelRepository.getTexture(entity.category, (entity.delegate as GenericBedrockClientDelegate))
    fun render_DEFER_NO_OVERRIDE(entity: GenericBedrockEntity, yaw: Float, partialTicks: Float, poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int) {
        if (entity.isInvisible) {
            return
        }

        val state = entity.delegate as GenericBedrockClientDelegate
        state.currentAspects = entity.aspects
        val model = VaryingModelRepository.getPoser(entity.category, state)
        this.model.posableModel = model
        model.context = this.model.context
        this.model.setupEntityTypeContext(entity)
        poseStack.pushPose()
        // PT144: PoseStack.scale now requires (x,y,z) 3-arg in MC 26.1.x.
        poseStack.scale(1.0F, -1.0F, 1.0F)
        poseStack.scale(entity.scale, entity.scale, entity.scale)
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw))
        val vertexConsumer = buffer.getBuffer(RenderTypes.entityCutout(VaryingModelRepository.getTexture(entity.category, state)))

        state.updatePartialTicks(partialTicks)
        model.setLayerContext(buffer, state, VaryingModelRepository.getLayers(entity.category, state))
        this.model.setupAnim(entity, 0f, 0f, entity.tickCount + partialTicks, 0F, 0F)
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, -0x1)

        model.green = 1F
        model.blue = 1F
        model.red = 1F

        model.resetLayerContext()

        poseStack.popPose()
    }

    override fun createRenderState(): net.minecraft.client.renderer.entity.state.EntityRenderState =
        net.minecraft.client.renderer.entity.state.EntityRenderState()
}