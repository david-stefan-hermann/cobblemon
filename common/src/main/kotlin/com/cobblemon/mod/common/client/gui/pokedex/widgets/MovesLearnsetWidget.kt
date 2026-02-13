/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.pokedex.widgets

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.pokedex.entry.PokedexForm
import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.gui.MoveCategoryIcon
import com.cobblemon.mod.common.client.gui.ScrollingWidget
import com.cobblemon.mod.common.client.gui.TypeIcon
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.HALF_OVERLAY_HEIGHT
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.HALF_OVERLAY_WIDTH
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.POKEMON_DESCRIPTION_PADDING
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.SCALE
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUIConstants.SCROLL_BAR_WIDTH
import com.cobblemon.mod.common.client.gui.pokedex.ScaledButton
import com.cobblemon.mod.common.client.gui.summary.widgets.SoundlessWidget
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.drawScaledTextJustifiedRight
import com.cobblemon.mod.common.pokemon.FormData
import com.cobblemon.mod.common.pokemon.Species
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import java.math.RoundingMode
import java.text.DecimalFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.util.FastColor
import net.minecraft.util.Mth

class MovesLearnsetWidget(val pX: Int, val pY: Int) : SoundlessWidget(
    pX,
    pY,
    HALF_OVERLAY_WIDTH,
    HALF_OVERLAY_HEIGHT,
    Component.literal("MovesLearnset")
) {
    companion object {
        private const val LIST_TOP_OFFSET = -15
        private const val LIST_HEIGHT = 54
        private const val LIST_SLOT_HEIGHT = 10
        private const val LIST_SIDE_PADDING = 4

        private const val DATA_TOP_OFFSET = 108
        private const val DATA_ROW_HEIGHT = 10
        private const val DATA_LABEL_OFFSET_X = 14
        private const val DATA_ICON_SIZE = 10
        private const val DATA_INFO_TOP_OFFSET = 7

        private const val DESCRIPTION_TOP_OFFSET = 75
        private const val DESCRIPTION_HEIGHT = 38

        private val overlayResource = cobblemonResource("textures/gui/pokedex/pokedex_screen_info_overlay.png")
        private val arrowFormLeft = cobblemonResource("textures/gui/pokedex/forms_arrow_left.png")
        private val arrowFormRight = cobblemonResource("textures/gui/pokedex/forms_arrow_right.png")
        private val typeBar = cobblemonResource("textures/gui/pokedex/type_bar.png")
        private val typeBarDouble = cobblemonResource("textures/gui/pokedex/type_bar_double.png")
        private val filterArrowLeft = cobblemonResource("textures/gui/pokedex/info_arrow_left.png")
        private val filterArrowRight = cobblemonResource("textures/gui/pokedex/info_arrow_right.png")
        private val sortAlphaIcon = cobblemonResource("textures/gui/pokedex/moves_sort_alpha.png")
        private val sortTypeIcon = cobblemonResource("textures/gui/pokedex/moves_sort_type.png")
        private val sortSourceIcon = cobblemonResource("textures/gui/pokedex/moves_sort_source.png")
        private val tmLockedIcon = cobblemonResource("textures/gui/trade/trade_slot_icon_locked.png")
        private val movesPowerIconResource = cobblemonResource("textures/gui/summary/summary_moves_icon_power.png")
        private val movesAccuracyIconResource = cobblemonResource("textures/gui/summary/summary_moves_icon_accuracy.png")
        private val movesCategoryIconResource = cobblemonResource("textures/gui/summary/summary_moves_icon_category.png")

        // Match PokemonInfoWidget placement
        private const val TYPE_BAR_Y = 14
        private const val TYPE_BAR_HEIGHT = 25
        private const val TYPE_ICON_X = 3
        private const val TYPE_ICON_Y = 17
        private const val FORM_LABEL_X = 85
        private const val FORM_LABEL_Y = 14
        private const val FORM_ARROW_LEFT_X = 56F
        private const val FORM_ARROW_RIGHT_X = 107F
        private const val FORM_ARROW_Y = 15F
        private const val FORM_ARROW_WIDTH = 10
        private const val FORM_ARROW_HEIGHT = 16

        // Adjustable filter arrow placement
        private const val FILTER_ARROW_LEFT_X = 56F
        private const val FILTER_ARROW_RIGHT_X = 107F
        private const val FILTER_ARROW_Y = 28F
        private const val FILTER_ARROW_WIDTH = 7
        private const val FILTER_ARROW_HEIGHT = 10
        private const val FILTER_LABEL_X = 84
        private const val FILTER_LABEL_Y = 26

        // Sort button placement
        private const val SORT_BUTTON_X = 125F
        private const val SORT_BUTTON_Y = 27.5F
        private const val SORT_BUTTON_WIDTH = 20
        private const val SORT_BUTTON_HEIGHT = 20
    }

    private val listWidget = LearnsetMovesScrollingWidget(
        pX + LIST_SIDE_PADDING,
        pY + LIST_TOP_OFFSET,
        HALF_OVERLAY_WIDTH - (LIST_SIDE_PADDING * 2),
        LIST_HEIGHT
    ) { entry -> selectMove(entry) }

    private val descriptionWidget = MoveDescriptionWidget(
        pX + LIST_SIDE_PADDING + 65,
        pY + DESCRIPTION_TOP_OFFSET,
        HALF_OVERLAY_WIDTH - (LIST_SIDE_PADDING * 2) - 50,
        DESCRIPTION_HEIGHT
    )

    private var moveEntries: List<LearnsetMoveEntry> = emptyList()
    private var filteredEntries: List<LearnsetMoveEntry> = emptyList()
    private var selectedEntry: LearnsetMoveEntry? = null
    private var speciesName: MutableComponent = Component.literal("")
    private var speciesNumber: MutableComponent = "0000".text()
    private var formLabel: MutableComponent = Component.literal("")
    private var primaryType: ElementalType? = null
    private var secondaryType: ElementalType? = null
    private var availableForms: List<PokedexForm> = emptyList()
    private var onFormChange: ((Boolean) -> Unit)? = null
    private var filterIndex = 0
    private var sortMode = LearnsetSort.ALPHABETICAL

    private val decimalFormat = DecimalFormat("#.##").also { it.roundingMode = RoundingMode.CEILING }

    private val formLeftButton: ScaledButton = ScaledButton(
        pX + FORM_ARROW_LEFT_X,
        pY + FORM_ARROW_Y,
        FORM_ARROW_WIDTH,
        FORM_ARROW_HEIGHT,
        arrowFormLeft,
        clickAction = { onFormChange?.invoke(false) }
    ).apply { addWidget(this) }

    private val formRightButton: ScaledButton = ScaledButton(
        pX + FORM_ARROW_RIGHT_X,
        pY + FORM_ARROW_Y,
        FORM_ARROW_WIDTH,
        FORM_ARROW_HEIGHT,
        arrowFormRight,
        clickAction = { onFormChange?.invoke(true) }
    ).apply { addWidget(this) }

    private val filterLeftButton: ScaledButton = ScaledButton(
        pX + FILTER_ARROW_LEFT_X,
        pY + FILTER_ARROW_Y,
        FILTER_ARROW_WIDTH,
        FILTER_ARROW_HEIGHT,
        filterArrowLeft,
        clickAction = { cycleFilter(false) }
    ).apply { addWidget(this) }

    private val filterRightButton: ScaledButton = ScaledButton(
        pX + FILTER_ARROW_RIGHT_X,
        pY + FILTER_ARROW_Y,
        FILTER_ARROW_WIDTH,
        FILTER_ARROW_HEIGHT,
        filterArrowRight,
        clickAction = { cycleFilter(true) }
    ).apply { addWidget(this) }

    private val sortButton: ScaledButton = ScaledButton(
        pX + SORT_BUTTON_X,
        pY + SORT_BUTTON_Y,
        SORT_BUTTON_WIDTH,
        SORT_BUTTON_HEIGHT,
        sortAlphaIcon,
        clickAction = { toggleSort() }
    ).apply { addWidget(this) }

    init {
        addWidget(listWidget)
        addWidget(descriptionWidget)
    }

    fun setLearnset(
        species: Species,
        form: FormData,
        tmUnlocked: Boolean,
        availableForms: List<PokedexForm>,
        selectedForm: PokedexForm,
        onFormChange: (Boolean) -> Unit
    ) {
        speciesName = species.translatedName
        speciesNumber = species.nationalPokedexNumber.toString().padStart(4, '0').text()
        primaryType = form.primaryType
        secondaryType = form.secondaryType
        this.availableForms = availableForms
        this.onFormChange = onFormChange
        updateFormLabel(species, selectedForm)
        updateFormButtons()

        moveEntries = buildLearnsetEntries(form, tmUnlocked)
        applyFilter()
        selectMove(null)
    }

    override fun renderWidget(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val matrices = context.pose()

        blitk(
            matrixStack = matrices,
            texture = overlayResource,
            x = pX,
            y = pY,
            width = HALF_OVERLAY_WIDTH,
            height = HALF_OVERLAY_HEIGHT
        )

        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = speciesNumber.bold(),
            x = pX + 3,
            y = pY + 1,
            shadow = true
        )

        if (!speciesName.string.isBlank()) {
            drawScaledText(
                context = context,
                font = CobblemonResources.DEFAULT_LARGE,
                text = speciesName.bold(),
                x = pX + 26,
                y = pY + 1,
                colour = 0x606B6E
            )
        }

        blitk(
            matrixStack = context.pose(),
            texture = if (secondaryType != null) typeBarDouble else typeBar,
            x = pX,
            y = pY + TYPE_BAR_Y,
            width = HALF_OVERLAY_WIDTH,
            height = TYPE_BAR_HEIGHT
        )

        if (primaryType != null) {
            TypeIcon(
                x = pX + TYPE_ICON_X,
                y = pY + TYPE_ICON_Y,
                type = primaryType!!,
                secondaryType = secondaryType
            ).render(context)
        }

        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = formLabel.bold(),
            x = pX + FORM_LABEL_X,
            y = pY + FORM_LABEL_Y,
            shadow = true,
            centered = true
        )

        formLeftButton.render(context, mouseX, mouseY, delta)
        formRightButton.render(context, mouseX, mouseY, delta)

        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = currentFilter().label.bold(),
            x = pX + FILTER_LABEL_X,
            y = pY + FILTER_LABEL_Y,
            shadow = true,
            centered = true
        )

        filterLeftButton.render(context, mouseX, mouseY, delta)
        filterRightButton.render(context, mouseX, mouseY, delta)
        sortButton.render(context, mouseX, mouseY, delta)

        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = lang("ui.moves").bold(),
            x = pX + 4,
            y = pY + 11,
            shadow = true
        )

        if (filteredEntries.isEmpty()) {
            drawScaledText(
                context = context,
                text = Component.literal("No moves available."),
                x = pX + (HALF_OVERLAY_WIDTH / 2),
                y = pY + LIST_TOP_OFFSET + (LIST_HEIGHT / 2) - 3,
                shadow = false,
                colour = 0x606B6E,
                scale = SCALE,
                centered = true
            )
        } else {
            listWidget.renderWidget(context, mouseX, mouseY, delta)
        }

        renderDataSection(context)

        descriptionWidget.renderWidget(context, mouseX, mouseY, delta)
    }

    private fun renderDataSection(context: GuiGraphics) {
        val dataTop = pY + DATA_TOP_OFFSET
        val dataRight = pX + HALF_OVERLAY_WIDTH - 70
        val entry = selectedEntry
        val move = entry?.move
        val showMoveInfo = entry?.isDiscovered == true

        drawScaledText(
            context = context,
            font = CobblemonResources.DEFAULT_LARGE,
            text = Component.literal("Data").bold(),
            x = pX + 4,
            y = dataTop - 10,
            shadow = true
        )

        blitk(
            matrixStack = context.pose(),
            texture = movesPowerIconResource,
            x = (pX + 6) / SCALE,
            y = (dataTop + 1 + DATA_INFO_TOP_OFFSET) / SCALE,
            width = DATA_ICON_SIZE,
            height = DATA_ICON_SIZE,
            scale = SCALE
        )

        blitk(
            matrixStack = context.pose(),
            texture = movesAccuracyIconResource,
            x = (pX + 6) / SCALE,
            y = (dataTop + 1 + DATA_ROW_HEIGHT + DATA_INFO_TOP_OFFSET) / SCALE,
            width = DATA_ICON_SIZE,
            height = DATA_ICON_SIZE,
            scale = SCALE
        )

        blitk(
            matrixStack = context.pose(),
            texture = movesCategoryIconResource,
            x = (pX + 6) / SCALE,
            y = (dataTop + 1 + (DATA_ROW_HEIGHT * 2) + DATA_INFO_TOP_OFFSET) / SCALE,
            width = DATA_ICON_SIZE,
            height = DATA_ICON_SIZE,
            scale = SCALE
        )

        drawScaledText(
            context = context,
            text = lang("ui.power"),
            x = pX + DATA_LABEL_OFFSET_X,
            y = dataTop + 1 + DATA_INFO_TOP_OFFSET,
            scale = SCALE,
            shadow = true
        )

        drawScaledText(
            context = context,
            text = lang("ui.accuracy"),
            x = pX + DATA_LABEL_OFFSET_X,
            y = dataTop + 1 + DATA_ROW_HEIGHT + DATA_INFO_TOP_OFFSET,
            scale = SCALE,
            shadow = true
        )

        drawScaledText(
            context = context,
            text = Component.literal("Category"),
            x = pX + DATA_LABEL_OFFSET_X,
            y = dataTop + 1 + (DATA_ROW_HEIGHT * 2) + DATA_INFO_TOP_OFFSET,
            scale = SCALE,
            shadow = true
        )

        val powerText = if (showMoveInfo && move != null && move.power.toInt() > 0) move.power.toInt().toString() else "-"
        drawScaledTextJustifiedRight(
            context = context,
            text = powerText.text(),
            x = dataRight,
            y = dataTop + 1  + DATA_INFO_TOP_OFFSET,
            scale = SCALE,
            shadow = true
        )

        val accuracyText = if (showMoveInfo && move != null) formatAccuracy(move.accuracy) else "-"
        drawScaledTextJustifiedRight(
            context = context,
            text = accuracyText.text(),
            x = dataRight,
            y = dataTop + 1 + DATA_ROW_HEIGHT + DATA_INFO_TOP_OFFSET,
            scale = SCALE,
            shadow = true
        )

        val categoryText = if (showMoveInfo && move != null) move.damageCategory.displayName else "-".text()
        drawScaledTextJustifiedRight(
            context = context,
            text = categoryText as MutableComponent,
            x = dataRight,
            y = dataTop + 1 + (DATA_ROW_HEIGHT * 2) + DATA_INFO_TOP_OFFSET,
            scale = SCALE,
            shadow = true
        )

        if (showMoveInfo && move != null) {
            MoveCategoryIcon(
                x = pX + 6,
                y = dataTop + 2 + (DATA_ROW_HEIGHT * 2) + DATA_INFO_TOP_OFFSET,
                category = move.damageCategory
            ).render(context)
        }
    }

    private fun formatAccuracy(input: Double): String {
        if (input <= 0) return "-"
        return "${decimalFormat.format(input)}%"
    }

    private fun applyFilter() {
        val filtered = when (currentFilter()) {
            LearnsetFilter.ALL -> moveEntries
            LearnsetFilter.LEVEL_UP -> moveEntries.filter { it.source == LearnsetSource.LEVEL_UP }
            LearnsetFilter.TM -> moveEntries.filter { it.source == LearnsetSource.TM }
            LearnsetFilter.EGG -> moveEntries.filter { it.source == LearnsetSource.EGG }
            LearnsetFilter.LEGACY -> moveEntries.filter { it.source == LearnsetSource.LEGACY }
        }
        val sorted = sortEntries(filtered)
        filteredEntries = sorted
        listWidget.setEntries(sorted)
        if (selectedEntry !in sorted) {
            selectMove(null)
        } else {
            listWidget.setSelectedEntry(selectedEntry)
        }
    }

    private fun sortEntries(entries: List<LearnsetMoveEntry>): List<LearnsetMoveEntry> {
        return when (sortMode) {
            LearnsetSort.ALPHABETICAL -> entries.sortedWith(
                compareBy(
                    { it.move.displayName.string.lowercase() },
                    { it.level ?: Int.MAX_VALUE }
                )
            )
            LearnsetSort.TYPE -> entries.sortedWith(
                compareBy(
                    { it.move.elementalType.displayName.string.lowercase() },
                    { it.move.displayName.string.lowercase() },
                    { it.level ?: Int.MAX_VALUE }
                )
            )
            LearnsetSort.SOURCE -> entries.sortedWith(
                compareBy(
                    { sourceOrder(it.source) },
                    { it.level ?: Int.MAX_VALUE },
                    { it.move.displayName.string.lowercase() }
                )
            )
        }
    }

    private fun cycleFilter(next: Boolean) {
        val total = LearnsetFilter.entries.size
        filterIndex = if (next) {
            (filterIndex + 1) % total
        } else {
            (filterIndex - 1 + total) % total
        }
        applyFilter()
    }

    private fun toggleSort() {
        sortMode = when (sortMode) {
            LearnsetSort.ALPHABETICAL -> LearnsetSort.TYPE
            LearnsetSort.TYPE -> LearnsetSort.SOURCE
            LearnsetSort.SOURCE -> LearnsetSort.ALPHABETICAL
        }
        sortButton.resource = when (sortMode) {
            LearnsetSort.ALPHABETICAL -> sortAlphaIcon
            LearnsetSort.TYPE -> sortTypeIcon
            LearnsetSort.SOURCE -> sortSourceIcon
        }
        applyFilter()
    }

    private fun sourceOrder(source: LearnsetSource): Int {
        return when (source) {
            LearnsetSource.LEVEL_UP -> 0
            LearnsetSource.TM -> 1
            LearnsetSource.TUTOR -> 2
            LearnsetSource.EGG -> 3
            LearnsetSource.EVOLUTION -> 4
            LearnsetSource.FORM_CHANGE -> 5
            LearnsetSource.SPECIAL -> 6
            LearnsetSource.LEGACY -> 7
        }
    }

    private fun currentFilter(): LearnsetFilter {
        return LearnsetFilter.entries[filterIndex.coerceIn(0, LearnsetFilter.entries.lastIndex)]
    }

    private fun updateFormButtons() {
        val hasForms = availableForms.size > 1
        formLeftButton.isVisible = hasForms
        formRightButton.isVisible = hasForms
        formLeftButton.active = hasForms
        formRightButton.active = hasForms
    }

    private fun updateFormLabel(species: Species, form: PokedexForm) {
        val formName = if (form.displayForm.equals("normal", ignoreCase = true)) {
            ""
        } else {
            "-${form.displayForm.lowercase().replace("-", "")}"
        }
        val label = lang("ui.pokedex.info.form.${species}${formName}")
        formLabel = if (label.string.isBlank() || label.string.startsWith("cobblemon.ui.pokedex.info.form.")) {
            Component.literal(form.displayForm)
        } else {
            label
        }
    }

    private fun selectMove(entry: LearnsetMoveEntry?) {
        selectedEntry = if (selectedEntry == entry || entry == null) null else entry
        listWidget.setSelectedEntry(selectedEntry)

        if (selectedEntry?.isDiscovered == true) {
            val description = selectedEntry!!.move.description.string
            descriptionWidget.showPlaceholder = false
            descriptionWidget.setText(listOf(description))
        } else {
            descriptionWidget.showPlaceholder = true
            descriptionWidget.setText(emptyList())
        }
    }

    private fun buildLearnsetEntries(form: FormData, tmUnlocked: Boolean): List<LearnsetMoveEntry> {
        val entries = linkedMapOf<String, LearnsetMoveEntry>()

        fun addEntry(move: MoveTemplate, source: LearnsetSource, level: Int? = null, tmLocked: Boolean = false) {
            val key = move.name
            if (!entries.containsKey(key)) {
                entries[key] = LearnsetMoveEntry(move, source, level, tmLocked)
            }
        }

        form.moves.levelUpMoves.toSortedMap().forEach { (level, moves) ->
            moves.forEach { move -> addEntry(move, LearnsetSource.LEVEL_UP, level = level) }
        }

        form.moves.tmMoves.sortedBy { it.displayName.string }.forEach { move ->
            addEntry(move, LearnsetSource.TM, tmLocked = !tmUnlocked)
        }

        form.moves.tutorMoves.sortedBy { it.displayName.string }.forEach { move ->
            addEntry(move, LearnsetSource.TUTOR)
        }

        form.moves.eggMoves.sortedBy { it.displayName.string }.forEach { move ->
            addEntry(move, LearnsetSource.EGG)
        }

        form.moves.evolutionMoves.sortedBy { it.displayName.string }.forEach { move ->
            addEntry(move, LearnsetSource.EVOLUTION)
        }

        form.moves.formChangeMoves.sortedBy { it.displayName.string }.forEach { move ->
            addEntry(move, LearnsetSource.FORM_CHANGE)
        }

        form.moves.specialMoves.sortedBy { it.displayName.string }.forEach { move ->
            addEntry(move, LearnsetSource.SPECIAL)
        }

        form.moves.legacyMoves.sortedBy { it.displayName.string }.forEach { move ->
            addEntry(move, LearnsetSource.LEGACY)
        }

        return entries.values.toList()
    }

    private enum class LearnsetSource(val label: String) {
        LEVEL_UP("Lv"),
        TM("TM"),
        TUTOR("Tutor"),
        EGG("Egg"),
        EVOLUTION("Evo"),
        FORM_CHANGE("Form"),
        SPECIAL("Spec"),
        LEGACY("Legacy")
    }

    private enum class LearnsetFilter(val label: MutableComponent) {
        ALL(Component.literal("All")),
        LEVEL_UP(Component.literal("Level-Up")),
        TM(Component.literal("TM")),
        EGG(Component.literal("Egg")),
        LEGACY(Component.literal("Legacy"))
    }

    private enum class LearnsetSort {
        ALPHABETICAL,
        TYPE,
        SOURCE
    }

    private data class LearnsetMoveEntry(
        val move: MoveTemplate,
        val source: LearnsetSource,
        val level: Int? = null,
        val tmLocked: Boolean = false
    ) {
        val isDiscovered: Boolean = !tmLocked
    }

    // todo fix the scrolling area height
    private class LearnsetMovesScrollingWidget(
        val pX: Int,
        val pY: Int,
        listWidth: Int,
        listHeight: Int,
        val onSelect: (LearnsetMoveEntry?) -> Unit
    ) : ScrollingWidget<LearnsetMovesScrollingWidget.MoveEntrySlot>(
        width = listWidth,
        height = listHeight,
        left = pX,
        top = pY,
        slotHeight = LIST_SLOT_HEIGHT
    ) {
        private var selectedEntry: LearnsetMoveEntry? = null

        fun setEntries(entries: List<LearnsetMoveEntry>) {
            clearEntries()
            entries.forEach { entry -> addEntry(MoveEntrySlot(entry, this)) }
            scrollAmount = 0.0
        }

        fun setSelectedEntry(entry: LearnsetMoveEntry?) {
            selectedEntry = entry
        }

        override fun getScrollbarPosition(): Int {
            return left + width - 3
        }

        override fun renderScrollbar(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
            val xLeft = this.scrollbarPosition
            val xRight = xLeft + 3
            val yStart = y + 1

            val barHeight = this.bottom - yStart

            var yBottom = ((barHeight * barHeight).toFloat() / this.maxPosition.toFloat()).toInt()
            yBottom = Mth.clamp(yBottom, 32, barHeight - 8)
            var yTop = scrollAmount.toInt() * (barHeight - yBottom) / this.maxScroll + yStart
            if (yTop < yStart) yTop = yStart

            context.fill(xLeft + 1, yStart, xRight - 1, this.bottom, FastColor.ARGB32.color(255, 126, 231, 229)) // track
            context.fill(xLeft, yTop, xRight, yTop + yBottom, FastColor.ARGB32.color(255, 58, 150, 182)) // bar
        }

        override fun getEntry(index: Int): MoveEntrySlot {
            return children()[index] as MoveEntrySlot
        }

        inner class MoveEntrySlot(
            val entry: LearnsetMoveEntry,
            private val parentList: LearnsetMovesScrollingWidget
        ) : Slot<MoveEntrySlot>() {
            private var lastX = 0
            private var lastY = 0

            override fun render(
                context: GuiGraphics,
                index: Int,
                y: Int,
                x: Int,
                entryWidth: Int,
                entryHeight: Int,
                mouseX: Int,
                mouseY: Int,
                hovered: Boolean,
                tickDelta: Float
            ) {
                lastX = x
                lastY = y

                val isSelected = entry == parentList.selectedEntry
                if (hovered || isSelected) {
                    val color = if (isSelected) {
                        FastColor.ARGB32.color(140, 58, 150, 182)
                    } else {
                        FastColor.ARGB32.color(90, 126, 231, 229)
                    }
                    context.fill(x, y, x + entryWidth - 3, y + entryHeight, color)
                }

                TypeIcon(
                    x = x + 1,
                    y = y + 1,
                    type = entry.move.elementalType,
                    small = true
                ).render(context)

                drawScaledText(
                    context = context,
                    text = entry.move.displayName,
                    x = x + 12,
                    y = y + 2,
                    scale = SCALE,
                    shadow = false,
                    colour = 0x606B6E
                )

                val rightLabel = when {
                    entry.level != null -> "Lv.${entry.level}"
                    entry.source == LearnsetSource.TM -> entry.source.label
                    else -> entry.source.label
                }

                drawScaledTextJustifiedRight(
                    context = context,
                    text = rightLabel.text(),
                    x = x + entryWidth - 6,
                    y = y + 2,
                    scale = SCALE,
                    shadow = false,
                    colour = if (entry.tmLocked) 0x8A8F91 else 0x606B6E
                )

                if (entry.source == LearnsetSource.TM && entry.tmLocked) {
                    blitk(
                        matrixStack = context.pose(),
                        texture = tmLockedIcon,
                        x = (x + entryWidth - 12) / SCALE,
                        y = (y + 2) / SCALE,
                        width = 8,
                        height = 8,
                        scale = SCALE
                    )
                }
            }

            override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
                val canClick = mouseX < (lastX + (parentList.width - 3))
                if (canClick) {
                    onSelect(entry)
                    Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(CobblemonSounds.POKEDEX_CLICK_SHORT, 1.0F))
                }
                return true
            }

            override fun getNarration(): Component {
                return entry.move.displayName
            }
        }
    }

    private class MoveDescriptionWidget(
        val pX: Int,
        val pY: Int,
        width: Int,
        height: Int
    ) : ScrollingWidget<MoveDescriptionWidget.TextSlot>(
        left = pX,
        top = pY,
        width = width,
        height = height,
        slotHeight = LIST_SLOT_HEIGHT
    ) {
        var showPlaceholder = true

        override fun renderWidget(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
            if (showPlaceholder) {
                drawScaledText(
                    context = context,
                    text = Component.literal("Select a move."),
                    x = pX + (width / 2),
                    y = pY + (height / 2) - 3,
                    shadow = false,
                    colour = 0x606B6E,
                    scale = SCALE,
                    centered = true
                )
                return
            }
            super.renderWidget(context, mouseX, mouseY, delta)
        }

        fun setText(text: Collection<String>) {
            clearEntries()
            text.forEach { line ->
                Minecraft.getInstance().font.splitter.splitLines(
                    Component.literal(line),
                    ((width - SCROLL_BAR_WIDTH - (POKEMON_DESCRIPTION_PADDING * 2)) / SCALE).toInt(),
                    Style.EMPTY
                ).stream()
                    .map { it.string }
                    .forEach { addEntry(TextSlot(it)) }
            }
            scrollAmount = 0.0
        }

        override fun renderScrollbar(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
            val xLeft = this.scrollbarPosition
            val xRight = xLeft + 3
            val yStart = y + 1

            val barHeight = this.bottom - yStart

            var yBottom = ((barHeight * barHeight).toFloat() / this.maxPosition.toFloat()).toInt()
            yBottom = Mth.clamp(yBottom, 16, barHeight - 6)
            var yTop = scrollAmount.toInt() * (barHeight - yBottom) / this.maxScroll + yStart
            if (yTop < yStart) yTop = yStart

            context.fill(xLeft + 1, yStart, xRight - 1, this.bottom, FastColor.ARGB32.color(255, 126, 231, 229)) // track
            context.fill(xLeft, yTop, xRight, yTop + yBottom, FastColor.ARGB32.color(255, 58, 150, 182)) // bar
        }

        override fun getScrollbarPosition(): Int {
            return left + width - 3
        }

        class TextSlot(val text: String) : Slot<TextSlot>() {
            override fun render(
                context: GuiGraphics,
                index: Int,
                y: Int,
                x: Int,
                entryWidth: Int,
                entryHeight: Int,
                mouseX: Int,
                mouseY: Int,
                hovered: Boolean,
                tickDelta: Float
            ) {
                drawScaledText(
                    context = context,
                    text = text.text(),
                    x = x + POKEMON_DESCRIPTION_PADDING,
                    y = y + 2,
                    scale = SCALE,
                    shadow = false,
                    colour = 0x606B6E
                )
            }

            override fun getNarration(): Component {
                return Component.literal(text)
            }
        }
    }
}
