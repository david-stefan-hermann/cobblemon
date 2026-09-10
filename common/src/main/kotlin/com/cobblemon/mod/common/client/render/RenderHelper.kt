/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render

import net.minecraft.client.renderer.rendertype.RenderTypes

import com.cobblemon.mod.common.api.gui.drawText
import com.cobblemon.mod.common.api.gui.drawTextJustifiedRight
import com.cobblemon.mod.common.api.text.font
import com.cobblemon.mod.common.client.CobblemonResources
import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.Identifier
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.entity.LivingEntity

/**
 * port/26.2: Minecraft.itemRenderer and its immediate-mode renderStatic are gone. Items are now built
 * into an [ItemStackRenderState] by [net.minecraft.client.renderer.item.ItemModelResolver] and then
 * submitted to the collector, which draws them in the later render pass.
 *
 * This keeps the old renderStatic call shape so the mod's item-rendering sites (held items, display
 * cases, the healing machine, the lectern, the campfire pot, the fossil analyser and the fishing
 * bobber) read the same as before.
 *
 * A fresh render state is allocated per call rather than reused: submission is deferred, so a shared
 * instance would be overwritten before the render pass consumed it.
 */
class CobblemonItemRenderer {
    fun renderStatic(
        stack: ItemStack, ctx: ItemDisplayContext, light: Int, overlay: Int,
        pose: PoseStack, buffer: SubmitNodeCollector, level: Level?, seed: Int
    ) {
        if (stack.isEmpty) return
        val state = ItemStackRenderState()
        Minecraft.getInstance().itemModelResolver.updateForTopItem(state, stack, ctx, level, null, seed)
        state.submit(pose, buffer, light, overlay, NO_OUTLINE)
    }

    fun renderStatic(
        entity: LivingEntity?, stack: ItemStack, ctx: ItemDisplayContext, leftHand: Boolean,
        pose: PoseStack, buffer: SubmitNodeCollector, level: Level?, light: Int, overlay: Int, seed: Int
    ) {
        if (stack.isEmpty) return
        val state = ItemStackRenderState()
        val resolver = Minecraft.getInstance().itemModelResolver
        // port/26.2: updateForLiving no longer takes a leftHand flag - which hand is being drawn is
        // carried by the display context (THIRD_PERSON_LEFT_HAND vs THIRD_PERSON_RIGHT_HAND) instead.
        if (entity != null) {
            resolver.updateForLiving(state, stack, ctx, entity)
        } else {
            resolver.updateForTopItem(state, stack, ctx, level, null, seed)
        }
        state.submit(pose, buffer, light, overlay, NO_OUTLINE)
    }

    companion object {
        /** The outline colour argument of ItemStackRenderState.submit; 0 means no glow outline. */
        const val NO_OUTLINE = 0
    }
}
private val ITEM_RENDERER = CobblemonItemRenderer()
val Minecraft.itemRenderer: CobblemonItemRenderer get() = ITEM_RENDERER

fun renderScaledGuiItemIcon(itemStack: ItemStack, x: Double, y: Double, scale: Double = 1.0, zTranslation: Float = 100.0F, matrixStack: org.joml.Matrix3x2fStack? = null) {
    // TODO PT128-DEFER: ItemRenderer.render(stack, ItemDisplayContext, ...) API rework
    // MC 26.1.x removed Minecraft.itemRenderer; use GuiGraphicsExtractor for item drawing or
    // ItemModelResolver via Minecraft.getInstance().itemModelResolver. Currently no-op stub
    // to unblock compilation — callsites still invoke this for HUD-side item icons.
    val modelViewStack = matrixStack ?: org.joml.Matrix3x2fStack(32)
    modelViewStack.pushMatrix()
    modelViewStack.translate(x.toFloat(), y.toFloat())
    modelViewStack.translate(8.0F * scale.toFloat(), 8.0F * scale.toFloat())
    modelViewStack.scale(1.0F, -1.0F)
    modelViewStack.scale(16.0F * scale.toFloat(), 16.0F * scale.toFloat())
    modelViewStack.popMatrix()
    Unit
}

fun getDepletableRedGreen(
    ratio: Float,
    yellowRatio: Float = 0.5F,
    redRatio: Float = 0.2F
): Pair<Float, Float> {
    val m = -2

    val r = if (ratio > redRatio) {
        m * ratio - m
    } else {
        1.0
    }

    val g = if (ratio > yellowRatio) {
        1.0
    } else if (ratio > redRatio) {
        ratio * 1 / yellowRatio
    } else {
        0.0
    }

    return r.toFloat() to g.toFloat()
}


fun drawScaledText(
    context: GuiGraphicsExtractor,
    font: Identifier? = null,
    text: MutableComponent,
    x: Number,
    y: Number,
    scale: Float = 1F,
    opacity: Number = 1F,
    maxCharacterWidth: Int = Int.MAX_VALUE,
    colour: Int = 0x00FFFFFF + ((opacity.toFloat() * 255).toInt() shl 24),
    centered: Boolean = false,
    shadow: Boolean = false,
    pMouseX: Int? = null,
    pMouseY: Int? = null
) {
    if (opacity.toFloat() < 0.05F) {
        return
    }

    val textWidth = Minecraft.getInstance().font.width(if (font != null) text.font(font) else text)
    val extraScale = if (textWidth < maxCharacterWidth) 1F else (maxCharacterWidth / textWidth.toFloat())
    val fontHeight = if (font == null) 5F else 6F
    val matrices = context.pose()
    matrices.pushMatrix()
    matrices.scale(scale * extraScale, scale * extraScale)
    val isHovered = drawText(
        context = context,
        font = font,
        text = text,
        x = x.toFloat() / (scale * extraScale),
        y = y.toFloat() / (scale * extraScale) + (1F - extraScale) * fontHeight * scale,
        centered = centered,
        colour = colour,
        shadow = shadow,
        pMouseX = pMouseX?.toFloat()?.div((scale * extraScale)),
        pMouseY = pMouseY?.toFloat()?.div(scale * extraScale)?.plus((1F - extraScale) * fontHeight * scale)
    )
    matrices.popMatrix()
    // Draw tooltip that was created with onHover and is attached to the MutableComponent
    // PT145: renderComponentHoverEffect removed in MC 26.1.x; tooltips are now applied via setTooltipForNextRenderPass on widgets.
    // Hover effect handled by widget-level tooltips; this in-text style hover is dropped.
}

fun drawScaledText(
    context: GuiGraphicsExtractor,
    text: FormattedCharSequence,
    x: Number,
    y: Number,
    scaleX: Float = 1F,
    scaleY: Float = 1F,
    opacity: Number = 1F,
    colour: Int = 0x00FFFFFF + ((opacity.toFloat() * 255).toInt() shl 24),
    centered: Boolean = false,
    shadow: Boolean = false
) {
    if (opacity.toFloat() < 0.05F) {
        return
    }
    val matrixStack = context.pose()
    matrixStack.pushMatrix()
    matrixStack.scale(scaleX, scaleY)
    drawText(
        context = context,
        text = text,
        x = x.toFloat() / scaleX,
        y = y.toFloat() / scaleY,
        centered = centered,
        colour = colour,
        shadow = shadow
    )
    matrixStack.popMatrix()
}

fun drawScaledTextJustifiedRight(
    context: GuiGraphicsExtractor,
    font: Identifier? = null,
    text: MutableComponent,
    x: Number,
    y: Number,
    scale: Float = 1F,
    opacity: Number = 1F,
    maxCharacterWidth: Int = Int.MAX_VALUE,
    colour: Int = 0x00FFFFFF + ((opacity.toFloat() * 255).toInt() shl 24),
    shadow: Boolean = false
) {
    if (opacity.toFloat() < 0.05F) {
        return
    }
    val textWidth = Minecraft.getInstance().font.width(if (font != null) text.font(font) else text)
    val extraScale = if (textWidth < maxCharacterWidth) 1F else (maxCharacterWidth / textWidth.toFloat())
    val fontHeight = if (font == null) 5F else 6F
    val matrixStack = context.pose()
    matrixStack.pushMatrix()
    matrixStack.scale(scale * extraScale, scale * extraScale)
    drawTextJustifiedRight(
        context = context,
        font = font,
        text = text,
        x = x.toFloat() / (scale * extraScale),
        y = y.toFloat() / (scale * extraScale) + (1F - extraScale) * fontHeight * scale,
        colour = colour,
        shadow = shadow
    )
    matrixStack.popMatrix()
}

fun drawScaledTextJustifiedRight(
    context: GuiGraphicsExtractor,
    text: MutableComponent,
    x: Number,
    y: Number,
    scaleX: Float = 1F,
    scaleY: Float = 1F,
    opacity: Number = 1F,
    colour: Int = 0x00FFFFFF + ((opacity.toFloat() * 255).toInt() shl 24),
    shadow: Boolean = false
) {
    if (opacity.toFloat() < 0.05F) {
        return
    }
    val matrixStack = context.pose()
    matrixStack.pushMatrix()
    matrixStack.scale(scaleX, scaleY)
    drawTextJustifiedRight(
        context = context,
        text = text,
        x = x.toFloat() / scaleX,
        y = y.toFloat() / scaleY,
        colour = colour,
        shadow = shadow
    )
    matrixStack.popMatrix()
}

fun renderBeaconBeam(
    matrixStack: PoseStack,
    buffer: SubmitNodeCollector,
    textureLocation: Identifier = CobblemonResources.PHASE_BEAM,
    partialTicks: Float,
    totalLevelTime: Long,
    yOffset: Float = 0F,
    height: Float,
    red: Float,
    green: Float,
    blue: Float,
    alpha: Float,
    beamRadius: Float,
    glowRadius: Float,
    glowAlpha: Float
) {
    val i = yOffset + height
    val beamRotation = Math.floorMod(totalLevelTime, 40).toFloat() + partialTicks

    // port/26.2: MultiBufferSource is gone, so the beam can no longer fetch a VertexConsumer and write
    // vertices immediately. Each pass is handed to the collector, which replays it during the render
    // pass; the pose is captured at submit time, so the rotation below still applies to the core beam
    // only and the glow is still submitted after it has been undone.
    matrixStack.pushPose()
    matrixStack.mulPose(Axis.YP.rotationDegrees(beamRotation * 2.25f - 45.0f))
    val coreOffset = -beamRadius
    buffer.submitCustomGeometry(matrixStack, RenderTypes.beaconBeam(textureLocation, false)) { pose, consumer ->
        renderPart(
            pose,
            consumer,
            red,
            green,
            blue,
            alpha,
            yOffset,
            i,
            0.0f,
            beamRadius,
            beamRadius,
            0.0f,
            coreOffset,
            0.0f,
            0.0f,
            coreOffset
        )
    }
    // Undo the rotation so that the glow is at a rotated offset
    matrixStack.popPose()

    val glowOffset = -glowRadius
    buffer.submitCustomGeometry(matrixStack, RenderTypes.beaconBeam(textureLocation, true)) { pose, consumer ->
        renderPart(
            pose,
            consumer,
            red,
            green,
            blue,
            glowAlpha,
            yOffset,
            i,
            glowOffset,
            glowOffset,
            glowRadius,
            glowOffset,
            glowOffset,
            glowRadius,
            glowRadius,
            glowRadius
        )
    }
}


// port/26.2: takes a PoseStack.Pose rather than the whole stack - it only ever used last(), and this is
// exactly what SubmitNodeCollector.submitCustomGeometry hands to its callback.
fun renderPart(
    pose: PoseStack.Pose,
    vertexBuffer: VertexConsumer,
    red: Float,
    green: Float,
    blue: Float,
    alpha: Float,
    yMin: Float,
    yMax: Float,
    p_112164_: Float,
    p_112165_: Float,
    p_112166_: Float,
    p_112167_: Float,
    p_112168_: Float,
    p_112169_: Float,
    p_112170_: Float,
    p_112171_: Float
) {
    val matrix4f = pose.pose()
    val matrix3f = pose.normal()
    renderQuad(
        pose,
        vertexBuffer,
        red,
        green,
        blue,
        alpha,
        yMin,
        yMax,
        p_112164_,
        p_112165_,
        p_112166_,
        p_112167_
    )
    renderQuad(
        pose,
        vertexBuffer,
        red,
        green,
        blue,
        alpha,
        yMin,
        yMax,
        p_112170_,
        p_112171_,
        p_112168_,
        p_112169_
    )
    renderQuad(
        pose,
        vertexBuffer,
        red,
        green,
        blue,
        alpha,
        yMin,
        yMax,
        p_112166_,
        p_112167_,
        p_112170_,
        p_112171_
    )
    renderQuad(
        pose,
        vertexBuffer,
        red,
        green,
        blue,
        alpha,
        yMin,
        yMax,
        p_112168_,
        p_112169_,
        p_112164_,
        p_112165_
    )
}

fun renderQuad(
    matrixEntry: PoseStack.Pose,
    buffer: VertexConsumer,
    red: Float,
    green: Float,
    blue: Float,
    alpha: Float,
    yMin: Float,
    yMax: Float,
    x1: Float,
    z1: Float,
    x2: Float,
    z2: Float
) {
    addVertex(matrixEntry, buffer, red, green, blue, alpha, yMax, x1, z1, 1F, 0F)
    addVertex(matrixEntry, buffer, red, green, blue, alpha, yMin, x1, z1, 1F, 1F)
    addVertex(matrixEntry, buffer, red, green, blue, alpha, yMin, x2, z2, 0F, 1F)
    addVertex(matrixEntry, buffer, red, green, blue, alpha, yMax, x2, z2, 0F, 0F)
}

fun addVertex(
    matrixEntry: PoseStack.Pose,
    buffer: VertexConsumer,
    red: Float,
    green: Float,
    blue: Float,
    alpha: Float,
    y: Float,
    x: Float,
    z: Float,
    texU: Float,
    texV: Float
) {
    buffer
        .addVertex(matrixEntry.pose(), x, y, z)
        .setColor(red, green, blue, alpha)
        .setUv(texU, texV)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(15728880)
        .setNormal(matrixEntry, 0.0f, 1.0f, 0.0f)
}