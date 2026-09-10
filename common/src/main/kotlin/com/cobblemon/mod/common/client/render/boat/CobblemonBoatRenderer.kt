/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.boat

import net.minecraft.client.renderer.rendertype.RenderTypes

import com.cobblemon.mod.common.entity.boat.CobblemonBoatEntity
import com.cobblemon.mod.common.entity.boat.CobblemonBoatType
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.model.`object`.boat.BoatModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.EntityRenderer
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.cobblemon.mod.common.client.render.submitPosableModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import org.joml.Quaternionf

// PT144: EntityRenderer<T,S> requires EntityRenderState 2nd type-arg; render() removed (submit pipeline); BoatModel.setupAnim/waterPatch refactored.
class CobblemonBoatRenderer(ctx: EntityRendererProvider.Context, private val hasChest: Boolean) : EntityRenderer<CobblemonBoatEntity, net.minecraft.client.renderer.entity.state.EntityRenderState>(ctx) {

    private val boatModels = hashMapOf<CobblemonBoatType, Pair<Identifier, BoatModel>>()

    init {
        // PT134-DEFER: shadowRadius moved to EntityRenderState in MC 26.1.x — set in extractRenderState
        // this.shadowRadius = 0.8F
        CobblemonBoatType.values().forEach { type ->
            this.boatModels[type] = generateTextureIdentifier(type, this.hasChest) to generateBoatModel(ctx, type, this.hasChest)
        }
    }

    fun getTextureLocation(entity: CobblemonBoatEntity): Identifier = this.boatModels[entity.boatType]!!.first

    override fun createRenderState(): net.minecraft.client.renderer.entity.state.EntityRenderState = net.minecraft.client.renderer.entity.state.EntityRenderState()

    // PT144: EntityRenderer.render(Entity, Float, Float, PoseStack, SubmitNodeCollector, Int) removed in MC 26.1.x; deferred until submit pipeline migration.
    fun render_DEFER_NO_OVERRIDE(entity: CobblemonBoatEntity, yaw: Float, tickDelta: Float, matrices: PoseStack, vertexConsumers: SubmitNodeCollector, light: Int) {
        matrices.pushPose()
        matrices.translate(0F, 0.375F, 0F)
        matrices.mulPose(Axis.YP.rotationDegrees(180F - yaw))
        val h = entity.hurtTime - tickDelta
        val j = (entity.damage - tickDelta).coerceAtLeast(0F)
        if (h > 0F) {
            matrices.mulPose(Axis.XP.rotationDegrees(Mth.sin(h.toDouble()) * h * j / 10F * entity.hurtDir))
        }
        val k = entity.getBubbleAngle(tickDelta)
        if (!Mth.equal(k, 0F)) {
            matrices.mulPose(Quaternionf().setAngleAxis(entity.getBubbleAngle(tickDelta) * 0.017453292F, 1F, 0F, 1F))
        }
        val (identifier, entityModel) = this.boatModels[entity.boatType]!!
        matrices.scale(-1F, -1F, 1F)
        matrices.mulPose(Axis.YP.rotationDegrees(90F))
        // PT144: BoatModel.setupAnim(Entity, Float, Float, Float, Float, Float) deprecated — single-arg state form deferred.
        // entityModel.setupAnim(entity, tickDelta, 0F, -0.1F, 0F, 0F)
        vertexConsumers.submitPosableModel(matrices, entityModel.renderType(identifier)) { stack, consumer ->
            entityModel.renderToBuffer(stack, consumer, light, OverlayTexture.NO_OVERLAY, -0x1)
        }
        // PT144: BoatModel.waterPatch() removed in MC 26.1.x — water mask rendering deferred.
        // if (!entity.isUnderWater) {
        //     val vertexConsumer2 = vertexConsumers.getBuffer(RenderTypes.waterMask())
        //     entityModel.waterPatch().render(matrices, vertexConsumer2, light, OverlayTexture.NO_OVERLAY)
        // }
        matrices.popPose()
        // PT144: super.render removed - submit pipeline now drives rendering.
        // super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light)
    }

    companion object {

        private fun generateTextureIdentifier(type: CobblemonBoatType, hasChest: Boolean): Identifier {
            val boatSubPath = if (hasChest) "chest_boat" else "boat"
            val path = "textures/entity/$boatSubPath/${type.name.lowercase()}.png"
            return cobblemonResource(path)
        }

        private fun generateBoatModel(ctx: EntityRendererProvider.Context, type: CobblemonBoatType, hasChest: Boolean): BoatModel {
            val modelLayer = this.createBoatModelLayer(type, hasChest)
            val modelPart = ctx.bakeLayer(modelLayer)
            return if (hasChest) BoatModel(modelPart) else BoatModel(modelPart)
        }

        internal fun createBoatModelLayer(type: CobblemonBoatType, hasChest: Boolean): ModelLayerLocation {
            val boatSubPath = if (hasChest) "chest_boat" else "boat"
            val path = "$boatSubPath/${type.name.lowercase()}"
            return ModelLayerLocation(cobblemonResource(path), "main")
        }

    }

}