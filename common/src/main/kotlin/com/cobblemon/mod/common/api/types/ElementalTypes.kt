/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.types

import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

/**
 * Registry for all known ElementalTypes
 */
object ElementalTypes {

    private val allTypes = mutableListOf<ElementalType>()

    val NORMAL = register(
        name = "normal",
        displayName = Component.translatable("cobblemon.type.normal"),
        hue = 0xDDDDCF,
        textureXMultiplier = 0,
        primaryColor = 0xCCCCC1,
        secondaryColor = 0xF9FFFC
    )

    val FIRE = register(
        name = "fire",
        displayName = Component.translatable("cobblemon.type.fire"),
        hue = 0xE55C32,
        textureXMultiplier = 1,
        primaryColor = 0xFF8766,
        secondaryColor = 0xFFD9A5
    )

    val WATER = register(
        name = "water",
        displayName = Component.translatable("cobblemon.type.water"),
        hue = 0x4A9BE8,
        textureXMultiplier = 2,
        primaryColor = 0x7F9DFF,
        secondaryColor = 0xA5EEFF
    )

    val GRASS = register(
        name = "grass",
        displayName = Component.translatable("cobblemon.type.grass"),
        hue = 0x4DBC3C,
        textureXMultiplier = 3,
        primaryColor = 0x66CC99,
        secondaryColor = 0xD6FFA5
    )

    val ELECTRIC = register(
        name = "electric",
        displayName = Component.translatable("cobblemon.type.electric"),
        hue = 0xEFD128,
        textureXMultiplier = 4,
        primaryColor = 0xFFCF3F,
        secondaryColor = 0xFFFFBF
    )

    val ICE = register(
        name = "ice",
        displayName = Component.translatable("cobblemon.type.ice"),
        hue = 0x6BC3EF,
        textureXMultiplier = 5,
        primaryColor = 0x8CC7FF,
        secondaryColor = 0xD6FFFF
    )

    val FIGHTING = register(
        name = "fighting",
        displayName = Component.translatable("cobblemon.type.fighting"),
        hue = 0xC44C5C,
        textureXMultiplier = 6,
        primaryColor = 0xCC6673,
        secondaryColor = 0xFFADA5
    )

    val POISON = register(
        name = "poison",
        displayName = Component.translatable("cobblemon.type.poison"),
        hue = 0xA24BD8,
        textureXMultiplier = 7,
        primaryColor = 0x8D66CC,
        secondaryColor = 0xEDA5FF
    )

    val GROUND = register(
        name = "ground",
        displayName = Component.translatable("cobblemon.type.ground"),
        hue = 0xD89950,
        textureXMultiplier = 8,
        primaryColor = 0xE5A27E,
        secondaryColor = 0xFFE1A5
    )

    val FLYING = register(
        name = "flying",
        displayName = Component.translatable("cobblemon.type.flying"),
        hue = 0xBCC1FF,
        textureXMultiplier = 9,
        primaryColor = 0x8CB6FF,
        secondaryColor = 0xE8DDFF
    )

    val PSYCHIC = register(
        name = "psychic",
        displayName = Component.translatable("cobblemon.type.psychic"),
        hue = 0xD86AD6,
        textureXMultiplier = 10,
        primaryColor = 0xE48CFF,
        secondaryColor = 0xFFCCF6
    )

    val BUG = register(
        name = "bug",
        displayName = Component.translatable("cobblemon.type.bug"),
        hue = 0xA2C831,
        textureXMultiplier = 11,
        primaryColor = 0xAFCC66,
        secondaryColor = 0xF6FFBF
    )

    val ROCK = register(
        name = "rock",
        displayName = Component.translatable("cobblemon.type.rock"),
        hue = 0xAA9666,
        textureXMultiplier = 12,
        primaryColor = 0x997F4C,
        secondaryColor = 0xDBC6A4

    )

    val GHOST = register(
        name = "ghost",
        displayName = Component.translatable("cobblemon.type.ghost"),
        hue = 0x9572E5,
        textureXMultiplier = 13,
        primaryColor = 0x8E8CFF,
        secondaryColor = 0xE8A5FF
    )

    val DRAGON = register(
        name = "dragon",
        displayName = Component.translatable("cobblemon.type.dragon"),
        hue = 0x535DE8,
        textureXMultiplier = 14,
        primaryColor = 0xBA8CFF,
        secondaryColor = 0xA5E5FF
    )

    val DARK = register(
        name = "dark",
        displayName = Component.translatable("cobblemon.type.dark"),
        hue = 0x5C6CB2,
        textureXMultiplier = 15,
        primaryColor = 0x5966B2,
        secondaryColor = 0xA5B1FF
    )

    val STEEL = register(
        name = "steel",
        displayName = Component.translatable("cobblemon.type.steel"),
        hue = 0xC3CCE0,
        textureXMultiplier = 16,
        primaryColor = 0x858EB2,
        secondaryColor = 0xF4FFFD
    )

    val FAIRY = register(
        name = "fairy",
        displayName = Component.translatable("cobblemon.type.fairy"),
        hue = 0xEA727E,
        textureXMultiplier = 17,
        primaryColor = 0xFF8CB6,
        secondaryColor = 0xFFD0CC
    )

    fun register(name: String, displayName: MutableComponent, hue: Int, textureXMultiplier: Int, primaryColor: Int, secondaryColor: Int): ElementalType {
        return ElementalType(
            name = name,
            displayName = displayName,
            hue = hue,
            textureXMultiplier = textureXMultiplier,
            primaryColor =  primaryColor,
            secondaryColor = secondaryColor,
            typeGem = cobblemonResource("${name}_gem")
        ).also {
            allTypes.add(it)
        }
    }

    fun register(elementalType: ElementalType): ElementalType {
        allTypes.add(elementalType)
        return elementalType
    }

    fun get(name: String): ElementalType? {
        return allTypes.firstOrNull { type -> type.name.equals(name, ignoreCase = true) }
    }

    fun getOrException(name: String): ElementalType {
        return allTypes.first { type -> type.name.equals(name, ignoreCase = true) }
    }

    fun count() = allTypes.size

    fun all() = this.allTypes.toList()
}
