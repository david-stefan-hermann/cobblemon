/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.item

import com.cobblemon.mod.common.api.tags.CobblemonItemTags
import com.cobblemon.mod.common.api.tags.CobblemonItemTags.WEARABLE_FACE_ITEMS
import com.cobblemon.mod.common.api.tags.CobblemonItemTags.WEARABLE_HAT_ITEMS
import com.cobblemon.mod.common.client.entity.NPCClientDelegate
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import com.cobblemon.mod.common.client.render.itemRenderer
import com.cobblemon.mod.common.client.render.ItemRendererStub
import net.minecraft.client.renderer.MultiBufferSource
// PT128: ItemRenderer class removed in MC 26.1 — use ItemRendererStub.
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Vector3f

class HeldItemRenderer {
    private val itemRenderer: ItemRendererStub = Minecraft.getInstance().itemRenderer
    private var displayContext: ItemDisplayContext = ItemDisplayContext.FIXED

    companion object {
        const val ITEM_FACE = "item_face"
        const val ITEM_HAT = "item_hat"
        const val ITEM = "item"

        private fun PoseStack.setOriginAndScale(x:Float,y:Float,z:Float,scale:Float=1f) {
            this.translate(x*.0625*scale, y*.0625*scale, z*.0625*scale)
            this.scale(scale, scale, scale)
        }
    }

    fun renderOnModel(
        item: ItemStack,
        state: PosableState,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        light: Int = ((11) or ((7) shl 16)),
        frontLight: Boolean = false,
        entity: LivingEntity? = null
    ) {
        if (!item.`is`(CobblemonItemTags.HIDDEN_ITEMS)) renderAtLocator(item, state, entity, poseStack, buffer, light, 0, frontLight)
        for ( (targetLocator, item) in state.animationItems) renderAtLocator(item, state, entity, poseStack, buffer, light, 0, frontLight, targetLocator)
    }

    // PT128: Matrix3x2fStack overload bridges MC 26.1 GuiGraphicsExtractor.pose() callers.
    fun renderOnModel(
        item: ItemStack,
        state: PosableState,
        poseStack: org.joml.Matrix3x2fStack,
        buffer: MultiBufferSource,
        light: Int = ((11) or ((7) shl 16)),
        frontLight: Boolean = false,
        entity: LivingEntity? = null
    ) {
        val internalStack = PoseStack()
        renderOnModel(item, state, internalStack, buffer, light, frontLight, entity)
    }

    private fun renderAtLocator(
        item: ItemStack,
        state: PosableState,
        entity: LivingEntity?,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        seed: Int,
        frontLight: Boolean = false,
        targetLocator: String =
            if (item.`is`(WEARABLE_FACE_ITEMS) && state.locatorStates.containsKey(ITEM_FACE)) ITEM_FACE
            else if (item.`is`(WEARABLE_HAT_ITEMS) && state.locatorStates.containsKey(ITEM_HAT)) ITEM_HAT
            else ITEM
    ) {
        if (item.isEmpty || !state.locatorStates.containsKey(targetLocator)) return

        poseStack.pushPose()
        Unit

        displayContext = state.currentModel?.getLocatorDisplayContext(targetLocator)?:
            if ((item.`is`(WEARABLE_FACE_ITEMS) && targetLocator==ITEM_FACE) || (item.`is`(WEARABLE_HAT_ITEMS) && targetLocator== ITEM_HAT)) ItemDisplayContext.HEAD
            else ItemDisplayContext.FIXED

        val isLeft = displayContext==ItemDisplayContext.THIRD_PERSON_LEFT_HAND

        poseStack.mulPose(state.locatorStates[targetLocator]!!.matrix)
        when (displayContext) {
            ItemDisplayContext.FIXED -> {
                poseStack.setOriginAndScale(0f,.5f,0f, .5f)
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0f))
            }
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND -> {
                //For Minecraft player models
                if (state is NPCClientDelegate) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f))
                    // PT144: PoseStack.translate now requires (x,y,z) 3-arg in MC 26.1.x.
                    poseStack.translate((0.025f * if (isLeft) 1 else -1 ).toFloat(), 0.0f, 0.0f)
                }
                //for T-Posing Models
                else if (state is PokemonClientDelegate || state is FloatingState) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f * if (isLeft) -1f else 1f))
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f * if (isLeft) -1f else 1f))
                    poseStack.translate(0.0f,0.0f,-0.0625f) // offset item by -1/16 to recenter item onto the locator
                }
            }
            ItemDisplayContext.HEAD -> {
                if (state is NPCClientDelegate) {
                    if (item.`is`(WEARABLE_FACE_ITEMS)) poseStack.setOriginAndScale(0f,-2f,7.25f, 0.62f)
                    else if (item.`is`(WEARABLE_HAT_ITEMS)) poseStack.setOriginAndScale(0f,-10.25f,0f, 0.62f)
                }
                else {
                    if (item.`is`(WEARABLE_FACE_ITEMS)) poseStack.setOriginAndScale(0f,1f,(7.25f)-.5f, 0.62f)
                    else if (item.`is`(WEARABLE_HAT_ITEMS)) poseStack.setOriginAndScale(0f,(-7.25f)+.5f,0f, 0.62f)
                }
            }
            else -> {}
        }

        //Render front lighting for UI Models
        // PT144: RenderSystem.setShaderLights now takes GpuBufferSlice (not 2x Vector3f) in MC 26.1.x. Stubbed pending pipeline migration.
        if (frontLight) {
            // RenderSystem.setShaderLights(buffer) — stub
        }

        itemRenderer.renderStatic(entity, item, displayContext, isLeft, poseStack, buffer, null, light, OverlayTexture.NO_OVERLAY, seed)

        poseStack.popPose()
    }
}