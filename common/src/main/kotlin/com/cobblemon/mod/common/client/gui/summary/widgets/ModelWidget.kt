/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.summary.widgets

import net.minecraft.client.input.MouseButtonEvent
import com.bedrockk.molang.runtime.value.DoubleValue
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.tags.CobblemonItemTags
import com.cobblemon.mod.common.client.gui.drawProfilePokemon
import com.cobblemon.mod.common.client.gui.calculateHeadYawAndPitch
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.client.render.item.HeldItemRenderer
import com.cobblemon.mod.common.pokemon.RenderablePokemon
import com.cobblemon.mod.common.util.math.fromEulerXYZDegrees
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import org.joml.Quaternionf
import org.joml.Vector3f

class ModelWidget(
    pX: Int, pY: Int,
    pWidth: Int, pHeight: Int,
    pokemon: RenderablePokemon,
    var baseScale: Float = 2.7F,
    var rotationY: Float = 35F,
    var offsetY: Double = 0.0,
    val playCryOnClick: Boolean = false,
    val shouldFollowCursor: Boolean = false,
    val blockLight: Int = 13
): SoundlessWidget(pX, pY, pWidth, pHeight, Component.literal("Summary - ModelWidget")) {

    companion object {
        var render = true
        const val MAX_SECONDS_LOOKING = 6L
    }

    var pokemon: RenderablePokemon = pokemon
        set (value) {
            field = value
            currentYawAndPitch = Pair(0f, 0f)
            state = FloatingState()
            state.runtime.environment.query.addFunction("is_holding_item") { return@addFunction DoubleValue(field.heldItem.let {
               !it.isEmpty && !it.`is`(CobblemonItemTags.WEARABLE_HAT_ITEMS) && !it.`is`(CobblemonItemTags.WEARABLE_FACE_ITEMS)
            }) }
            state.runtime.environment.query.addFunction("is_wearing_hat") { return@addFunction DoubleValue(field.heldItem.`is`(CobblemonItemTags.WEARABLE_HAT_ITEMS)) }
            state.runtime.environment.query.addFunction("is_wearing_face") { return@addFunction DoubleValue(field.heldItem.`is`(CobblemonItemTags.WEARABLE_FACE_ITEMS)) }
        }

    private val heldItemRenderer = HeldItemRenderer()

    var state = FloatingState()
    var lookStartTime: Long? = null
    var currentYawAndPitch: Pair<Float, Float> = Pair(0f, 0f)
    val rotationVector = Vector3f(13F, rotationY, 0F)

    override fun extractWidgetRenderState(context: GuiGraphicsExtractor, pMouseX: Int, pMouseY: Int, partialTicks: Float) {
        if (!render) {
            return
        }
        isHovered = pMouseX >= x && pMouseY >= y && pMouseX < x + width && pMouseY < y + height
        renderPKM(context, partialTicks, pMouseX, pMouseY)
    }

    private fun renderPKM(context: GuiGraphicsExtractor, partialTicks: Float, mouseX: Int, mouseY: Int) {
        val matrices = context.pose()
        matrices.pushMatrix()

        context.enableScissor(
            x,
            y,
            x + width,
            y + height
        )

        matrices.translate((x + width * 0.5).toFloat(), (y.toDouble() + offsetY).toFloat())
        // PT144: Matrix3x2f is 2D; only 2-arg scale is valid in MC 26.1.x.
        matrices.scale(baseScale, baseScale)
        matrices.pushMatrix()

        if (isHovered) {
            lookStartTime = System.currentTimeMillis()
        }

        val timeLooking = lookStartTime?.let { (System.currentTimeMillis() - it) / 1000 } ?: MAX_SECONDS_LOOKING
        val lookedTooMuch = timeLooking >= MAX_SECONDS_LOOKING
        val resetToCenter = (!isHovered && lookedTooMuch) || !shouldFollowCursor || !Cobblemon.config.summaryPokemonFollowCursor

        if (resetToCenter) {
            lookStartTime = null
        }

        val rotation = Quaternionf().fromEulerXYZDegrees(rotationVector)

        if (shouldFollowCursor) {
            currentYawAndPitch = calculateHeadYawAndPitch(
                x + width / 2f,
                y + height / 2f,
                rotation,
                mouseX,
                mouseY,
                currentYawAndPitch.first,
                currentYawAndPitch.second,
                resetToCenter
            )
        }

        drawProfilePokemon(
            renderablePokemon = pokemon,
            matrixStack = matrices,
            rotation = rotation,
            state = state,
            partialTicks = partialTicks,
            headYaw = currentYawAndPitch.first,
            headPitch = currentYawAndPitch.second,
            blockLight = blockLight
        )

        // PT144: GuiGraphicsExtractor.bufferSource() removed — use renderBuffers().bufferSource().
        heldItemRenderer.renderOnModel(
            pokemon.heldItem,
            state,
            matrices,
            net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource(),
            light = 0xF000F0,
            true
        )

        matrices.popMatrix()
        context.disableScissor()

        matrices.popMatrix()
    }

    override fun mouseClicked(event: MouseButtonEvent, fromOnClick: Boolean): Boolean {
        val pMouseX = event.x
        val pMouseY = event.y
        val pButton = event.button()
        if (this.isHovered) {
            playCry()
        }

        return super.mouseClicked(event, fromOnClick)
    }

    private fun playCry() {
        if (playCryOnClick) {
            state.activeAnimations.clear()
            state.addFirstAnimation(setOf("cry"))
        }
    }
}