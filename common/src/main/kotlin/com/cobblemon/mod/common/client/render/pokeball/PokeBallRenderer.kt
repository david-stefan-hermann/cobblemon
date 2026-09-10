/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.pokeball

import net.minecraft.client.renderer.rendertype.RenderTypes

import com.cobblemon.mod.common.client.entity.EmptyPokeBallClientDelegate
import com.cobblemon.mod.common.client.render.models.blockbench.pokeball.PosablePokeBallModel
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.cobblemon.mod.common.client.render.submitPosableModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.util.Mth
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
// PT144: ItemRenderer class restructured in MC 26.1.x — getFoilBufferDirect access via ItemRendererStub.
import com.cobblemon.mod.common.client.render.CobblemonItemRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier

/** port/26.2: the live entity is carried on the state so submit can reach its client delegate. */
class PokeBallRenderState : EntityRenderState() {
    var entity: EmptyPokeBallEntity? = null
    var partialTicks: Float = 0F
    /** EntityRenderState carries no yaw - only the living variant does - so it is captured here. */
    var yaw: Float = 0F
}

class PokeBallRenderer(context: EntityRendererProvider.Context) : EntityRenderer<EmptyPokeBallEntity, PokeBallRenderState>(context) {
    val model = PosablePokeBallModel()

    // PT144: getTextureLocation removed from EntityRenderer abstract — kept as non-override helper for legacy callers.
    fun getTextureLocation(pEntity: EmptyPokeBallEntity): Identifier {
        return VaryingModelRepository.getTexture(pEntity.pokeBall.name, pEntity.delegate as EmptyPokeBallClientDelegate)
    }

    override fun createRenderState(): PokeBallRenderState = PokeBallRenderState()

    override fun extractRenderState(entity: EmptyPokeBallEntity, state: PokeBallRenderState, partialTick: Float) {
        super.extractRenderState(entity, state, partialTick)
        state.entity = entity
        state.partialTicks = partialTick
        state.yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.yRot)
    }

    override fun submit(
        state: PokeBallRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val entity = state.entity ?: return
        val partialTicks = state.partialTicks
        val packedLight = state.lightCoords

        val ballState = entity.delegate as EmptyPokeBallClientDelegate
        this.model.context.put(RenderContext.POSABLE_STATE, ballState)
        this.model.context.put(RenderContext.ASPECTS, entity.aspects)
        ballState.currentAspects = entity.aspects
        val model = VaryingModelRepository.getPoser(entity.pokeBall.name, ballState)
        this.model.posableModel = model
        this.model.posableModel.context = this.model.context
        this.model.setupEntityTypeContext(entity)
        this.model.context.put(RenderContext.RENDER_STATE, RenderContext.RenderState.WORLD)

        poseStack.pushPose()
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw))
        poseStack.scale(0.7F, -0.7F, -0.7F)
        val textureLoc = VaryingModelRepository.getTexture(entity.pokeBall.name, ballState)
        ballState.updatePartialTicks(partialTicks)
        model.setLayerContext(collector, ballState, VaryingModelRepository.getLayers(entity.pokeBall.name, ballState))
        this.model.setupAnim(entity, 0f, 0f, entity.tickCount + partialTicks, 0F, 0F)
        // port/26.2: Model.renderToBuffer is final and draws the wrapper's empty root part; Cobblemon's
        // geometry hangs off posableModel, which renderToBufferLegacy draws.
        collector.submitPosableModel(poseStack, RenderTypes.entityCutout(textureLoc)) { stack, consumer ->
            this.model.renderToBufferLegacy(stack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -0x1)
        }

        model.green = 1F
        model.blue = 1F
        model.red = 1F
        model.resetLayerContext()

        poseStack.popPose()
    }
}
