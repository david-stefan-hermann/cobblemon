/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.types

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

/**
 * Registry for all known ElementalTypes
 */
object ElementalTypes {

    private val allTypes = mutableListOf<ElementalType>()

    val NORMAL = register(
        name = "Normal",
        displayName = Component.translatable("cobblemon.type.normal"),
        hue = 0xE8E8DA,
        textureXMultiplier = 0,
        primaryColor = 0x8FA689,
        secondaryColor = 0xFFFBE2
    )

    val FIRE = register(
        name = "Fire",
        displayName = Component.translatable("cobblemon.type.fire"),
        hue = 0xFF6E21,
        textureXMultiplier = 1,
        primaryColor = 0xF92D2D,
        secondaryColor = 0xF99B1D
    )

    val WATER = register(
        name = "Water",
        displayName = Component.translatable("cobblemon.type.water"),
        hue = 0x3FA5FF,
        textureXMultiplier = 2,
        primaryColor = 0x377AF6,
        secondaryColor = 0x50DDF9
    )

    val GRASS = register(
        name = "Grass",
        displayName = Component.translatable("cobblemon.type.grass"),
        hue = 0x62D14F,
        textureXMultiplier = 3,
        primaryColor = 0x0EAE95,
        secondaryColor = 0xC6F554
    )

    val ELECTRIC = register(
        name = "Electric",
        displayName = Component.translatable("cobblemon.type.electric"),
        hue = 0xFFD314,
        textureXMultiplier = 4,
        primaryColor = 0xF7AB19,
        secondaryColor = 0xFFF257
    )

    val ICE = register(
        name = "Ice",
        displayName = Component.translatable("cobblemon.type.ice"),
        hue = 0x54F2F2,
        textureXMultiplier = 5,
        primaryColor = 0x41BAED,
        secondaryColor = 0x91F4F9
    )

    val FIGHTING = register(
        name = "Fighting",
        displayName = Component.translatable("cobblemon.type.fighting"),
        hue = 0xEF565D,
        textureXMultiplier = 6,
        primaryColor = 0xC8356B,
        secondaryColor = 0xF36A3F
    )

    val POISON = register(
        name = "Poison",
        displayName = Component.translatable("cobblemon.type.poison"),
        hue = 0xD651FF,
        textureXMultiplier = 7,
        primaryColor = 0x7B41DD,
        secondaryColor = 0xE568F4
    )

    val GROUND = register(
        name = "Ground",
        displayName = Component.translatable("cobblemon.type.ground"),
        hue = 0xF4A453,
        textureXMultiplier = 8,
        primaryColor = 0xE08835,
        secondaryColor = 0xFFD057
    )

    val FLYING = register(
        name = "Flying",
        displayName = Component.translatable("cobblemon.type.flying"),
        hue = 0xB8B2FF,
        textureXMultiplier = 9,
        primaryColor = 0x6083F6,
        secondaryColor = 0xD5CCFA
    )

    val PSYCHIC = register(
        name = "Psychic",
        displayName = Component.translatable("cobblemon.type.psychic"),
        hue = 0xFF5E9E,
        textureXMultiplier = 10,
        primaryColor = 0xE553CE,
        secondaryColor = 0xF69DD5
    )

    val BUG = register(
        name = "Bug",
        displayName = Component.translatable("cobblemon.type.bug"),
        hue = 0xD3D319,
        textureXMultiplier = 11,
        primaryColor = 0x9FBE1B,
        secondaryColor = 0xE9FB5B
    )

    val ROCK = register(
        name = "Rock",
        displayName = Component.translatable("cobblemon.type.rock"),
        hue = 0xB7A16E,
        textureXMultiplier = 12,
        primaryColor = 0x9F814A,
        secondaryColor = 0xEECB7A
    )

    val GHOST = register(
        name = "Ghost",
        displayName = Component.translatable("cobblemon.type.ghost"),
        hue = 0x9C80F7,
        textureXMultiplier = 13,
        primaryColor = 0x4F57DD,
        secondaryColor = 0xC182F1
    )

    val DRAGON = register(
        name = "Dragon",
        displayName = Component.translatable("cobblemon.type.dragon"),
        hue = 0x7580FF,
        textureXMultiplier = 14,
        primaryColor = 0x4E68E2,
        secondaryColor = 0x47C7F7
    )

    val DARK = register(
        name = "Dark",
        displayName = Component.translatable("cobblemon.type.dark"),
        hue = 0x587DA0,
        textureXMultiplier = 15,
        primaryColor = 0x465B99,
        secondaryColor = 0x689EBF
    )

    val STEEL = register(
        name = "Steel",
        displayName = Component.translatable("cobblemon.type.steel"),
        hue = 0xABD1F4,
        textureXMultiplier = 16,
        primaryColor = 0x628CB9,
        secondaryColor = 0xD6F7F3
    )

    val FAIRY = register(
        name = "Fairy",
        displayName = Component.translatable("cobblemon.type.fairy"),
        hue = 0xFF7FE5,
        textureXMultiplier = 17,
        primaryColor = 0xE2678C,
        secondaryColor = 0xF5A7B7
    )

    fun register(name: String, displayName: MutableComponent, hue: Int, textureXMultiplier: Int, primaryColor: Int, secondaryColor: Int): ElementalType {
        return ElementalType(
            name = name,
            displayName = displayName,
            hue = hue,
            textureXMultiplier = textureXMultiplier,
            primaryColor =  primaryColor,
            secondaryColor = secondaryColor
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
