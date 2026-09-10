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
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
// PT144: ItemRenderer class restructured in MC 26.1.x — getFoilBufferDirect access via ItemRendererStub.
import com.cobblemon.mod.common.client.render.CobblemonItemRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier

// PT144: EntityRenderer<T,S> 2-type-args in MC 26.1.x; render→createRenderState/extractRenderState/submit flow.
class PokeBallRenderer(context: EntityRendererProvider.Context) : EntityRenderer<EmptyPokeBallEntity, net.minecraft.client.renderer.entity.state.EntityRenderState>(context) {
    val model = PosablePokeBallModel()

    // PT144: getTextureLocation removed from EntityRenderer abstract — kept as non-override helper for legacy callers.
    fun getTextureLocation(pEntity: EmptyPokeBallEntity): Identifier {
        return VaryingModelRepository.getTexture(pEntity.pokeBall.name, pEntity.delegate as EmptyPokeBallClientDelegate)
    }

    fun render_DEFER_NO_OVERRIDE(entity: EmptyPokeBallEntity, yaw: Float, partialTicks: Float, poseStack: PoseStack, buffer: MultiBufferSource, packedLight: Int) {
        val state = entity.delegate as EmptyPokeBallClientDelegate
        this.model.context.put(RenderContext.POSABLE_STATE, state)
        this.model.context.put(RenderContext.ASPECTS, entity.aspects)
        state.currentAspects = entity.aspects
        val model = VaryingModelRepository.getPoser(entity.pokeBall.name, state)
        this.model.posableModel = model
        this.model.posableModel.context = this.model.context
        this.model.setupEntityTypeContext(entity)
        this.model.context.put(RenderContext.RENDER_STATE, RenderContext.RenderState.WORLD)
        poseStack.pushPose()
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw))
        poseStack.scale(0.7F, -0.7F, -0.7F)
        val textureLoc = VaryingModelRepository.getTexture(entity.pokeBall.name, state)
        // PT144: ItemRenderer.getFoilBufferDirect removed in MC 26.1.x — use direct buffer fallback.
        val vertexConsumer = buffer.getBuffer(RenderTypes.entityCutout(textureLoc))
        state.updatePartialTicks(partialTicks)
        model.setLayerContext(buffer, state, VaryingModelRepository.getLayers(entity.pokeBall.name, state))
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