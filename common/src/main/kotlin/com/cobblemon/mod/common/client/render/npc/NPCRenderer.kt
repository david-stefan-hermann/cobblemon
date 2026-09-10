/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.npc

import com.cobblemon.mod.common.client.entity.NPCClientDelegate
import com.cobblemon.mod.common.client.render.item.HeldItemRenderer
import com.cobblemon.mod.common.client.render.models.blockbench.PosableEntityModel
import com.cobblemon.mod.common.client.render.models.blockbench.npc.PosableNPCModel
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.entity.npc.NPCEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.cobblemon.mod.common.client.render.submitPosableModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.rendertype.RenderTypes
import com.mojang.math.Axis
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.resources.Identifier
import kotlin.math.min

/** port/26.2: the live entity is carried on the state so submit can reach its client delegate. */
class NPCRenderState : LivingEntityRenderState() {
    var entity: NPCEntity? = null
    var partialTicks: Float = 0F
}

class NPCRenderer(context: Context) : LivingEntityRenderer<NPCEntity, NPCRenderState, PosableEntityModel<NPCEntity>>(context, PosableNPCModel(), 0.5f) {
    override fun getTextureLocation(state: NPCRenderState): Identifier {
        return state.entity?.let { getTextureLocationForEntity(it) }
            ?: cobblemonResource("textures/entity/npc/missing.png")
    }

    fun getTextureLocationForEntity(entity: NPCEntity): Identifier {
        return VaryingModelRepository.getTexture(entity.resourceIdentifier, (entity.delegate as NPCClientDelegate))
    }

    private val heldItemRenderer = HeldItemRenderer()

    override fun createRenderState(): NPCRenderState = NPCRenderState()

    override fun extractRenderState(entity: NPCEntity, state: NPCRenderState, partialTick: Float) {
        super.extractRenderState(entity, state, partialTick)
        state.entity = entity
        state.partialTicks = partialTick
    }

    override fun submit(
        state: NPCRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val entity = state.entity ?: return
        currentRenderState = state
        render(entity, state.bodyRot, state.partialTicks, poseStack, collector, state.lightCoords)
    }

    private var currentRenderState: NPCRenderState? = null

    /** port/26.2: applied by hand now that LivingEntityRenderer.scale is state-based. */
    fun applyEntityScale(livingEntity: NPCEntity, poseStack: PoseStack, partialTickTime: Float) {
        poseStack.scale(livingEntity.renderScale, livingEntity.renderScale, livingEntity.renderScale)
    }

    private fun render(
        entity: NPCEntity,
        entityYaw: Float,
        partialTicks: Float,
        poseMatrix: PoseStack,
        collector: SubmitNodeCollector,
        packedLight: Int
    ) {
        val aspects = entity.aspects
        val clientDelegate = entity.delegate as NPCClientDelegate
        clientDelegate.currentAspects = aspects
        // PT134-DEFER: shadowRadius moved to EntityRenderState in MC 26.1.x — set in extractRenderState
        // shadowRadius = min((entity.boundingBox.maxX - entity.boundingBox.minX), (entity.boundingBox.maxZ) - (entity.boundingBox.minZ)).toFloat() / 1.5F
        val model = VaryingModelRepository.getPoser(entity.resourceIdentifier, clientDelegate)
        this.model.posableModel = model
        model.context = this.model.context
        this.model.setupEntityTypeContext(entity)
        this.model.context.put(RenderContext.TEXTURE, getTextureLocationForEntity(entity))
        clientDelegate.updatePartialTicks(partialTicks)

        model.setLayerContext(collector, clientDelegate, VaryingModelRepository.getLayers(entity.resourceIdentifier, clientDelegate))

        poseMatrix.pushPose()
        poseMatrix.scale(entity.npc.modelScale, entity.npc.modelScale, entity.npc.modelScale)
        // port/26.2: vanilla's own submission would draw the wrapper model's empty root, so the
        // transforms LivingEntityRenderer applied are reproduced and the real model is submitted.
        val st = currentRenderState
        this.model.setupAnim(
            entity,
            st?.walkAnimationPos ?: 0F,
            st?.walkAnimationSpeed ?: 0F,
            st?.ageInTicks ?: 0F,
            entityYaw,
            st?.xRot ?: 0F
        )
        poseMatrix.pushPose()
        poseMatrix.mulPose(Axis.YP.rotationDegrees(180F - entityYaw))
        poseMatrix.scale(-1F, -1F, 1F)
        applyEntityScale(entity, poseMatrix, partialTicks)
        poseMatrix.translate(0.0, -1.501, 0.0)
        collector.submitPosableModel(poseMatrix, RenderTypes.entityCutout(getTextureLocationForEntity(entity))) { stack, consumer ->
            this.model.renderToBufferLegacy(stack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -0x1)
        }
        poseMatrix.popPose()
        poseMatrix.popPose()
        model.red = 1F
        model.green = 1F
        model.blue = 1F
        model.resetLayerContext()

        if (entity.deathTime < 1) {
            //Render Held Item
            heldItemRenderer.renderOnModel(
                entity.mainHandItem,
                clientDelegate,
                poseMatrix,
                collector,
                packedLight,
                false,
                entity
            )
        }
    }

    // PT144: shouldShowName(Entity) removed — replaced by render-state-based visibility.
    fun shouldShowName_DEFER_NO_OVERRIDE(entity: NPCEntity): Boolean {
        if (entity.hideNameTag) {
            return false
        }
        return true
    }
}