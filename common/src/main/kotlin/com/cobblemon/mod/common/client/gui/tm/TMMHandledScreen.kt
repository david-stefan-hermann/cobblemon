package com.cobblemon.mod.common.client.gui.tm

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.CobblemonNetwork
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.ColourLibrary
import com.cobblemon.mod.common.api.gui.MultiLineLabelK
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.reactive.SettableObservable
import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.api.tms.TechnicalMachine
import com.cobblemon.mod.common.api.tms.TechnicalMachineRecipe
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.block.TMBlock
import com.cobblemon.mod.common.block.entity.TMBlockEntity
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.gui.ExitButton
import com.cobblemon.mod.common.client.gui.MoveCategoryIcon
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.renderScaledGuiItemIcon
import com.cobblemon.mod.common.gui.TMMScreenHandler
import com.cobblemon.mod.common.net.messages.client.ui.CraftBlankTMPacket
import com.cobblemon.mod.common.net.messages.client.ui.CraftTMPacket
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.sounds.SoundManager
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class TMMHandledScreen(
        handler: TMMScreenHandler,
        val inventory: Inventory,
        title: Component
) : AbstractContainerScreen<TMMScreenHandler>(handler, inventory, title) {

    companion object {
        const val TEXTURE_HEIGHT = 222
        const val TEXTURE_WIDTH = 254

        const val TYPE_MENU_MODE = 1
        const val TM_BROWSING_MODE = 2

        val TYPE_SELECTION_BASE = cobblemonResource("textures/gui/tm/type_selection_base.png")
        val TM_SELECTION_BASE = cobblemonResource("textures/gui/tm/tm_selection_base.png")
        val TM_SELECTION_BORDER = cobblemonResource("textures/gui/tm/tm_selection_border.png")
        val TM_SELECTION_LISTING = cobblemonResource("textures/gui/tm/tm_selection_listing.png")
        val EJECT_BUTTON_SMALL = cobblemonResource("textures/gui/tm/eject_button_small.png")
        val EJECT_BUTTON_LARGE = cobblemonResource("textures/gui/tm/eject_button_large.png")
        val PARTY_SLOT = cobblemonResource("textures/gui/tm/party_slot.png")
        val TEACH_BUTTON = cobblemonResource("textures/gui/tm/teach_button.png")
    }

    var selectedTM: TechnicalMachine? = null
    var tmList: SettableObservable<MutableList<TechnicalMachine>> = SettableObservable(mutableListOf())
    var output: ItemStack? = null
    var mode: Int = TYPE_MENU_MODE
    var sortType: ElementalType? = null
    var selectedPokemon: Pokemon? = null
    var scroll: TMScrollingList? = null

    init {
        scroll = TMScrollingList(
                x = leftPos + 132,
                y = topPos + 45,
                parent = this
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (scroll?.isHovered(mouseX, mouseY) == true) {
            scroll!!.mouseClicked(mouseX, mouseY, button)
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        if (scroll?.isHovered(mouseX, mouseY) == true) {
            scroll!!.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
            return true
        }
        return false
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderBackground(graphics, mouseX, mouseY, delta)
        val x = (width - TEXTURE_WIDTH) / 2
        val y = (height - TEXTURE_HEIGHT) / 2

        blitk(
                matrixStack = graphics.pose(),
                texture = TYPE_SELECTION_BASE,
                x = x,
                y = y,
                width = TEXTURE_WIDTH,
                height = TEXTURE_HEIGHT
        )
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(graphics, mouseX, mouseY, delta)
        when (mode) {
            TYPE_MENU_MODE -> drawTypeMenu(graphics, delta, mouseX, mouseY)
            else -> drawTmSelectMenu(graphics, delta, mouseX, mouseY)
        }
        drawMoveInfo(graphics, delta, mouseX, mouseY)
        super.render(graphics, mouseX, mouseY, delta)
    }

    fun drawTypeMenu(graphics: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        val x = (width - TEXTURE_WIDTH) / 2
        val y = (height - TEXTURE_HEIGHT) / 2

        blitk(
                matrixStack = graphics.pose(),
                texture = TYPE_SELECTION_BASE,
                x = x,
                y = y,
                width = TEXTURE_WIDTH,
                height = TEXTURE_HEIGHT
        )

        // Additional logic for type menu...
    }

    fun drawTmSelectMenu(graphics: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        val x = (width - TEXTURE_WIDTH) / 2
        val y = (height - TEXTURE_HEIGHT) / 2

        blitk(
                matrixStack = graphics.pose(),
                texture = TM_SELECTION_BASE,
                x = x,
                y = y,
                width = TEXTURE_WIDTH,
                height = TEXTURE_HEIGHT
        )

        blitk(
                matrixStack = graphics.pose(),
                texture = TM_SELECTION_BORDER,
                x = x + 31,
                y = y + 14,
                width = 157,
                height = 101
        )
    }

    fun drawMoveInfo(graphics: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        val currentTm = selectedTM ?: return
        val x = (width - TEXTURE_WIDTH) / 2
        val y = (height - TEXTURE_HEIGHT) / 2

        // Draw logic for move info...
    }

    override fun removed() {
        super.removed()
        val inventory = menu.inventory
        if (inventory is TMBlockEntity.TMBlockInventory) {
            inventory.tmBlockEntity.blockState.setValue(TMBlock.ON, false)
        }
    }
}
