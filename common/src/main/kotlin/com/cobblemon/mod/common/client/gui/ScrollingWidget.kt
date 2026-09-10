/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui

import net.minecraft.client.renderer.rendertype.RenderTypes

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.ObjectSelectionList
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.util.Mth

abstract class ScrollingWidget<T : ObjectSelectionList.Entry<T>>(
    top: Int = 0,
    val left: Int = 0,
    width: Int = 10,
    height: Int = 10,
    slotHeight: Int = 10,
    val scrollBarWidth: Int = 5
) : ObjectSelectionList<T>(
    Minecraft.getInstance(),
    width, // Width
    height, // Height
    top, // Top
    slotHeight // Slot Height
), CobblemonRenderable {
    override fun extractListBackground(guiGraphics: GuiGraphicsExtractor) {}
    // PT128: renderSelection(6-arg)→extractSelection(3-arg); renderDecorations(3-arg)→extractListSeparators(1-arg). MC 26.1 AbstractSelectionList API.
    override fun extractSelection(context: GuiGraphicsExtractor, entry: T, ignored: Int) {}
    override fun extractListSeparators(context: GuiGraphicsExtractor) {}


    init {
        setRectangle(width, height, top, top + height)
        setLeft(left)
    }

    final override fun setRectangle(width: Int, height: Int, top: Int, bottom: Int) {
        this.width = width
        this.height = height
        this.y = bottom
        this.x = left + width
    }

    fun setLeft(left: Int){
        this.x = left
    }

    override fun extractWidgetRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        // PT128: simplified — AbstractSelectionList.extractWidgetRenderState already orchestrates scissor/items/separators internally.
        super.extractWidgetRenderState(context, mouseX, mouseY, delta)
        val renderHorizontalShadows = false
        if (renderHorizontalShadows) {
            this.renderHorizontalShadows(context, mouseX, mouseY, delta)
        }
    }

    open fun renderScrollbar(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float){
        val xLeft = this.scrollBarX()
        val xRight = xLeft + scrollBarWidth

        val barHeight = this.bottom - this.y

        var j2 = ((barHeight * barHeight).toFloat() / this.contentHeight().toFloat()).toInt()
        j2 = Mth.clamp(j2, 32, barHeight - 8)
        var k1 = scrollAmount.toInt() * (barHeight - j2) / this.maxScrollAmount() + this.y
        if (k1 < this.y) {
            k1 = this.y
        }

        context.fill(xLeft, this.y, xRight, this.bottom, -16777216)
        context.fill(xLeft, k1, xRight, k1 + j2, -8355712)
        context.fill(xLeft ,k1, xRight - 1, k1 + j2 - 1, -4144960)
    }

    open fun renderHorizontalShadows(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float){
        Unit /* MC 26.1.x: GuiGraphicsExtractor.setColor removed */
        // PT132: MC 26.1 blit(Identifier, Int x, Int y, Int w, Int h, Float u, Float v, Float atlasW, Float atlasH)
        context.blit(
            Screen.MENU_BACKGROUND,
            this.left, 0,
            this.width, this.y,
            0.0f, 0.0f, 32.0f, 32.0f
        )
        context.blit(
            Screen.MENU_BACKGROUND,
            this.left, this.bottom,
            this.width, this.height - this.bottom,
            0.0f, bottom.toFloat(), 32.0f, 32.0f
        )
        Unit /* MC 26.1.x: GuiGraphicsExtractor.setColor removed */
        // PT137: fillGradient 26.1.x signature reduced to 6 args; RenderType param removed
        context.fillGradient(
            this.left,
            this.y,
            this.right,
            this.y + 4, -16777216, 0
        )
        context.fillGradient(
            this.left,
            this.bottom - 4,
            this.right,
            this.bottom, 0, -16777216
        )
    }

    // PT128: legacy renderListItems/renderItem replaced — AbstractSelectionList now provides extractListItems/extractItem stubs.
    override fun extractItem(
        context: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
        entry: T
    ) {
        entry.extractContent(context, mouseX, mouseY, focused == entry, delta)
    }

    override fun getRowLeft(): Int {
        return this.left
    }

    override fun getRowRight(): Int {
        return this.rowLeft + this.rowWidth
    }

    override fun getRowWidth(): Int {
        return this.width
    }

    override fun getRowTop(index: Int): Int {
        return this.y - scrollAmount.toInt() + (index * this.defaultEntryHeight)
    }

    override fun getRowBottom(index: Int): Int {
        return this.getRowTop(index) + this.defaultEntryHeight
    }

    override fun scrollBarX(): Int = this.left + this.width - this.scrollBarWidth

    abstract class Slot<T : Slot<T>>(): Entry<T>() {
        // Override render to show each individual element
    }
}