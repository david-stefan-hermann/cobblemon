package com.cobblemon.mod.common.client.gui.tm

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.tms.TechnicalMachine
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractSelectionList
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component

class TMScrollingList(
        val listX: Int,
        val listY: Int,
        val parent: TMMHandledScreen
) : AbstractSelectionList<TMScrollingList.TMScrollingListEntry>(
        Minecraft.getInstance(),
        WIDTH, // width
        HEIGHT, // height
        0, // top
        SLOT_HEIGHT // slot height
) {

    companion object {
        const val WIDTH = 140
        const val HEIGHT = 78
        const val SLOT_WIDTH = 140
        const val SLOT_HEIGHT = 20
        const val SLOT_SPACING = 3
        const val SCALE = 0.5f
    }

    private var scrolling = false

    override fun getRowWidth() = SLOT_WIDTH

    override fun renderListBackground(guiGraphics: GuiGraphics) {
        super.renderListBackground(guiGraphics)
    }

    override fun renderHeader(graphics: GuiGraphics, x: Int, y: Int) {}

    init {
        correctSize()

        parent.tmList.subscribeIncludingCurrent { tmList ->
            val currentEntries = children()
            val newTMs = tmList.filter { tm -> currentEntries.none { it.tm.id == tm.id } }
            val removedTMs = currentEntries.filter { entry -> tmList.none { it.id == entry.tm.id } }

            removedTMs.forEach(this::removeEntry)
            newTMs.forEach { tm -> addEntry(TMScrollingListEntry(tm, parent)) }
        }
    }

    override fun getScrollbarPosition() = listX + width - 3

    private fun correctSize() {
        /*setRenderPosition(
                x,
                y,
                x + WIDTH,
                y + HEIGHT
        )*/
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
    }



    public override fun addEntry(entry: TMScrollingListEntry) = super.addEntry(entry)
    public override fun removeEntry(entry: TMScrollingListEntry) = super.removeEntry(entry)

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        correctSize()

        graphics.enableScissor(listX, listY, listX + width, listY + height)
        super.renderWidget(graphics, mouseX, mouseY, delta) // Call the parent method for default rendering
        graphics.disableScissor()
    }

    fun isHovered(mouseX: Double, mouseY: Double): Boolean {
        return mouseX.toFloat() in (listX.toFloat()..(listX.toFloat() + WIDTH)) && mouseY.toFloat() in (listY.toFloat()..(listY.toFloat() + HEIGHT))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        updateScrollingState(mouseX, mouseY)
        if (scrolling) {
            focused = getEntryAtPosition(mouseX, mouseY)
            isDragging = true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (scrolling) {
            if (mouseY < listY) {
                scrollAmount = 0.0
            } else if (mouseY > bottom) {
                scrollAmount = maxScroll.toDouble()
            } else {
                scrollAmount += deltaY
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
        TODO("Not yet implemented")
    }

    private fun updateScrollingState(mouseX: Double, mouseY: Double) {
        scrolling = mouseX >= scrollbarPosition.toDouble()
                && mouseX < (scrollbarPosition + 3).toDouble()
                && mouseY >= listY
                && mouseY < bottom
    }


    class TMScrollingListEntry(val tm: TechnicalMachine, private val parent: TMMHandledScreen) : Entry<TMScrollingListEntry>() {

        private val tmButton: TMListingButton = TMListingButton(
                pX = 0,
                pY = 0,
                onPress = {
                    parent.selectedTM = tm
                    parent.inventory.player.playSound(CobblemonSounds.GUI_CLICK, 1f, 1f)
                },
                tm = tm
        )

        override fun render(
                graphics: GuiGraphics,
                index: Int,
                rowTop: Int,
                rowLeft: Int,
                rowWidth: Int,
                rowHeight: Int,
                mouseX: Int,
                mouseY: Int,
                isHovered: Boolean,
                delta: Float
        ) {
            val x = rowLeft
            val y = rowTop
            val matrixStack = graphics.pose()

            // Render just this TMListingButton
            tmButton.setPosition(x + 1, y)
            tmButton.render(graphics, mouseX, mouseY, delta)
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (tmButton.isMouseOver(mouseX, mouseY)) {
                tmButton.onPress()
                return true
            }
            return false
        }
    }
}
