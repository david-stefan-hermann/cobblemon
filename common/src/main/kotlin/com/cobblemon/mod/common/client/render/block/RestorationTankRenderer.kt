/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.block

import net.minecraft.client.renderer.rendertype.RenderTypes

import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.block.entity.RestorationTankBlockEntity
import com.cobblemon.mod.common.block.multiblock.FossilMultiblockStructure
import com.cobblemon.mod.common.client.CobblemonBakingOverrides
import com.cobblemon.mod.common.client.render.models.blockbench.fossil.FossilModel
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.client.render.models.blockbench.wavefunction.WaveFunction
import com.cobblemon.mod.common.client.render.models.blockbench.wavefunction.parabolaFunction
import com.cobblemon.mod.common.client.render.models.blockbench.wavefunction.rerange
import com.cobblemon.mod.common.client.render.models.blockbench.wavefunction.timeDilate
import com.cobblemon.mod.common.util.cobblemonResource
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import kotlin.math.pow
import com.cobblemon.mod.common.client.render.cutoutBlockSheet
import com.cobblemon.mod.common.client.render.submitBlockStateModel
import com.cobblemon.mod.common.client.render.submitPosableModel
import com.cobblemon.mod.common.client.render.translucentBlockSheet
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.block.BlockModelRenderState
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.util.RandomSource
import net.minecraft.world.phys.Vec3
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.Direction
import net.minecraft.world.level.block.HorizontalDirectionalBlock

class RestorationTankRenderer(ctx: BlockEntityRendererProvider.Context) : BlockEntityRenderer<RestorationTankBlockEntity, BlockEntityRenderState> {
    val context = RenderContext().also {
        it.put(RenderContext.DO_QUIRKS, true)
        it.put(RenderContext.RENDER_STATE, RenderContext.RenderState.RESURRECTION_MACHINE)
    }

    override fun createRenderState(): BlockEntityRenderState = BlockEntityRenderState()

    // port/26.2: the fetus model and the fluid models need the live block entity (the multiblock
    // structure, its fill level and animation clock), which the render state cannot carry, so the entity
    // is held here between extract and submit. A block entity renderer instance is per block entity
    // type, but extract and submit run back to back for one block entity at a time, so this is safe.
    private var currentEntity: RestorationTankBlockEntity? = null
    private var currentPartialTick: Float = 0F

    override fun extractRenderState(
        entity: RestorationTankBlockEntity,
        state: BlockEntityRenderState,
        partialTick: Float,
        cameraPos: Vec3,
        crumbling: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        BlockEntityRenderState.extractBase(entity, state, crumbling)
        currentEntity = entity
        currentPartialTick = partialTick
    }

    override fun submit(
        state: BlockEntityRenderState,
        matrices: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val entity = currentEntity ?: return
        val struct = entity.multiblockStructure as? FossilMultiblockStructure ?: return
        val light = state.lightCoords
        val overlay = OverlayTexture.NO_OVERLAY
        val tickDelta = currentPartialTick

        val connectionDir = struct.tankConnectorDirection
        // FYI, rendering models this way ignores the pivots set in the model, so set the pivots manually
        when (connectionDir) {
            Direction.NORTH -> matrices.rotateAround(Axis.YP.rotationDegrees(0f), 0.5f, 0f, 0.5f)
            Direction.EAST -> matrices.rotateAround(Axis.YP.rotationDegrees(270f), 0.5f, 0f, 0.5f)
            Direction.SOUTH -> matrices.rotateAround(Axis.YP.rotationDegrees(180f), 0.5f, 0f, 0.5f)
            Direction.WEST -> matrices.rotateAround(Axis.YP.rotationDegrees(90f), 0.5f, 0f, 0.5f)
            else -> {}
        }

        if (connectionDir != null) {
            matrices.pushPose()
            collector.submitBlockStateModel(CONNECTOR_OVERRIDE.getModel(), matrices, cutoutBlockSheet(), light, overlay)
            matrices.popPose()
        }

        val fillLevel = struct.fillLevel
        if (fillLevel == 0 && !struct.hasCreatedPokemon) {
            return
        }

        if (struct.isRunning() or (struct.hasCreatedPokemon)) {
            renderFetus(entity, tickDelta, matrices, collector, light, overlay)
        }

        matrices.pushPose()
        val fluidOverride = if (struct.isRunning()) FLUID_OVERRIDES[8]
            else if (struct.hasCreatedPokemon) FLUID_OVERRIDES[7]
            else FLUID_OVERRIDES[fillLevel.coerceAtMost(FLUID_OVERRIDES.size - 1) - 1]
        collector.submitBlockStateModel(fluidOverride.getModel(), matrices, translucentBlockSheet(), light, overlay)
        matrices.popPose()
    }

    private fun renderFetus(
        entity: RestorationTankBlockEntity,
        tickDelta: Float,
        matrices: PoseStack,
        collector: SubmitNodeCollector,
        light: Int,
        overlay: Int
    ) {
        val struc = entity.multiblockStructure as? FossilMultiblockStructure ?: return
        val fossil = struc.resultingFossil ?: return
        val timeRemaining = struc.timeRemaining

        val tankBlockState = entity.level?.getBlockState(entity.blockPos) ?: return
        if (tankBlockState.block != CobblemonBlocks.RESTORATION_TANK) {
            // Block has been destroyed/replaced
            return
        }
        val tankDirection = tankBlockState.getValue(HorizontalDirectionalBlock.FACING)
        val struct = entity.multiblockStructure as FossilMultiblockStructure
        val connectionDir = struct.tankConnectorDirection

        val state = struc.fossilState
        state.updatePartialTicks(tickDelta)
        state.currentAspects = struc.resultingFossil?.result?.aspects ?: emptySet()

        var fossilPoserId = fossil.identifier.withPath("${fossil.identifier.path}_fetus")

        val completionPercentage = (1 - timeRemaining / FossilMultiblockStructure.TIME_TO_TAKE.toFloat()).coerceIn(0F, 1F)
        val fossilFetusModel = VaryingModelRepository.getPoser(fossilPoserId, state) as? FossilModel ?: let {
            fossilPoserId = cobblemonResource("substitute_fetus")
            VaryingModelRepository.getPoser(fossilPoserId, state) as FossilModel
        }

        fossilFetusModel.context = context

        val embryo1Scale = EMBRYO_CURVE_1(completionPercentage)
        val embryo2Scale = EMBRYO_CURVE_2(completionPercentage)
        val embryo3Scale = EMBRYO_CURVE_3(completionPercentage)
        val fossilScale = FOSSIL_CURVE(completionPercentage)

        val identifiersAndScales = listOf(
            Pair(EMBRYO_IDENTIFIERS[0], embryo1Scale),
            Pair(EMBRYO_IDENTIFIERS[1], embryo2Scale),
            Pair(EMBRYO_IDENTIFIERS[2], embryo3Scale),
            Pair(fossilPoserId, fossilScale)
        )

        identifiersAndScales.forEach { (identifier, scale) ->
            val model = VaryingModelRepository.getPoser(identifier, state) as FossilModel
            val texture = VaryingModelRepository.getTexture(identifier, state)

            if (scale > 0F) {
                state.currentModel = model
                state.setPoseToFirstSuitable()

                val scale = if (timeRemaining == 0) {
                    model.maxScale
                } else {
                    scale * model.maxScale
                }

                matrices.pushPose()
                matrices.translate(0.5, 1.0 + fossilFetusModel.yTranslation,  0.5);
                matrices.mulPose(Axis.ZP.rotationDegrees(180F))
                if (tankDirection.getCounterClockWise(Direction.Axis.Y) == connectionDir) {
                    matrices.mulPose(Axis.YP.rotationDegrees(-90F))
                } else if (tankDirection == connectionDir) {
                    matrices.mulPose(Axis.YP.rotationDegrees(180F))
                } else if (tankDirection.opposite != connectionDir) {
                    matrices.mulPose(Axis.YP.rotationDegrees(90F))
                }

                model.context = context
                context.put(RenderContext.TEXTURE, texture)
                context.put(RenderContext.SPECIES, fossil.identifier)
                context.put(RenderContext.RENDER_STATE, RenderContext.RenderState.RESURRECTION_MACHINE)
                context.put(RenderContext.POSABLE_STATE, state)

                matrices.pushPose()
                matrices.scale(scale, scale, scale)
                matrices.translate(0.0f, model.yGrowthPoint.toFloat(), 0.0f)
                model.applyAnimations(
                    entity = null,
                    state = state,
                    headYaw = 0F,
                    headPitch = 0F,
                    limbSwing = 0F,
                    limbSwingAmount = 0F,
                    ageInTicks = state.animationSeconds * 20
                )
                collector.submitPosableModel(matrices, RenderTypes.entityCutout(texture)) { stack, consumer ->
                    model.render(context, stack, consumer, light, overlay, -0x1)
                    model.withLayerContext(collector, state, VaryingModelRepository.getLayers(fossilPoserId, state)) {
                        model.render(context, stack, consumer, light, OverlayTexture.NO_OVERLAY, -0x1)
                    }
                }
                model.setDefault()
                matrices.popPose()
                matrices.popPose()
            }

        }
    }

    companion object {
        // port/26.2: these hold the overrides rather than the baked models. Baking happens on every
        // resource reload, well after this companion initialises, so the model has to be looked up at
        // draw time - resolving here would pin nulls forever.
        val FLUID_OVERRIDES = listOf(
            CobblemonBakingOverrides.RESTORATION_TANK_FLUID_CHUNKED_1,
            CobblemonBakingOverrides.RESTORATION_TANK_FLUID_CHUNKED_2,
            CobblemonBakingOverrides.RESTORATION_TANK_FLUID_CHUNKED_3,
            CobblemonBakingOverrides.RESTORATION_TANK_FLUID_CHUNKED_4,
            CobblemonBakingOverrides.RESTORATION_TANK_FLUID_CHUNKED_5,
            CobblemonBakingOverrides.RESTORATION_TANK_FLUID_CHUNKED_6,
            CobblemonBakingOverrides.RESTORATION_TANK_FLUID_CHUNKED_7,
            CobblemonBakingOverrides.RESTORATION_TANK_FLUID_CHUNKED_8,
            CobblemonBakingOverrides.RESTORATION_TANK_FLUID_BUBBLING
        )

        val CONNECTOR_OVERRIDE = CobblemonBakingOverrides.RESTORATION_TANK_CONNECTOR

        val EMBRYO_IDENTIFIERS = listOf(
            cobblemonResource("embryo_stage1"),
            cobblemonResource("embryo_stage2"),
            cobblemonResource("embryo_stage3")
        )

        val EMBRYO_CURVE_1 = parabolaFunction(peak = 0.5F, period = 1F).rerange(0.0F, 0.8F).timeDilate(2.5F)
        val EMBRYO_CURVE_2 = parabolaFunction(peak = 0.9F, period = 1F).rerange(0.2F, 1.2F).timeDilate(2.5F)
        val EMBRYO_CURVE_3 = parabolaFunction(peak = 1F, period = 1F).rerange(0.6F, 1.4F).timeDilate(2.5F)
        val FOSSIL_CURVE: WaveFunction = { t: Float -> -0.4F * (t - 2.5F).pow(2) + 1F }.timeDilate(2.5F)

    }
}