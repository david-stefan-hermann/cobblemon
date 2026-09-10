/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.tmmachine

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.client.gui.ScrollingWidget
import com.cobblemon.mod.common.client.gui.TypeIcon
import com.cobblemon.mod.common.client.gui.tmmachine.TypesScrollingWidget.ScrollSlotRow
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import net.minecraft.util.Mth

class TypesScrollingWidget(val pX: Int, val pY: Int, val setType: (ElementalType?) -> (Unit)): ScrollingWidget<ScrollSlotRow>(
    width = WIDTH,
    height = HEIGHT,
    left = pX,
    top = pY - HEIGHT,
    slotHeight = SLOT_SIZE + SLOT_SPACING
) {
    companion object {
        const val WIDTH = 113
        const val HEIGHT = 100
        const val SLOT_SIZE = 24
        const val SLOT_SPACING = 3
    }

    init {
        (listOf(null) + ElementalTypes.all()).chunked(4).forEachIndexed { index, listChunk ->
            addEntry(ScrollSlotRow(listChunk, setType))
        }
    }

    override fun scrollBarX(): Int = rowLeft + width - 3

    override fun renderScrollbar(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val xLeft = this.scrollBarX()
        val xRight = xLeft + 3
        val barHeight = this.bottom - this.y

        var yBottom = ((barHeight * barHeight).toFloat() / this.contentHeight().toFloat()).toInt()
        yBottom = Mth.clamp(yBottom, 32, barHeight - 8)
        var yTop = scrollAmount.toInt() * (barHeight - yBottom) / this.maxScrollAmount() + this.y
        if (yTop < this.y) {
            yTop = this.y
        }

        context.fill(xLeft, this.y, xRight, this.bottom, ARGB.color(255, 75, 75, 75)) // background
        context.fill(xLeft,yTop, xRight, yTop + yBottom, ARGB.color(255, 141, 141, 141)) // base
    }

    fun renderEntry(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, index: Int, x: Int, y: Int) {
        // PT137: AbstractSelectionList.getEntry removed — index children() list directly
        val entry =  children()[index] as ScrollSlotRow
        entry.slotX = x
        entry.slotY = y
        entry.renderRow( context, y, x, mouseX, mouseY)
    }

    // PT137: AbstractSelectionList.getEntry/renderListItems removed in 26.1; entries iterated via children()

    fun setDisabled(disabled: Boolean, vararg exclusions: ElementalType?) {
        children().forEach { row ->
            row.disabled = disabled
            row.disabledExclusions = exclusions.toSet() + null
        }
    }

    class ScrollSlotRow(val types:  List<ElementalType?>, val setType : (ElementalType?) -> Unit): Slot<ScrollSlotRow>() {
        companion object {
            private val allIconResource = cobblemonResource("textures/gui/tmmachine/type_slot_icon_all.png")
            private val slotResource = cobblemonResource("textures/gui/tmmachine/type_slot.png")
            private val slotDisabledResource = cobblemonResource("textures/gui/tmmachine/type_slot_disabled.png")
        }

        // PT145: 'x'/'y' clash with parent Slot's getX/setX/getY/setY in MC 26.1.x — renamed to slotX/slotY.
        var slotX: Int = 0
        var slotY: Int = 0
        var disabled: Boolean = false
        var disabledExclusions: Set<ElementalType?> = setOf(null)

        fun renderRow(context: GuiGraphicsExtractor, y: Int, x: Int, mouseX: Int, mouseY: Int) {
            types.forEachIndexed { index, type ->
                val matrices = context.pose()

                val startPosX = x + ((SLOT_SPACING + SLOT_SIZE) * index)
                val startPosY = y + SLOT_SPACING + 1

                val slotHovered = getHoveredSlotIndex(mouseX, mouseY) == index
                blitk(
                    matrixStack = matrices,
                    texture = if (disabled && !disabledExclusions.contains(type)) slotDisabledResource else slotResource,
                    x = startPosX,
                    y = startPosY,
                    width = SLOT_SIZE,
                    height = SLOT_SIZE,
                    vOffset = if (slotHovered) SLOT_SIZE else 0,
                    textureHeight = SLOT_SIZE * 2
                )

                if (type != null) {
                    TypeIcon(startPosX + 3, startPosY + 3, type).render(context)
                } else {
                    blitk(
                        matrixStack = matrices,
                        texture = allIconResource,
                        x = startPosX + 7,
                        y = startPosY + 7,
                        width = 11,
                        height = 11
                    )
                }
            }
        }

        // PT137: AbstractSelectionList.Entry.extractContent(GuiGraphicsExtractor, mouseX, mouseY, hovered, tickDelta) is the 5-arg abstract
        override fun extractContent(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, hovered: Boolean, tickDelta: Float) {
            renderRow(context, contentY, contentX, mouseX, mouseY)
        }

        override fun mouseClicked(event: MouseButtonEvent, fromOnClick: Boolean): Boolean {
        val mouseX = event.x
        val mouseY = event.y
        val button = event.button()
            var disable = false

            val hoverIndex = getHoveredSlotIndex(mouseX.toInt(), mouseY.toInt())
            if (hoverIndex > -1 && hoverIndex < types.size) {
                val type = types[hoverIndex]
                disable = disabled && !disabledExclusions.contains(type)
                if (!disable) {
                    setType.invoke(type)
                    Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.GUI_CLICK, 1.0F))
                }
            }

            return !disable
        }

        private fun getHoveredSlotIndex(mouseX: Int, mouseY: Int): Int {
            types.forEachIndexed { index, _ ->
                val startPosX = x + ((SLOT_SPACING + SLOT_SIZE) * index)
                val startPosY = y + SLOT_SPACING + 1

                if (mouseX in startPosX..(startPosX + SLOT_SIZE) && mouseY in startPosY..(startPosY + SLOT_SIZE)) {
                    return index
                }
            }
            return -1
        }

        override fun getNarration(): Component {
            if (types.isNotEmpty()) {
                return "${types[0]}-${types[types.size - 1]}".text()
            }
            return "".text()
        }
    }
}
