/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.models.blockbench

import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.entity.PosableEntity
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.resources.Identifier

/**
 * A wrapping around a [PosableModel] that presents as an [EntityModel]. This is used to continue using
 * the [LivingEntityRenderer] system while still being able to use the [PosableModel] system. Subclasses
 * just lock in the type of [EntityModel].
 *
 * @author Hiroku
 * @since January 5th, 2024
 */
// PT144: EntityModel<T : EntityRenderState> bound tightened in MC 26.1.x; ModelPart constructor required; renderToBuffer final.
abstract class PosableEntityModel<T : Entity>(
    renderTypeFunc: (Identifier) -> RenderType = RenderTypes::entityCutout
) : EntityModel<net.minecraft.client.renderer.entity.state.EntityRenderState>(ModelPart(emptyList(), emptyMap()), renderTypeFunc) {
    val context: RenderContext = RenderContext().also {
        it.put(RenderContext.RENDER_STATE, RenderContext.RenderState.WORLD)
    }
    lateinit var posableModel: PosableModel

    // PT145: Model.renderToBuffer is final in MC 26.1.x — renamed to renderToBufferLegacy to avoid hide-conflict.
    fun renderToBufferLegacy(
        stack: PoseStack,
        buffer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int,
        color: Int
    ) {
        val entity = context.request(RenderContext.ENTITY)
        val overlay = getOverlayTexture(entity) ?: packedOverlay
        if (entity is LivingEntity) {
            // We're using the living entity renderer, ergo we need to apply a Y offset of 24 model units (1.5 blocks) for reasons only God can tell us.
            stack.translate(0.0, 1.5, 0.0)
        }
        posableModel.render(context, stack, buffer, packedLight, overlay, color)
        posableModel.setDefault()
    }

    open fun getOverlayTexture(entity: Entity?): Int? {
        return if (entity is LivingEntity) {
            OverlayTexture.pack(
                OverlayTexture.u(0F),
                OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0)
            )
        } else if (entity != null) {
            OverlayTexture.NO_OVERLAY
        } else {
            null
        }
    }

    // PT144: setupAnim(S) in MC 26.1.x — keep legacy multi-arg as helper used by per-entity logic; new flow keys off RenderState.
    fun setupAnim(
        entity: T,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        headYaw: Float,
        headPitch: Float
    ) {
        setupEntityTypeContext(entity)
        if (entity is PosableEntity) {
            val ticks = getTicksForAnimation(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch)
            posableModel.applyAnimations(entity, entity.delegate as PosableState, limbSwing, limbSwingAmount, ticks, headYaw, headPitch)
        }
    }

    /**
     * This is to support cases where the ageInTicks value that should go to the animator is not the same as the entity's
     * normal value. This can happen in the case of riding where it has been rendered in a different part of the pipeline
     * and we want to prevent stuttering / re-render it for a specific moment.
     */
    open fun getTicksForAnimation(
        entity: T,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        headYaw: Float,
        headPitch: Float
    ): Float = ageInTicks

    open fun setupEntityTypeContext(entity: Entity?) {
        entity?.let {
            context.put(RenderContext.ENTITY, entity)
            if (it is PosableEntity) {
                context.put(RenderContext.POSABLE_STATE, it.delegate as PosableState)
            }
        }
    }
}
