/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.tm

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.CobblemonNetwork
import com.cobblemon.mod.common.CobblemonSounds
import net.minecraft.resources.ResourceLocation
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
import com.sun.jna.platform.unix.X11.Drawable
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.sounds.SoundManager
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class TMMHandledScreen(
        val handler: TMMScreenHandler,
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
    val children: MutableMap<String, AbstractWidget> = mutableMapOf()
    var sortType: ElementalType? = null
    var selectedPokemon: Pokemon? = null
    var scroll: TMScrollingList? = null

    override fun init() {
        super.init()

        scroll = TMScrollingList(
            listX = leftPos + 5,
            listY = topPos + 8,
            parent = this
        )
    }

    private fun <T : AbstractWidget> addChild(widget: T, identifier: String) {
        if (children.contains(identifier)) return
        addRenderableWidget(widget)
        children[identifier] = widget
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

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (mode != TYPE_MENU_MODE) scroll?.mouseDragged(
            mouseX,
            mouseY,
            button,
            deltaX,
            deltaY
        )
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    fun drawBlankCraftingRecipe(context: GuiGraphics) {
        val x = (width - TEXTURE_WIDTH) / 2
        val y = (height - TEXTURE_HEIGHT) / 2
        // ingredient
        renderScaledGuiItemIcon(
            itemStack = Items.AMETHYST_SHARD.defaultInstance,
            x = (x + 197 + 36).toDouble(),
            y = (y + 127).toDouble(),
            matrixStack = context.pose(),
        )

        drawScaledText(
            context = context,
            text = lang("ui.tms.material_cost"),
            x = (x + 223),
            y = (y + 117.5),
            scale = 0.5f,
            centered = true
        )

        drawScaledText(
            context = context,
            text = Component.translatable(CobblemonItems.BLANK_TM.descriptionId).bold(),
            x = (x + 223),
            y = (y + 12),
            scale = 0.5f,
            centered = true
        )

        drawScaledText(
            context = context,
            text = lang("ui.power"),
            x = (x + 199),
            y = (y + 25),
            scale = 0.5f
        )

        drawScaledText(
            context = context,
            text = Component.literal("-"),
            x = (x + 241),
            y = (y + 25),
            scale = 0.5f,
            centered = true
        )

        drawScaledText(
            context = context,
            text = lang("ui.accuracy_short"),
            x = (x + 199),
            y = (y + 35),
            scale = 0.5f
        )

        drawScaledText(
            context = context,
            text = Component.literal("-"),
            x = (x + 241),
            y = (y + 35),
            scale = 0.5f,
            centered = true
        )

        drawScaledText(
            context = context,
            text = lang("ui.pp"),
            x = (x + 199),
            y = (y + 45),
            scale = 0.5f
        )

        drawScaledText(
            context = context,
            text = Component.literal("-"),
            x = (x + 241),
            y = (y + 45),
            scale = 0.5f,
            centered = true
        )

        drawScaledText(
            context = context,
            text = lang("ui.summary.title"),
            x = (x + 199),
            y = (y + 54.5),
            scale = 0.5f
        )

        val scale = 0.5f

        context.pose().pushPose()
        context.pose().scale(scale, scale, 1f)
        MultiLineLabelK.create(
            component = lang("ui.tms.blank_tm_desc"),
            width = 49 / scale,
            maxLines = 7
        ).renderLeftAligned(
            context = context,
            x = (x + 199) / scale,
            y = (y + 62) / scale,
            ySpacing = 5.5 / scale,
            colour = ColourLibrary.WHITE,
            shadow = true
        )
        context.pose().popPose()
    }

    fun drawMoveInfo(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        val currentTm = selectedTM
        if (currentTm == null) {
            return drawBlankCraftingRecipe(context)
        }

        val x = (width - TEXTURE_WIDTH) / 2
        val y = (height - TEXTURE_HEIGHT) / 2
        // blank tm
        renderScaledGuiItemIcon(
            itemStack = CobblemonItems.BLANK_TM.defaultInstance,
            x = (x + 197).toDouble(),
            y = (y + 127).toDouble(),
            matrixStack = context.pose(),
        )

        val typeGem = BuiltInRegistries.ITEM.get(ElementalTypes.get(currentTm.type)?.typeGem).defaultInstance
        // type gem
        renderScaledGuiItemIcon(
            itemStack = typeGem ?: ItemStack.EMPTY,
            x = (x + 197 + 18).toDouble(),
            y = (y + 127).toDouble(),
            matrixStack = context.pose(),
        )

        val recipe = currentTm.recipe ?: TechnicalMachineRecipe(ResourceLocation.tryParse("minecraft:air")!!, 1)
        // ingredient
        renderScaledGuiItemIcon(
            itemStack = BuiltInRegistries.ITEM.get(recipe.item).defaultInstance,
            x = (x + 197 + 36).toDouble(),
            y = (y + 127).toDouble(),
            zTranslation = 1f,
            matrixStack = context.pose(),
        )

        context.pose().pushPose()
        context.pose().translate(0f, 0f, 2f)

        if (recipe.count > 1) {
            val xIncrease = if (recipe.count < 10) 47 else 41
            drawScaledText(
                context = context,
                text = Component.literal(recipe.count.toString()),
                x = (x + 197 + xIncrease).toDouble(),
                y = (y + 136).toDouble(),
                shadow = true
            )
        }

        context.pose().popPose()

        drawScaledText(
            context = context,
            text = lang("ui.tms.material_cost"),
            x = (x + 223),
            y = (y + 117.5),
            scale = 0.5f,
            centered = true
        )

        drawScaledText(
            context = context,
            text = currentTm.translatedMoveName().bold(),
            x = (x + 223),
            y = (y + 12),
            scale = 0.5f,
            centered = true
        )

        MoveCategoryIcon(
            x = (x + 237.5),
            y = (y + 52.5),
            category = currentTm.moveName.damageCategory
        ).render(context)

        drawScaledText(
            context = context,
            text = lang("ui.power"),
            x = (x + 199),
            y = (y + 25),
            scale = 0.5f
        )

        drawScaledText(
            context = context,
            text = Component.literal(currentTm.moveName.power.toInt().toString()),
            x = (x + 241),
            y = (y + 25),
            scale = 0.5f,
            centered = true
        )

        drawScaledText(
            context = context,
            text = lang("ui.accuracy_short"),
            x = (x + 199),
            y = (y + 35),
            scale = 0.5f
        )

        drawScaledText(
            context = context,
            text = Component.literal(currentTm.moveName.accuracy.toInt().toString() + "%"),
            x = (x + 241),
            y = (y + 35),
            scale = 0.5f,
            centered = true
        )

        drawScaledText(
            context = context,
            text = lang("ui.pp"),
            x = (x + 199),
            y = (y + 45),
            scale = 0.5f
        )

        drawScaledText(
            context = context,
            text = Component.literal(currentTm.moveName.pp.toString()),
            x = (x + 241),
            y = (y + 45),
            scale = 0.5f,
            centered = true
        )

        drawScaledText(
            context = context,
            text = lang("ui.summary.title"),
            x = (x + 199),
            y = (y + 54.5),
            scale = 0.5f
        )

        val scale = 0.5f

        context.pose().pushPose()
        context.pose().scale(scale, scale, 1f)
        MultiLineLabelK.create(
            component = currentTm.moveName.description,
            width = 49 / scale,
            maxLines = 7
        ).renderLeftAligned(
            context = context,
            x = (x + 199) / scale,
            y = (y + 62) / scale,
            ySpacing = 5.5 / scale,
            colour = ColourLibrary.WHITE,
            shadow = true
        )
        context.pose().popPose()
    }

    fun drawTypeMenu(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        val x = (width - TEXTURE_WIDTH) / 2
        val y = (height - TEXTURE_HEIGHT) / 2

        blitk(
            matrixStack = context.pose(),
            texture = TYPE_SELECTION_BASE,
            x = x, // horizontal placement of GUI
            y = y, // vertical placement of GUI
            width = TEXTURE_WIDTH, // scale of the GUI width
            height = TEXTURE_HEIGHT // scale of the GUI height
        )

        fun drawTypeRow(drawX: Int, drawY: Int, types: List<ElementalType>) {
            var xOffset = 0
            for (type in types) {
                addChild(TypeButton(
                    pX = drawX + 32 + xOffset,
                    pY = drawY + 16,
                    type = type,
                    onPress = {
                        inventory.player.playSound(CobblemonSounds.GUI_CLICK, 1f, 1f)
                        Cobblemon.LOGGER.info("Filtered TMs for type: $type")
                        tmList.set(TechnicalMachine.filterTms(null, type, null).toMutableList())
                        sortType = type
                        mode = TM_BROWSING_MODE
                        clearGUI()
//                        addChild(scroll!!, "tm_scroll")
                        scroll?.scrollAmount = 0.0
                    }
                ), "${type.name}type")
                xOffset += 24
            }
        }

        drawTypeRow(x, y, listOf(
            ElementalTypes.NORMAL,
            ElementalTypes.FIRE,
            ElementalTypes.WATER,
            ElementalTypes.GRASS,
            ElementalTypes.ELECTRIC,
            ElementalTypes.ICE
        ))

        drawTypeRow(x + 12, y + 24, listOf(
            ElementalTypes.FIGHTING,
            ElementalTypes.POISON,
            ElementalTypes.GROUND,
            ElementalTypes.FLYING,
        ))

        drawTypeRow(x, y + 48, listOf(
            ElementalTypes.PSYCHIC,
            ElementalTypes.BUG,
            ElementalTypes.ROCK,
            ElementalTypes.GHOST,
        ))

        drawTypeRow(x + 12, y + 72, listOf(
            ElementalTypes.DRAGON,
            ElementalTypes.DARK,
            ElementalTypes.STEEL,
            ElementalTypes.FAIRY,
        ))

        addChild(
            DiscButton(
                pX = x + 134,
                pY = y + 50,
                onPress = {
                    inventory.player.playSound(CobblemonSounds.GUI_CLICK, 1f, 1f)
                    val test = Moves.getByName("thunderbolt")
                    tmList.set(TechnicalMachine.filterTms(null, null, null).toMutableList())
                    Cobblemon.LOGGER.info("tmList updated with ${tmList.get().size} TMs.")
                    mode = TM_BROWSING_MODE
                    sortType = null
//                    addChild(scroll!!, "tm_scroll")
                    scroll?.scrollAmount = 0.0
                    clearGUI()
                }
            ), "disc"
        )

    }

    fun drawTmSelectMenu(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        val x = (width - TEXTURE_WIDTH) / 2
        val y = (height - TEXTURE_HEIGHT) / 2

        blitk(
            matrixStack = context.pose(),
            texture = TM_SELECTION_BASE,
            x = x, // horizontal placement of GUI
            y = y, // vertical placement of GUI

            width = TEXTURE_WIDTH, // scale of the GUI width
            height = TEXTURE_HEIGHT // scale of the GUI height
        )

        blitk(
            matrixStack = context.pose(),
            texture = TM_SELECTION_BORDER,
            x = x + 31, // horizontal placement of GUI
            y = y + 14, // vertical placement of GUI
            width = 157,
            height = 101
        )

        val displayType = sortType

        addChild(
            TypeButton(
                pX = x + 31,
                pY = y + 16,
                type = displayType,
                onPress = {
                    mode = TYPE_MENU_MODE
                    inventory.player.playSound(CobblemonSounds.GUI_CLICK, 1f, 1f)
                    clearGUI()
                }
            ), "returnTypeMenu"
        )

        scroll?.render(context, mouseX, mouseY, delta)
    }

    fun clearGUI() {
        for (id in children.keys) {
            removeWidget(children[id]) // Remove the widget from rendering
        }
        children.clear() // Clear the map
    }

    override fun renderBg(context: GuiGraphics, delta: Float, mouseX: Int, mouseY: Int) {
        //super.renderBackground(context, mouseX, mouseY, delta) // todo why was this here in the old code? Seems to cause a crash now...
        val x = (width - TEXTURE_WIDTH) / 2
        val y = (height - TEXTURE_HEIGHT) / 2


        blitk(
            matrixStack = context.pose(),
            texture = TYPE_SELECTION_BASE,
            x = x, // horizontal placement of GUI
            y = y, // vertical placement of GUI

            width = TEXTURE_WIDTH, // scale of the GUI width
            height = TEXTURE_HEIGHT // scale of the GUI height
        )

        when (mode) {
            TYPE_MENU_MODE -> drawTypeMenu(context, delta, mouseX, mouseY)
            else -> drawTmSelectMenu(context, delta, mouseX, mouseY)
        }
        drawMoveInfo(context, delta, mouseX, mouseY)
        inventory.setChanged()

        addChild(
            EjectButton(
                pX = x + 196,
                pY = y + 168,
                small = false,
                onPress = {
                    inventory.player.playSound(CobblemonSounds.GUI_CLICK, 1f, 1f)
                    val currentTm = selectedTM ?: return@EjectButton CobblemonNetwork.sendToServer(CraftBlankTMPacket(handler.input.getItem(2)))

                    CobblemonNetwork.sendToServer(
                        CraftTMPacket(
                            currentTm,
                            handler.input.getItem(0),
                            handler.input.getItem(1),
                            handler.input.getItem(2)
                        )
                    )


                    this.handler.syncState()
                }
            ), "eject"
        )

        addChild(
            ExitButton(
                pX = x + 228,
                pY = y + 185,
                onPress = {
                    this.onClose()
                }
            ), "exit"
        )

        val startY = y + 13
        val offset = 28
        var iterations = 0
        CobblemonClient.storage.myParty.forEach { pokemon ->
            if (pokemon != null) {
                addChild(TMPartySlotWidget(
                    pX = x - 23,
                    pY = startY + (offset * iterations),
                    pokemon = pokemon,
                    onPress = {
                        selectedPokemon = if (selectedPokemon == pokemon) null else pokemon
                    }
                ), "partyslot_${pokemon.uuid}")
            }
            iterations++
        }
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

    override fun removed() {
        super.removed()
        val tmInventory = inventory as? TMBlockEntity.TMBlockInventory
        val tmBlockEntity = tmInventory?.blockEntity
        tmBlockEntity?.blockState?.setValue(TMBlock.ON, false)
    }

    override fun renderBlurredBackground(partialTick: Float) {}
    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {}
}
