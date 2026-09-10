/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.pokedex.widgets

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.gui.ScrollingWidget
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.pokemon.FormData
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import kotlin.math.max

class FormsWidget (val pX: Int, val pY: Int, val setFormData : (String) -> (Unit)): ScrollingWidget<FormsWidget.FormSlot>(
        left = pX,
        top = pY,
        width = PokedexGUIConstants.POKEMON_FORMS_WIDTH,
        height = PokedexGUIConstants.POKEMON_FORMS_HEIGHT,
        slotHeight = 15
) {

    override fun addEntry(entry: FormSlot): Int {
        return super.addEntry(entry)
    }

    fun setForms(forms: Collection<String>){
        clearEntries()
        forms.forEach {
            addEntry(
                FormSlot(it, setFormData)
            )
        }
    }

    override fun scrollBarX(): Int {
        return left + width - scrollBarWidth
    }

    // PT145: AbstractSelectionList.getMaxScroll removed in MC 26.1.x.
    fun getMaxScroll() = max(this.height.toDouble(), (this.contentHeight() - (this.bottom - this.y - 4)).toDouble()).toInt()

    class FormSlot(val form : String, val setFormData: (String) -> Unit) : Slot<FormSlot>() {

        companion object {
            private val scrollSlotResource = cobblemonResource("textures/gui/pokedex/scroll_slot_base.png")// Render Scroll Slot Background
        }

        override fun extractContent(
            context: GuiGraphicsExtractor,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            tickDelta: Float
        ) {
            val index = 0
            val y = contentY
            val x = contentX
            val entryWidth = width
            val entryHeight = contentHeight

            val textScale = 1F
            blitk(
                matrixStack = context.pose(),
                texture = scrollSlotResource,
                x = x,
                y = y,
                width = entryWidth,
                height = entryHeight
            )

            drawScaledText(
                context = context,
                text = form.text(),
                x = x + entryWidth/2,
                y = y + entryHeight/2 - 5,
                scale = textScale,
                centered = true
            )
        }

        override fun getNarration(): Component {
            return Component.literal(form)
        }

        override fun mouseClicked(event: MouseButtonEvent, fromOnClick: Boolean): Boolean {
        val mouseX = event.x
        val mouseY = event.y
        val button = event.button()
            setFormData.invoke(form)
            return true
        }
    }
}