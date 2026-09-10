/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.item

import net.minecraft.client.renderer.rendertype.RenderTypes

import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.item.PokemonItem
import com.cobblemon.mod.common.util.math.fromEulerXYZDegrees
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.cobblemon.mod.common.client.render.submitPosableModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Quaternionf
import org.joml.Vector3f

class PokemonItemRenderer : CobblemonBuiltinItemRenderer {
    val context = RenderContext().also {
        it.put(RenderContext.RENDER_STATE, RenderContext.RenderState.PROFILE)
        it.put(RenderContext.DO_QUIRKS, false)
    }

    override fun render(stack: ItemStack, mode: ItemDisplayContext, matrices: PoseStack, vertexConsumers: SubmitNodeCollector, light: Int, overlay: Int) {
        val pokemonItem = stack.item as? PokemonItem ?: return
        val (species, aspects) = pokemonItem.getSpeciesAndAspects(stack) ?: return
        val state = FloatingState()
        state.currentAspects = aspects
        matrices.pushPose()
        val model = VaryingModelRepository.getPoser(species.resourceIdentifier, state)
        model.context = context
        context.put(RenderContext.RENDER_STATE, RenderContext.RenderState.PROFILE)
        context.put(RenderContext.SPECIES, species.resourceIdentifier)
        context.put(RenderContext.ASPECTS, aspects)
        context.put(RenderContext.POSABLE_STATE, state)
        state.currentModel = model

        val transformations = positions[mode]!!

        // PT144: Lighting.setupForFlatItems/setupFor3DItems removed in MC 26.1.x — Lighting now instance-managed via setupFor(Lighting.Entry).
        // GUI light context now driven by GuiGraphicsExtractor pipeline.
        if (mode == ItemDisplayContext.GUI) {
            // Lighting.setupForFlatItems() – stubbed pending render pipeline migration
        }

        matrices.scale(transformations.scale.x, transformations.scale.y, transformations.scale.z)
        matrices.translate(transformations.translation.x, transformations.translation.y, transformations.translation.z)
        state.setPoseToFirstSuitable(PoseType.PORTRAIT)
        model.applyAnimations(null, state, 0F, 0F, 0F, 0F, 0F)

        matrices.scale(model.profileScale, model.profileScale, 0.15F)

        val rotation = Quaternionf().fromEulerXYZDegrees(Vector3f(transformations.rotation.x, transformations.rotation.y, transformations.rotation.z))
        matrices.mulPose(rotation)
        rotation.conjugate()

        val renderLayer = RenderTypes.entityCutout(VaryingModelRepository.getTexture(species.resourceIdentifier, state))
        val isEnchanted = stack.get(DataComponents.ENCHANTMENTS)?.isEmpty == false

        matrices.pushPose()
        val packedLight = if (mode == ItemDisplayContext.GUI) {
            ((13) or ((13) shl 16))
        } else {
            light
        }

        // x = red, y = green, z = blue, w = alpha
        val tint = pokemonItem.tint(stack)
        model.withLayerContext(vertexConsumers, state, VaryingModelRepository.getLayers(species.resourceIdentifier, state)) {
            val tintRed = (tint.x * 255).toInt()
            val tintGreen = (tint.y * 255).toInt()
            val tintBlue = (tint.z * 255).toInt()
            val tintAlpha = (tint.w * 255).toInt()
            val color = (tintAlpha shl 24) or (tintRed shl 16) or (tintGreen shl 8) or tintBlue
            // port/26.2: VertexMultiConsumer is gone. It fed the same geometry to two buffers at once, so
            // an enchanted item submits the model a second time under the glint render type instead.
            vertexConsumers.submitPosableModel(matrices, renderLayer) { stack, consumer ->
                model.render(context, stack, consumer, packedLight, OverlayTexture.NO_OVERLAY, color)
            }
            if (isEnchanted) {
                vertexConsumers.submitPosableModel(matrices, RenderTypes.entityGlint()) { stack, consumer ->
                    model.render(context, stack, consumer, packedLight, OverlayTexture.NO_OVERLAY, color)
                }
            }
        }

        model.setDefault()
        matrices.popPose()
        matrices.popPose()

        // PT144: Lighting.setupFor3DItems removed in MC 26.1.x — stubbed pending render pipeline migration.
        if (mode == ItemDisplayContext.GUI) {
            // Lighting.setupFor3DItems() – stubbed pending render pipeline migration
        }
    }

    companion object {
        val positions: MutableMap<ItemDisplayContext, Transformations> = mutableMapOf()

        init {
            positions[ItemDisplayContext.GUI] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(1.0, -0.2, -0.5),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -3.5F),
                PokemonItemRenderer().Transformation(0F, 35F, 0F)
            )
            positions[ItemDisplayContext.FIXED] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(1.0, -0.5, -5.0),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -0.1F),
                PokemonItemRenderer().Transformation(0F, 35F - 180F, 0F)
            )
            positions[ItemDisplayContext.FIRST_PERSON_RIGHT_HAND] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(2.75, 0.0, 1.0),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -0.5F),
                PokemonItemRenderer().Transformation(0F, 35F, 0F)
            )
            positions[ItemDisplayContext.FIRST_PERSON_LEFT_HAND] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(-0.75, 0.0, 1.0),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -0.5F),
                PokemonItemRenderer().Transformation(0F, -35F, 0F)
            )
            positions[ItemDisplayContext.THIRD_PERSON_RIGHT_HAND] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(1.0, -1.0, -1.25),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -0.5F),
                PokemonItemRenderer().Transformation(0F, 35F, 0F)
            )
            positions[ItemDisplayContext.THIRD_PERSON_LEFT_HAND] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(1.0, -1.0, -1.25),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -0.5F),
                PokemonItemRenderer().Transformation(0F, -35F, 0F)
            )
            positions[ItemDisplayContext.GROUND] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(1.0, -0.5, -1.0),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -0.5F),
                PokemonItemRenderer().Transformation(0F, 35F, 0F)
            )
            positions[ItemDisplayContext.HEAD] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(1.0, -3.5, 3.0),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -0.5F),
                PokemonItemRenderer().Transformation(0F, 215F, 0F)
            )
            positions[ItemDisplayContext.NONE] = PokemonItemRenderer().Transformations(
                PokemonItemRenderer().Transformation(0.0, 0.0, 0.0),
                PokemonItemRenderer().Transformation(0.5F, -0.5F, -0.5F),
                PokemonItemRenderer().Transformation(0F, 0F, 0F)
            )
        }
    }

    inner class Transformations(val translation: Transformation<Double>, val scale: Transformation<Float>, val rotation: Transformation<Float>)
    inner class Transformation<T>(val x: T, val y: T, val z: T)
}