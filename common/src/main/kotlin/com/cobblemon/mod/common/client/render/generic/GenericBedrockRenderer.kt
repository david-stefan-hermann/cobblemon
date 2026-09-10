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
import com.cobblemon.mod.common.client.render.submitPosableModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.util.Mth
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture

/** port/26.2: the live entity and its yaw are carried on the state; EntityRenderState has neither. */
class GenericBedrockRenderState : EntityRenderState() {
    var entity: GenericBedrockEntity? = null
    var partialTicks: Float = 0F
    var yaw: Float = 0F
}

// getTextureLocation removed from EntityRenderer abstract — texture retrieval now state-driven via submitNode.
class GenericBedrockRenderer(context: EntityRendererProvider.Context) : EntityRenderer<GenericBedrockEntity, GenericBedrockRenderState>(context) {
    val model = PosableGenericEntityModel()
    fun getTextureLocation(entity: GenericBedrockEntity) = VaryingModelRepository.getTexture(entity.category, (entity.delegate as GenericBedrockClientDelegate))
    override fun createRenderState(): GenericBedrockRenderState = GenericBedrockRenderState()

    override fun extractRenderState(entity: GenericBedrockEntity, state: GenericBedrockRenderState, partialTick: Float) {
        super.extractRenderState(entity, state, partialTick)
        state.entity = entity
        state.partialTicks = partialTick
        state.yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.yRot)
    }

    override fun submit(
        state: GenericBedrockRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val entity = state.entity ?: return
        if (entity.isInvisible) {
            return
        }
        val partialTicks = state.partialTicks
        val packedLight = state.lightCoords

        val bedrockState = entity.delegate as GenericBedrockClientDelegate
        bedrockState.currentAspects = entity.aspects
        val model = VaryingModelRepository.getPoser(entity.category, bedrockState)
        this.model.posableModel = model
        model.context = this.model.context
        this.model.setupEntityTypeContext(entity)

        poseStack.pushPose()
        poseStack.scale(1.0F, -1.0F, 1.0F)
        poseStack.scale(entity.scale, entity.scale, entity.scale)
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw))

        bedrockState.updatePartialTicks(partialTicks)
        model.setLayerContext(collector, bedrockState, VaryingModelRepository.getLayers(entity.category, bedrockState))
        this.model.setupAnim(entity, 0f, 0f, entity.tickCount + partialTicks, 0F, 0F)
        // port/26.2: Model.renderToBuffer is final and draws the wrapper's empty root part.
        val texture = VaryingModelRepository.getTexture(entity.category, bedrockState)
        collector.submitPosableModel(poseStack, RenderTypes.entityCutout(texture)) { stack, consumer ->
            this.model.renderToBufferLegacy(stack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -0x1)
        }

        model.green = 1F
        model.blue = 1F
        model.red = 1F
        model.resetLayerContext()

        poseStack.popPose()
    }
}
