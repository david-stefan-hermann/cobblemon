/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.block

import com.cobblemon.mod.common.block.campfirepot.CampfireBlock
import com.cobblemon.mod.common.block.campfirepot.CampfirePotBlock
import com.cobblemon.mod.common.block.entity.CampfireBlockEntity
import com.cobblemon.mod.common.item.CampfirePotItem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import com.cobblemon.mod.common.client.render.itemRenderer
// import net.minecraft.client.renderer.ItemBlockRenderTypes — removed in MC 26.1.x
import com.cobblemon.mod.common.client.render.blockStateModelOf
import com.cobblemon.mod.common.client.render.cutoutBlockSheet
import com.cobblemon.mod.common.client.render.submitBlockStateModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.world.phys.Vec3
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.Direction
import net.minecraft.util.ARGB
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING
import org.joml.Vector3f
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** port/26.2: the live block entity is carried on the state; the pot, seasonings and broth need it. */
class CampfireRenderState : BlockEntityRenderState() {
    var entity: CampfireBlockEntity? = null
    var partialTicks: Float = 0F
}

class CampfireBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) : BlockEntityRenderer<CampfireBlockEntity, CampfireRenderState> {

    companion object {
        const val CIRCLE_RADIUS = 0.4F
        const val ROTATION_SPEED = 1.5F
        const val JUMP_AMPLITUDE = 0.025F
        const val JUMP_SPEED = 0.1F
    }

    override fun createRenderState(): CampfireRenderState = CampfireRenderState()

    override fun extractRenderState(
        entity: CampfireBlockEntity,
        state: CampfireRenderState,
        partialTick: Float,
        cameraPos: Vec3,
        crumbling: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        BlockEntityRenderState.extractBase(entity, state, crumbling)
        state.entity = entity
        state.partialTicks = partialTick
    }

    override fun submit(
        state: CampfireRenderState,
        poseStack: PoseStack,
        multiBufferSource: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val blockEntity = state.entity ?: return
        val tickDelta = state.partialTicks
        val light = state.lightCoords
        val overlay = OverlayTexture.NO_OVERLAY
        val campfirePotItem = blockEntity.getPotItem()?.item as CampfirePotItem?
        if (campfirePotItem == null) return

        poseStack.pushPose()

        val facing = blockEntity.blockState.getValue(FACING)
        val rotationAngle = when (facing) {
            Direction.NORTH -> 180F
            Direction.SOUTH -> 0F
            Direction.WEST -> 90F
            Direction.EAST -> -90F
            else -> 0f
        }

        poseStack.translate(0.5, 0.5, 0.5)
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationAngle))
        poseStack.translate(-0.5, -0.5, -0.5)

        renderPot(campfirePotItem, blockEntity, tickDelta, poseStack, multiBufferSource, light, overlay)

        renderSeasonings(blockEntity, tickDelta, poseStack, multiBufferSource, light, overlay)

        poseStack.popPose()
    }

    private fun renderPot(
        campfirePotItem: CampfirePotItem,
        blockEntity: CampfireBlockEntity,
        tickDelta: Float,
        poseStack: PoseStack,
        multiBufferSource: SubmitNodeCollector,
        light: Int,
        overlay: Int
    ) {
        val isLidOpen = !blockEntity.blockState.getValue(CampfireBlock.LID)

        val yRot = (blockEntity.blockState.getValue(CampfireBlock.ITEM_DIRECTION).opposite.toYRot() + blockEntity.blockState.getValue(FACING).toYRot()) % 360

        poseStack.pushPose()
        poseStack.translate(0.0, 0.4375, 0.0)

        // port/26.2: Minecraft.blockRenderer and its modelRenderer are gone. The pot's block state model
        // is looked up from the model manager and submitted, with the broth colour passed as a packed
        // ARGB tint where the old renderer took separate red/green/blue floats.
        val potState = campfirePotItem.block.defaultBlockState()
            .setValue(CampfirePotBlock.OPEN, isLidOpen)
            .setValue(FACING, Direction.fromYRot(yRot.toDouble()))
            .setValue(CampfirePotBlock.OCCUPIED, (!blockEntity.getSeasonings().isEmpty() || !blockEntity.getIngredients().isEmpty()))
        multiBufferSource.submitBlockStateModel(
            blockStateModelOf(potState),
            poseStack,
            cutoutBlockSheet(),
            light,
            overlay,
            intArrayOf(blockEntity.brothColor or (0xFF shl 24))
        )

        poseStack.popPose()
    }

    private fun renderSeasonings(
        blockEntity: CampfireBlockEntity,
        tickDelta: Float,
        poseStack: PoseStack,
        multiBufferSource: SubmitNodeCollector,
        light: Int,
        overlay: Int
    ) {
        val seasonings = blockEntity.getSeasonings()

        val gameTime = blockEntity.time + tickDelta
        val rotationAngle = (gameTime * ROTATION_SPEED) % 360

        seasonings.forEachIndexed { index, seasoning ->
            poseStack.pushPose()
            poseStack.scale(0.5F, 0.5F, 0.5F)

            val angleOffset = index * (360f / seasonings.size)
            val angleInRadians = Math.toRadians((rotationAngle + angleOffset).toDouble())

            val xOffset = cos(angleInRadians.toDouble()).toFloat() * CIRCLE_RADIUS
            val zOffset = sin(angleInRadians.toDouble()).toFloat() * CIRCLE_RADIUS
            val jumpOffset = sin(gameTime * JUMP_SPEED + index * 2) * JUMP_AMPLITUDE
            poseStack.translate(1F + xOffset, 1.24F + jumpOffset, 1F + zOffset)
            val lookAtDirection = Vector3f(1f + xOffset - 1F, 0F, 1F + zOffset - 1F)
            poseStack.mulPose(Axis.YP.rotationDegrees((-Math.toDegrees(
                atan2(
                    lookAtDirection.z().toDouble(),
                    lookAtDirection.x().toDouble()
                )
            )).toFloat() + 90))

            poseStack.pushPose()
            poseStack.mulPose(Axis.XP.rotationDegrees(22.5F))

            Minecraft.getInstance().itemRenderer.renderStatic(
                seasoning,
                ItemDisplayContext.GROUND,
                light,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                multiBufferSource,
                blockEntity.level,
                0
            )

            poseStack.popPose()
            poseStack.popPose()
        }
    }

    private fun drawQuad(
        builder: VertexConsumer,
        poseStack: PoseStack,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        u0: Float, v0: Float, u1: Float, v1: Float,
        packedLight: Int, color: Int
    ) {
        drawVertex(builder, poseStack, x0, y0, z0, u0, v0, packedLight, color)
        drawVertex(builder, poseStack, x0, y1, z1, u0, v1, packedLight, color)
        drawVertex(builder, poseStack, x1, y1, z1, u1, v1, packedLight, color)
        drawVertex(builder, poseStack, x1, y0, z0, u1, v0, packedLight, color)
    }

    private fun drawVertex(
        builder: VertexConsumer,
        poseStack: PoseStack,
        x: Float, y: Float, z: Float,
        u: Float, v: Float,
        packedLight: Int, color: Int
    ) {
        builder.addVertex(poseStack.last().pose(), x, y, z)
            .setColor(color)
            .setUv(u, v)
            .setLight(packedLight)
            .setNormal(0f, 1f, 0f)
    }
}
