/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.block

import net.minecraft.client.renderer.rendertype.RenderTypes

import com.cobblemon.mod.common.api.berry.Berries
import com.cobblemon.mod.common.block.PokeSnackBlock
import com.cobblemon.mod.common.block.PokeSnackBlock.Companion.CANDLE
import com.cobblemon.mod.common.block.PokeSnackBlock.Companion.getCandleById
import com.cobblemon.mod.common.block.entity.PokeSnackBlockEntity
import com.cobblemon.mod.common.client.render.atlas.CobblemonAtlases.BERRY_SPRITE_ATLAS
import com.cobblemon.mod.common.client.render.models.blockbench.repository.BerryModelRepository
import com.cobblemon.mod.common.client.render.models.blockbench.setPosition
import com.cobblemon.mod.common.util.math.geometry.Axis
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import com.cobblemon.mod.common.client.render.atlas.CobblemonAtlases
import com.cobblemon.mod.common.client.render.blockStateModelOf
import com.cobblemon.mod.common.client.render.cutoutBlockSheet
import com.cobblemon.mod.common.client.render.submitBlockStateModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.phys.Vec3
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING
import net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT

/** port/26.2: the live block entity is carried on the state; the candle and berries are read off it. */
class PokeSnackRenderState : BlockEntityRenderState() {
    var entity: PokeSnackBlockEntity? = null
    var partialTicks: Float = 0F
}

open class PokeSnackBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) : BlockEntityRenderer<PokeSnackBlockEntity, PokeSnackRenderState> {
    override fun createRenderState(): PokeSnackRenderState = PokeSnackRenderState()

    override fun extractRenderState(
        entity: PokeSnackBlockEntity,
        state: PokeSnackRenderState,
        partialTick: Float,
        cameraPos: Vec3,
        crumbling: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        BlockEntityRenderState.extractBase(entity, state, crumbling)
        state.entity = entity
        state.partialTicks = partialTick
    }

    override fun submit(
        state: PokeSnackRenderState,
        poseStack: PoseStack,
        bufferSource: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val blockEntity = state.entity ?: return
        val packedLight = state.lightCoords
        val packedOverlay = OverlayTexture.NO_OVERLAY
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
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotationAngle))
        poseStack.translate(-0.5, -0.5, -0.5)

        val hasCandle = renderCandle(blockEntity, poseStack, bufferSource, packedLight, packedOverlay)
        renderBerries(blockEntity, poseStack, bufferSource, packedLight, packedOverlay, hasCandle)

        poseStack.popPose()
    }

    fun renderCandle(blockEntity: PokeSnackBlockEntity, poseStack: PoseStack, bufferSource: SubmitNodeCollector, packedLight: Int, packedOverlay: Int): Boolean {
        val candleId = blockEntity.blockState.getValue(CANDLE)

        if (candleId > 0) {
            // port/26.2: Minecraft.blockRenderer is gone; the candle's block state model comes from the
            // model manager and is submitted, restoring the visual the reference diff had dropped while
            // keeping the presence flag it still returned.
            bufferSource.submitBlockStateModel(
                blockStateModelOf(PokeSnackBlock.getCandleById(candleId).defaultBlockState()),
                poseStack,
                cutoutBlockSheet(),
                packedLight,
                packedOverlay
            )
            return true
        }
        return false
    }

    fun renderBerries(blockEntity: PokeSnackBlockEntity, poseStack: PoseStack, bufferSource: SubmitNodeCollector, packedLight: Int, packedOverlay: Int, hasCandle: Boolean) {
        val berryIngredients = (blockEntity.ingredientComponent?.ingredientIds?.take(3) ?: listOf())
            .mapNotNull { Berries.getByIdentifier(it) }

        berryIngredients.forEachIndexed { index, berry ->
            val model = BerryModelRepository.modelOf(berry.fruitModelIdentifier) ?: return

            when (berryIngredients.size) {
                1 -> {
                    val pos = berry.pokeSnackPositionings[0].position
                    val rot = berry.pokeSnackPositionings[0].rotation

                    if (hasCandle) {
                        model.setPosition(Axis.X_AXIS.ordinal, pos.x.toFloat())
                        model.setPosition(Axis.Y_AXIS.ordinal, pos.y.toFloat())
                        model.setPosition(Axis.Z_AXIS.ordinal, pos.z.toFloat())

                        model.setRotation(
                            Math.toRadians(rot.x).toFloat(),
                            Math.toRadians(rot.y).toFloat(),
                            Math.toRadians(rot.z).toFloat()
                        )
                    } else {
                        model.setPosition(Axis.X_AXIS.ordinal, 8F)
                        model.setPosition(Axis.Y_AXIS.ordinal, pos.y.toFloat())
                        model.setPosition(Axis.Z_AXIS.ordinal, 8F)

                        model.setRotation(
                            Math.toRadians(180.0).toFloat(),
                            Math.toRadians(0.0).toFloat(),
                            Math.toRadians(0.0).toFloat()
                        )
                    }
                }
                2 -> {
                    val pos = berry.pokeSnackPositionings[0].position
                    val rot = berry.pokeSnackPositionings[0].rotation
                    val isFirst = index == 0

                    model.setPosition(Axis.X_AXIS.ordinal, pos.x.toFloat())
                    model.setPosition(Axis.Y_AXIS.ordinal, pos.y.toFloat())
                    model.setPosition(Axis.Z_AXIS.ordinal, if (isFirst) pos.z.toFloat() else (16F - pos.z.toFloat()))

                    model.setRotation(
                        Math.toRadians(
                            if (isFirst) rot.x else (180.0 + (180.0 - rot.x))
                        ).toFloat(),
                        Math.toRadians(rot.y).toFloat(),
                        Math.toRadians(rot.z).toFloat()
                    )
                }
                else -> {
                    val pos = berry.pokeSnackPositionings[index].position
                    val rot = berry.pokeSnackPositionings[index].rotation

                    model.setPosition(Axis.X_AXIS.ordinal, pos.x.toFloat())
                    model.setPosition(Axis.Y_AXIS.ordinal, pos.y.toFloat())
                    model.setPosition(Axis.Z_AXIS.ordinal, pos.z.toFloat())

                    model.setRotation(
                        Math.toRadians(rot.x).toFloat(),
                        Math.toRadians(rot.y).toFloat(),
                        Math.toRadians(rot.z).toFloat()
                    )
                }
            }

            // port/26.2: the berry model is submitted against the berry atlas rather than drawn into a
            // buffer that the reference diff had hardcoded to null.
            bufferSource.submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutout(CobblemonAtlases.BERRY_SPRITE_ATLAS.textureAtlas.location(), false)
            ) { _, consumer ->
                model.render(poseStack, consumer, packedLight, packedOverlay)
            }
        }
    }
}
