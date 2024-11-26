/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.gui.tm

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.gui.drawProfilePokemon
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.math.fromEulerXYZDegrees
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipComponent
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipComponent.ItemTooltip
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipComponent.TooltipEntry
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipPositioner
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.network.chat.Component
import org.joml.Quaternionf
import org.joml.Vector3f

class TMPartySlotWidget(
        pX: Number,
        pY: Number,
        val pokemon: Pokemon?,
        onPress: OnPress
) : Button(pX.toInt(), pY.toInt(), WIDTH, HEIGHT, Component.literal("PartyMember"), onPress, DEFAULT_NARRATION_SUPPLIER) {

    val state = FloatingState()

    companion object {
        const val WIDTH = 46
        const val HEIGHT = 27
        private const val PORTRAIT_DIAMETER = 25

        private val slotResource = cobblemonResource("textures/gui/tm/party_slot.png")
        val genderIconMale = cobblemonResource("textures/gui/party/party_gender_male.png")
        val genderIconFemale = cobblemonResource("textures/gui/party/party_gender_female.png")
    }

    private fun getSlotVOffset(pokemon: Pokemon?, isHovered: Boolean, isSelected: Boolean): Int {
        return when {
            isHovered || isSelected -> if (pokemon == null) 0 else height
            else -> 0
        }
    }

    override fun renderWidget(context: net.minecraft.client.gui.GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        isHovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height
        val matrices = context.pose()

        blitk(
                matrixStack = matrices,
                texture = slotResource,
                x = x,
                y = y,
                width = width,
                height = height,
                vOffset = getSlotVOffset(pokemon, isHovered, isSelected),
                textureHeight = height * 2,
        )

        if (pokemon != null) {
            val halfScale = 0.5F

            // Render Pokémon
            matrices.pushPose()
            matrices.translate(x + (PORTRAIT_DIAMETER / 2.0) + 10, y - 1.0, 0.0)
            matrices.scale(2.5F, 2.5F, 1F)
            drawProfilePokemon(
                    species = pokemon.species.resourceIdentifier,
                    aspects = pokemon.aspects.toSet(),
                    matrixStack = matrices,
                    rotation = Quaternionf().fromEulerXYZDegrees(Vector3f(13F, 35F, 0F)),
                    state = state,
                    scale = 4.5F,
                    partialTicks = delta
            )
            matrices.popPose()

            // Draw Name
            drawScaledText(
                    context = context,
                    text = pokemon.getDisplayName(),
                    x = x + 16,
                    y = y + 21.5,
                    scale = halfScale
            )

            val ballIcon = cobblemonResource("textures/gui/ball/" + pokemon.caughtBall.name.path + ".png")
            val ballHeight = 22
            blitk(
                    matrixStack = matrices,
                    texture = ballIcon,
                    x = (x - 2) / halfScale,
                    y = (y - 3) / halfScale,
                    height = ballHeight,
                    width = 18,
                    textureHeight = ballHeight * 2,
                    scale = halfScale
            )
        }
    }
}
