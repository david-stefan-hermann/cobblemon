/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.gui

import net.minecraft.client.renderer.rendertype.RenderTypes

import com.cobblemon.mod.common.api.text.font
import com.cobblemon.mod.common.client.gui.battle.BattleOverlay.Companion.PORTRAIT_DIAMETER
import com.cobblemon.mod.common.client.render.SpriteType
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.util.toHex
import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import com.cobblemon.mod.common.client.render.gui.submitModelAtCurrentPose
import com.cobblemon.mod.common.client.render.submitPosableModel
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.SubmitNodeCollector
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.Identifier
import net.minecraft.util.FormattedCharSequence
import org.joml.Matrix4f
import org.joml.Vector3f

@JvmOverloads
fun blitk(
    matrixStack: org.joml.Matrix3x2fStack,
    texture: Identifier? = null,
    x: Number,
    y: Number,
    height: Number = 0,
    width: Number = 0,
    uOffset: Number = 0,
    vOffset: Number = 0,
    textureWidth: Number = width,
    textureHeight: Number = height,
    blitOffset: Number = 0,
    red: Number = 1,
    green: Number = 1,
    blue: Number = 1,
    alpha: Number = 1F,
    blend: Boolean = true,
    scale: Float = 1F
) {
    // PT104: stub for MC 26.1 — original PoseStack/BufferUploader/Tesselator approach removed.
    // TODO: rewrite using GuiGraphicsExtractor.blit RenderPipeline path.
}

@JvmOverloads
fun blitk(
    matrixStack: PoseStack,
    texture: Identifier? = null,
    x: Number,
    y: Number,
    height: Number = 0,
    width: Number = 0,
    uOffset: Number = 0,
    vOffset: Number = 0,
    textureWidth: Number = width,
    textureHeight: Number = height,
    blitOffset: Number = 0,
    red: Number = 1,
    green: Number = 1,
    blue: Number = 1,
    alpha: Number = 1F,
    blend: Boolean = true,
    scale: Float = 1F
) {
    // PT105: PoseStack overload — accepts legacy callers, delegates to no-op stub above.
}

fun drawRectangle(
    matrix: Matrix4f,
    x: Float,
    y: Float,
    endX: Float,
    endY: Float,
    blitOffset: Float,
    minU: Float,
    maxU: Float,
    minV: Float,
    maxV: Float
) {
    // PT104: stub for MC 26.1 — BufferUploader.drawWithShader removed.
}

@JvmOverloads
fun drawCenteredText(
    context: GuiGraphicsExtractor,
    font: Identifier? = null,
    text: Component,
    x: Number,
    y: Number,
    colour: Int,
    shadow: Boolean = true
) {
    val comp = (text as MutableComponent).let { if (font != null) it.font(font) else it }
    val textRenderer = Minecraft.getInstance().font
    context.text(textRenderer, comp, x.toInt() - textRenderer.width(comp) / 2, y.toInt(), colour, shadow)
}

@JvmOverloads
fun drawText(
    context: GuiGraphicsExtractor,
    font: Identifier? = null,
    text: MutableComponent,
    x: Number,
    y: Number,
    centered: Boolean = false,
    colour: Int,
    shadow: Boolean = true,
    pMouseX: Number? = null,
    pMouseY: Number? = null
): Boolean {
    // PT137: Style.withFont(Identifier) → withFont(FontDescription) in MC 26.1.x
    val comp = if (font == null) text else text.setStyle(text.style.withFont(net.minecraft.network.chat.FontDescription.Resource(font)))
    val textRenderer = Minecraft.getInstance().font
    var x = x
    val width = textRenderer.width(comp)
    if (centered) {
        x = x.toDouble() - width / 2
    }
    context.text(textRenderer, comp, x.toInt(), y.toInt(), colour, shadow)
    var isHovered = false
    if (pMouseY != null && pMouseX != null) {
        if (pMouseX.toInt() >= x.toInt() && pMouseX.toInt() <= x.toInt() + width &&
            pMouseY.toInt() >= y.toInt() && pMouseY.toInt() <= y.toInt() + textRenderer.lineHeight
        ) {
            isHovered = true
        }
    }
    return isHovered
}

@JvmOverloads
fun drawTextJustifiedRight(
    context: GuiGraphicsExtractor,
    font: Identifier? = null,
    text: MutableComponent,
    x: Number,
    y: Number,
    colour: Int,
    shadow: Boolean = true
) {
    val comp = text.let { if (font != null) it.font(font) else it }
    val font = Minecraft.getInstance().font
    context.text(font, comp, x.toInt() - font.width(comp), y.toInt(), colour, shadow)
}

@JvmOverloads
fun drawText(
    context: GuiGraphicsExtractor,
    text: FormattedCharSequence,
    x: Number,
    y: Number,
    centered: Boolean = false,
    colour: Int,
    shadow: Boolean = true
) {
    val textRenderer = Minecraft.getInstance().font
    var tweakedX = x
    if (centered) {
        val width = textRenderer.width(text)
        tweakedX = tweakedX.toDouble() - width / 2
    }
    context.text(textRenderer, text, tweakedX.toInt(), y.toInt(), colour, shadow)
}

@JvmOverloads
fun drawString(
    context: GuiGraphicsExtractor,
    text: String,
    x: Number,
    y: Number,
    colour: Int,
    shadow: Boolean = true,
    font: Identifier? = null
) {
    // PT137: Style.withFont(Identifier) → withFont(FontDescription) in MC 26.1.x
    val comp = Component.literal(text).also {
        font?.run {
            it.toFlatList(it.style.withFont(net.minecraft.network.chat.FontDescription.Resource(this)))
        }
    }
    val textRenderer = Minecraft.getInstance().font
    context.text(textRenderer, comp, x.toInt(), y.toInt(), colour, shadow)
}

@JvmOverloads
fun drawPosablePortrait(
    identifier: Identifier,
    matrixStack: PoseStack,
    collector: SubmitNodeCollector,
    scale: Float = 13F,
    contextScale: Float = 1F,
    reversed: Boolean = false,
    state: PosableState,
    partialTicks: Float,
    limbSwing: Float = 0F,
    limbSwingAmount: Float = 0F,
    ageInTicks: Float = 0F,
    headYaw: Float = 0F,
    headPitch: Float = 0F,
    doQuirks: Boolean = true,
    r: Float = 1F,
    g: Float = 1F,
    b: Float = 1F,
    a: Float = 1F
) {
    Unit
    matrixStack.pushPose()
    // PT137: PoseStack.translate is 3-arg(x,y,z) — 2-arg variant belongs to Matrix3x2fStack
    matrixStack.translate(0.0f, (PORTRAIT_DIAMETER + 2.0).toFloat(), 0.0f)
    matrixStack.scale(scale, scale, -scale)
    matrixStack.translate(0.0, -PORTRAIT_DIAMETER / 18.0, 0.0)

    val sprite = VaryingModelRepository.getSprite(identifier, state, SpriteType.PORTRAIT)

    if (sprite == null) {
        val model = VaryingModelRepository.getPoser(identifier, state)
        state.currentModel = model
        val texture = VaryingModelRepository.getTexture(identifier, state)

        val context = RenderContext()
        model.context = context
        VaryingModelRepository.getTextureNoSubstitute(identifier, state).let { context.put(RenderContext.TEXTURE, it) }
        context.put(RenderContext.SCALE, contextScale)
        context.put(RenderContext.SPECIES, identifier)
        context.put(RenderContext.ASPECTS, state.currentAspects)
        context.put(RenderContext.POSABLE_STATE, state)
        context.put(RenderContext.DO_QUIRKS, doQuirks)

        val renderType = RenderTypes.entityCutout(texture)

        val quaternion1 = Axis.YP.rotationDegrees(-32F * if (reversed) -1F else 1F)
        val quaternion2 = Axis.XP.rotationDegrees(5F)

        val originalPose = state.currentPose
        state.setPoseToFirstSuitable(PoseType.PORTRAIT)
        state.updatePartialTicks(partialTicks)
        model.applyAnimations(null, state, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch)
        originalPose?.let { state.setPose(it) }

        matrixStack.translate(
            model.portraitTranslation.x * if (reversed) -1F else 1F,
            model.portraitTranslation.y + 1.5 * model.portraitScale,
            model.portraitTranslation.z - 4
        )
        matrixStack.scale(model.portraitScale, model.portraitScale, 1 / model.portraitScale)
        matrixStack.mulPose(quaternion1)
        matrixStack.mulPose(quaternion2)

        val light1 = Vector3f(0.2F, 1.0F, -1.0F)
        val light2 = Vector3f(0.1F, 0.0F, 8.0F)
        // PT137: setShaderLights now takes GpuBufferSlice in MC 26.1.x — deferred GPU buffer wiring
        // RenderSystem.setShaderLights(light1, light2)
        quaternion1.conjugate()

        // port/26.2: Minecraft.renderBuffers() is gone - the portrait is submitted to the collector the
        // picture-in-picture renderer supplies.
        val packedLight = ((11) or ((7) shl 16))
        val colour = toHex(r, g, b, a)
        collector.submitPosableModel(matrixStack, renderType) { stack, consumer ->
            model.withLayerContext(collector, state, VaryingModelRepository.getLayers(identifier, state)) {
                model.render(context, stack, consumer, packedLight, OverlayTexture.NO_OVERLAY, colour)
            }
        }
        model.setDefault()
    } else {
        renderSprite(matrixStack, collector, sprite)
    }

    matrixStack.popPose()
}

@JvmOverloads
fun drawPosablePortrait(
    identifier: Identifier,
    context: GuiGraphicsExtractor,
    scale: Float = 13F,
    contextScale: Float = 1F,
    reversed: Boolean = false,
    state: PosableState,
    partialTicks: Float,
    limbSwing: Float = 0F,
    limbSwingAmount: Float = 0F,
    ageInTicks: Float = 0F,
    headYaw: Float = 0F,
    headPitch: Float = 0F,
    doQuirks: Boolean = true,
    r: Float = 1F,
    g: Float = 1F,
    b: Float = 1F,
    a: Float = 1F
) {
    // port/26.2: this had been left as an outright no-op, so portraits in the party and battle overlays
    // and the dialogue faces drew nothing. It submits through picture-in-picture now, like the profile
    // renderer.
    context.submitModelAtCurrentPose(scale) { poseStack, collector ->
        drawPosablePortrait(
            identifier = identifier,
            matrixStack = poseStack,
            collector = collector,
            scale = scale,
            contextScale = contextScale,
            reversed = reversed,
            state = state,
            partialTicks = partialTicks,
            limbSwing = limbSwing,
            limbSwingAmount = limbSwingAmount,
            ageInTicks = ageInTicks,
            headYaw = headYaw,
            headPitch = headPitch,
            doQuirks = doQuirks,
            r = r, g = g, b = b, a = a
        )
    }
}

fun drawProfile(
    resourceIdentifier: Identifier,
    matrixStack: PoseStack,
    collector: SubmitNodeCollector,
    state: PosableState,
    partialTicks: Float,
    scale: Float = 20F
) {
    Unit
    matrixStack.scale(scale, scale, -scale)

    val sprite = VaryingModelRepository.getSprite(resourceIdentifier, state, SpriteType.PROFILE)

    if (sprite == null) {

        val model = VaryingModelRepository.getPoser(resourceIdentifier, state)
        val texture = VaryingModelRepository.getTexture(resourceIdentifier, state)

        val context = RenderContext()
        model.context = context
        VaryingModelRepository.getTextureNoSubstitute(resourceIdentifier, state).let { context.put(RenderContext.TEXTURE, it) }
        context.put(RenderContext.SCALE, 1F)
        context.put(RenderContext.SPECIES, resourceIdentifier)
        context.put(RenderContext.ASPECTS, state.currentAspects)
        context.put(RenderContext.POSABLE_STATE, state)
        state.currentModel = model

        val renderType = RenderTypes.entityCutout(texture)//model.getLayer(texture)

        state.setPoseToFirstSuitable(PoseType.PORTRAIT)
        state.updatePartialTicks(partialTicks)
        model.applyAnimations(null, state, 0F, 0F, 0F, 0F, 0F)
        matrixStack.translate(
            model.profileTranslation.x,
            model.profileTranslation.y + 1.5 * model.profileScale,
            model.profileTranslation.z - 4.0
        )
        matrixStack.scale(model.profileScale, model.profileScale, 1 / model.profileScale)
//    matrixStack.multiply(rotation)
        val quaternion1 = Axis.YP.rotationDegrees(-32F * if (false) -1F else 1F)
        val quaternion2 = Axis.XP.rotationDegrees(5F)
        matrixStack.mulPose(quaternion1)
        matrixStack.mulPose(quaternion2)
        // PT137: Lighting.setupForEntityInInventory() removed in MC 26.1.x — replaced by setupFor(Entry) GPU buffer
        // Lighting.setupForEntityInInventory()
        val entityRenderDispatcher = Minecraft.getInstance().entityRenderDispatcher
        Unit

        // port/26.2: submitted rather than drawn immediately; see drawPosablePortrait.
        val light1 = Vector3f(-1F, 1F, 1.0F)
        val light2 = Vector3f(1.3F, -1F, 1.0F)
        // PT137: setShaderLights now takes GpuBufferSlice in MC 26.1.x — deferred GPU buffer wiring
        // RenderSystem.setShaderLights(light1, light2)
        val packedLight = ((11) or ((7) shl 16))

        collector.submitPosableModel(matrixStack, renderType) { stack, consumer ->
            model.withLayerContext(collector, state, VaryingModelRepository.getLayers(resourceIdentifier, state)) {
                model.render(context, stack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -0x1)
            }
        }
        model.setDefault()
    } else {
        renderSprite(matrixStack, collector, sprite)
    }
}

// PT128: Matrix3x2fStack overload — bridges MC 26.1 GuiGraphicsExtractor.pose() to legacy PoseStack drawProfile.
fun drawProfile(
    resourceIdentifier: Identifier,
    context: GuiGraphicsExtractor,
    state: PosableState,
    partialTicks: Float,
    scale: Float = 20F
) {
    context.submitModelAtCurrentPose(scale) { poseStack, collector ->
        drawProfile(
            resourceIdentifier = resourceIdentifier,
            matrixStack = poseStack,
            collector = collector,
            state = state,
            partialTicks = partialTicks,
            scale = scale
        )
    }
}

// port/26.2: Tesselator batching is gone from this path - the quad is submitted to the collector, whose
// callback supplies the pose and consumer this already worked with.
fun renderSprite(matrixStack: PoseStack, collector: SubmitNodeCollector, sprite: Identifier) {
    matrixStack.last().pose().translate(-1f, 0f, 0f)

    collector.submitCustomGeometry(matrixStack, RenderTypes.entityCutout(sprite)) { matrix, buffer ->
        buffer.addVertex(matrix, 2f, 0f, 0.0f).setUv(1f, 0f)
        buffer.addVertex(matrix, 0f, 0f, 0.0f).setUv(0f, 0f)
        buffer.addVertex(matrix, 0f, 2f, 0.0f).setUv(0f, 1f)
        buffer.addVertex(matrix, 2f, 2f, 0.0f).setUv(1f, 1f)
    }
}
