/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.gui

import com.cobblemon.mod.common.mixin.accessor.GuiGraphicsExtractorAccessor
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState
import org.joml.Matrix3x2f
import org.joml.Matrix3x2fc

/**
 * port/26.2: the GUI became a 2D, state-extracted pipeline. Screens can no longer grab a buffer and draw
 *3D geometry inline - anything three-dimensional goes through picture-in-picture, which is how vanilla
 * draws the inventory player, banners and book models.
 *
 * Cobblemon draws posable models all over its interfaces (party, PC, summary, battle, Pokedex), so this
 * carries the draw itself rather than a specific model: the caller keeps its existing transform and
 * render code and hands it over as [draw], which the renderer replays into the picture-in-picture
 * texture during the render pass.
 */
class PosableModelGuiRenderState(
    private val x0: Int,
    private val y0: Int,
    private val x1: Int,
    private val y1: Int,
    private val scale: Float,
    private val pose: Matrix3x2fc,
    private val scissorArea: ScreenRectangle?,
    val draw: (PoseStack, SubmitNodeCollector) -> Unit
) : PictureInPictureRenderState {
    override fun x0() = x0
    override fun x1() = x1
    override fun y0() = y0
    override fun y1() = y1
    override fun scale() = scale
    override fun pose(): Matrix3x2fc = pose
    override fun scissorArea(): ScreenRectangle? = scissorArea

    /** The screen area this occupies, clipped to any scissor region in effect. */
    override fun bounds(): ScreenRectangle =
        PictureInPictureRenderState.getBounds(x0, x1, y0, y1, scissorArea) ?: ScreenRectangle.empty()
}

class PosableModelGuiRenderer : PictureInPictureRenderer<PosableModelGuiRenderState>() {
    override fun getRenderStateClass(): Class<PosableModelGuiRenderState> = PosableModelGuiRenderState::class.java

    override fun renderToTexture(
        state: PosableModelGuiRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector
    ) {
        state.draw(poseStack, collector)
    }

    override fun getTextureLabel(): String = "cobblemon posable model"
}

/**
 * Queues a posable model to be drawn inside the given screen rectangle.
 *
 * The rectangle is the region the model is rendered into; [scale] is passed through to the
 * picture-in-picture state, and [draw] receives a fresh [PoseStack] centred on that region along with
 * the collector to submit geometry to.
 */
fun GuiGraphicsExtractor.submitPosableModelToGui(
    x0: Int,
    y0: Int,
    x1: Int,
    y1: Int,
    scale: Float,
    draw: (PoseStack, SubmitNodeCollector) -> Unit
) {
    if (x1 <= x0 || y1 <= y0) return
    val renderState = (this as GuiGraphicsExtractorAccessor).`cobblemon$getGuiRenderState`()
    renderState.addPicturesInPictureState(
        PosableModelGuiRenderState(
            x0 = x0,
            y0 = y0,
            x1 = x1,
            y1 = y1,
            scale = scale,
            pose = Matrix3x2f(this.pose()),
            scissorArea = null,
            draw = draw
        )
    )
}
