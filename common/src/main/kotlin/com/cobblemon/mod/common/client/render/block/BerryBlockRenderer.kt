/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.block

import net.minecraft.client.renderer.rendertype.RenderTypes

import com.cobblemon.mod.common.api.mulch.MulchVariant
import com.cobblemon.mod.common.block.BerryBlock
import com.cobblemon.mod.common.block.entity.BerryBlockEntity
import com.cobblemon.mod.common.client.CobblemonBakingOverrides
import com.cobblemon.mod.common.client.render.atlas.CobblemonAtlases.BERRY_SPRITE_ATLAS
import com.cobblemon.mod.common.client.render.models.blockbench.repository.BerryModelRepository
import com.cobblemon.mod.common.client.render.models.blockbench.setPosition
import com.cobblemon.mod.common.util.math.geometry.Axis
import com.cobblemon.mod.common.util.toVec3d
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3


class BerryBlockRenderer(private val context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<BerryBlockEntity, net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState> {

    val mulchModels = mutableMapOf(
        MulchVariant.COARSE to CobblemonBakingOverrides.COARSE_MULCH,
        MulchVariant.GROWTH to CobblemonBakingOverrides.GROWTH_MULCH,
        MulchVariant.HUMID to CobblemonBakingOverrides.HUMID_MULCH,
        MulchVariant.LOAMY to CobblemonBakingOverrides.LOAMY_MULCH,
        MulchVariant.PEAT to CobblemonBakingOverrides.PEAT_MULCH,
        MulchVariant.RICH to CobblemonBakingOverrides.RICH_MULCH,
        MulchVariant.SANDY to CobblemonBakingOverrides.SANDY_MULCH,
        MulchVariant.SURPRISE to CobblemonBakingOverrides.SURPRISE_MULCH,
        MulchVariant.NONE to null
    )


    override fun shouldRender(blockEntity: BerryBlockEntity, pos: Vec3): Boolean {
        // PT144: LevelRenderer.cullingFrustum field privatized in MC 26.1.x. Fallback to base-class default culling.
        return super.shouldRender(blockEntity, pos)
    }

    fun render_DEFER_NO_OVERRIDE(entity: BerryBlockEntity, tickDelta: Float, matrices: PoseStack, vertexConsumers: MultiBufferSource, light: Int, overlay: Int) {
        if (!shouldRender(entity, entity.blockPos.toVec3d())) return
        val blockState = entity.blockState
        if (entity.renderState == null) {
            entity.renderState = BerryBlockEntityRenderState()
        }

        // PT144: RenderTypes.entityCutoutNoCull removed in MC 26.1.x → entityCutout(id, false) no-cull variant.
        // TextureAtlas.identifier() renamed to location().
        val buffer = vertexConsumers.getBuffer(
            RenderTypes.entityCutout(BERRY_SPRITE_ATLAS.textureAtlas.location(), false)
        )

        val renderState = entity.renderState as BerryBlockEntityRenderState
//        if (renderState.needsRebuild || renderState.vboLightLevel != light) {
            renderToBuffer(entity, matrices, light, overlay, renderState, buffer)
//            renderState.vboLightLevel = light
//            (entity.renderState as BerryBlockEntityRenderState).needsRebuild = false
//        }
//        if (renderState.drawVbo) {
//            matrices.pushPose()
//            val mtx = RenderSystem.getModelViewMatrix().get(Matrix4f())
//            CobblemonRenderLayers.BERRY_LAYER.setupRenderState()
//            renderState.vbo.bind()
//            renderState.vbo.drawWithShader(
//                mtx.mul(matrices.last().pose()),
//                RenderSystem.getProjectionMatrix(),
//                GameRenderer.getRendertypeCutoutShader()!!
//            )
//            VertexBuffer.unbind()
//            CobblemonRenderLayers.BERRY_LAYER.clearRenderState()
//            matrices.popPose()
//        }
        drawMulch(matrices, vertexConsumers, entity, light, overlay)
    }

    private fun drawMulch(
        matrices: PoseStack,
        vertexConsumers: MultiBufferSource,
        entity: BerryBlockEntity,
        light: Int,
        overlay: Int
    ) {
        matrices.pushPose()
        //Mulch is rendered on a different layer than the actual berries so
        val mulchBuf = vertexConsumers.getBuffer(net.minecraft.client.renderer.Sheets.cutoutBlockSheet())
        val model = mulchModels[entity.mulchVariant]
        // PT136-DEFER: BlockModel.getQuads + VertexConsumer.putBulkData removed in MC 26.1.x — needs SectionMesher/RenderType refactor
        model?.let { /* mulch quads disabled until model API migrated */ }
        matrices.popPose()
    }


    fun renderToBuffer(entity: BerryBlockEntity, matrices: PoseStack, light: Int, overlay: Int, renderState: BerryBlockEntityRenderState, buffer: VertexConsumer) {
        if (entity.blockState.getValue(BerryBlock.AGE) == 0) {
            renderBabyToBuffer(entity, matrices, light, overlay, buffer)
            renderState.drawVbo = true
        }
        else if (entity.blockState.getValue(BerryBlock.AGE) > BerryBlock.MATURE_AGE){
            renderAdultToBuffer(entity, matrices, light, overlay, buffer)
            renderState.drawVbo = true
        }
        else {
            renderState.drawVbo = false
        }
    }

    fun renderBabyToBuffer(entity: BerryBlockEntity, matrices: PoseStack, light: Int, overlay: Int, buffer: VertexConsumer) {
//        val bufferBuilder = Tesselator.getInstance().begin(CobblemonRenderLayers.BERRY_LAYER.mode(), CobblemonRenderLayers.BERRY_LAYER.format())
        val berry = entity.berry() ?: return
        val model = BerryModelRepository.modelOf(berry.fruitModelIdentifier) ?: return
        val pos = berry.stageOnePositioning.position
        model.setPosition(Axis.X_AXIS.ordinal, pos.x.toFloat())
        model.setPosition(Axis.Y_AXIS.ordinal, pos.y.toFloat())
        model.setPosition(Axis.Z_AXIS.ordinal, pos.z.toFloat())
        val rot = berry.stageOnePositioning.rotation
        model.setRotation(
            Math.toRadians(180 - rot.x).toFloat(),
            Math.toRadians(180 + rot.y).toFloat(),
            Math.toRadians(rot.z).toFloat()
        )
        model.render(matrices, buffer, light, overlay)
//        val bufferBuilderFinal = bufferBuilder.buildOrThrow()
//        buffer.bind()
//        buffer.upload(bufferBuilderFinal)
//        VertexBuffer.unbind()
    }

    fun renderAdultToBuffer(entity: BerryBlockEntity, matrices: PoseStack, light: Int, overlay: Int, buffer: VertexConsumer) {
        val blockState = entity.blockState
        val age = blockState.getValue(BerryBlock.AGE)
        if (age <= BerryBlock.MATURE_AGE) {
            return
        }
        val isFlower = age == BerryBlock.FLOWER_AGE
//        val bufferBuilder = Tesselator.getInstance().begin(CobblemonRenderLayers.BERRY_LAYER.mode(), CobblemonRenderLayers.BERRY_LAYER.format())
        for ((berry, growthPoint) in entity.berryAndGrowthPoint()) {
            val model = (if (isFlower) BerryModelRepository.modelOf(berry.flowerModelIdentifier) else BerryModelRepository.modelOf(berry.fruitModelIdentifier)) ?: continue
//            matrices.translate(growthPoint.position.x.toFloat() / 16F, growthPoint.position.y.toFloat() / 16F, growthPoint.position.z.toFloat() / 16F)
//            matrices.mulPose(com.mojang.math.Axis.XP.rotationDegrees(growthPoint.rotation.x.toFloat()))
//            matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(growthPoint.rotation.y.toFloat()))
//            matrices.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(growthPoint.rotation.z.toFloat()))

            model.setRotation(
                Math.toRadians(180.0 - growthPoint.rotation.x).toFloat(),
                Math.toRadians(180.0 + growthPoint.rotation.y).toFloat(),
                Math.toRadians(growthPoint.rotation.z).toFloat()
            )
            model.setPosition(Axis.X_AXIS.ordinal, growthPoint.position.x.toFloat())
            model.setPosition(Axis.Y_AXIS.ordinal, growthPoint.position.y.toFloat())
            model.setPosition(Axis.Z_AXIS.ordinal, growthPoint.position.z.toFloat())
            model.render(matrices, buffer, light, overlay)
        }
//        val bufferBuilderFinal = bufferBuilder.buildOrThrow()
//        buffer.bind()
//        buffer.upload(bufferBuilderFinal)
//        VertexBuffer.unbind()
    }


    override fun createRenderState(): net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState =
        net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState()

    override fun submit(
        state: net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState,
        poseStack: com.mojang.blaze3d.vertex.PoseStack,
        collector: net.minecraft.client.renderer.SubmitNodeCollector,
        camera: net.minecraft.client.renderer.state.level.CameraRenderState
    ) { /* PT129-DEFER */ }
}
