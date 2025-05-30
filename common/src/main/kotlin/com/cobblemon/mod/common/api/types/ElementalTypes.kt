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
        primaryColor = 0x88AA83,
        secondaryColor = 0xFFFFD8
    )

    val FIRE = register(
        name = "fire",
        displayName = Component.translatable("cobblemon.type.fire"),
        hue = 0xE55C32,
        textureXMultiplier = 1,
        primaryColor = 0xFF0202,
        secondaryColor = 0xFFAA00
    )

    val WATER = register(
        name = "water",
        displayName = Component.translatable("cobblemon.type.water"),
        hue = 0x4A9BE8,
        textureXMultiplier = 2,
        primaryColor = 0x1447FF,
        secondaryColor = 0x1EFBFF
    )

    val GRASS = register(
        name = "grass",
        displayName = Component.translatable("cobblemon.type.grass"),
        hue = 0x4DBC3C,
        textureXMultiplier = 3,
        primaryColor = 0x03B59D,
        secondaryColor = 0xD1FF05
    )

    val ELECTRIC = register(
        name = "electric",
        displayName = Component.translatable("cobblemon.type.electric"),
        hue = 0xEFD128,
        textureXMultiplier = 4,
        primaryColor = 0xFF8800,
        secondaryColor = 0xFFFF4F
    )

    val ICE = register(
        name = "ice",
        displayName = Component.translatable("cobblemon.type.ice"),
        hue = 0x6BC3EF,
        textureXMultiplier = 5,
        primaryColor = 0x009DFF,
        secondaryColor = 0xA3FFF1
    )

    val FIGHTING = register(
        name = "fighting",
        displayName = Component.translatable("cobblemon.type.fighting"),
        hue = 0xC44C5C,
        textureXMultiplier = 6,
        primaryColor = 0xCC005B,
        secondaryColor = 0xFF831E
    )

    val POISON = register(
        name = "poison",
        displayName = Component.translatable("cobblemon.type.poison"),
        hue = 0xA24BD8,
        textureXMultiplier = 7,
        primaryColor = 0x6E02EA,
        secondaryColor = 0xFF49DA
    )

    val GROUND = register(
        name = "ground",
        displayName = Component.translatable("cobblemon.type.ground"),
        hue = 0xD89950,
        textureXMultiplier = 8,
        primaryColor = 0xE02E0B,
        secondaryColor = 0xFFD632
    )

    val FLYING = register(
        name = "flying",
        displayName = Component.translatable("cobblemon.type.flying"),
        hue = 0xBCC1FF,
        textureXMultiplier = 9,
        primaryColor = 0x3059FF,
        secondaryColor = 0xD0C1FF
    )

    val PSYCHIC = register(
        name = "psychic",
        displayName = Component.translatable("cobblemon.type.psychic"),
        hue = 0xD86AD6,
        textureXMultiplier = 10,
        primaryColor = 0xFF11A0,
        secondaryColor = 0xFF9666
    )

    val BUG = register(
        name = "bug",
        displayName = Component.translatable("cobblemon.type.bug"),
        hue = 0xA2C831,
        textureXMultiplier = 11,
        primaryColor = 0xA0BC00,
        secondaryColor = 0xF4FF32
    )

    val ROCK = register(
        name = "rock",
        displayName = Component.translatable("cobblemon.type.rock"),
        hue = 0xAA9666,
        textureXMultiplier = 12,
        primaryColor = 0xA87932,
        secondaryColor = 0xFFD477
    )

    val GHOST = register(
        name = "ghost",
        displayName = Component.translatable("cobblemon.type.ghost"),
        hue = 0x9572E5,
        textureXMultiplier = 13,
        primaryColor = 0x3221EF,
        secondaryColor = 0xCA68FF
    )

    val DRAGON = register(
        name = "dragon",
        displayName = Component.translatable("cobblemon.type.dragon"),
        hue = 0x535DE8,
        textureXMultiplier = 14,
        primaryColor = 0x610ECE,
        secondaryColor = 0x32D6FF
    )

    val DARK = register(
        name = "dark",
        displayName = Component.translatable("cobblemon.type.dark"),
        hue = 0x5C6CB2,
        textureXMultiplier = 15,
        primaryColor = 0x153166,
        secondaryColor = 0x52A5C4
    )

    val STEEL = register(
        name = "steel",
        displayName = Component.translatable("cobblemon.type.steel"),
        hue = 0xC3CCE0,
        textureXMultiplier = 16,
        primaryColor = 0x2B77E2,
        secondaryColor = 0xCCFFFA
    )

    val FAIRY = register(
        name = "fairy",
        displayName = Component.translatable("cobblemon.type.fairy"),
        hue = 0xEA727E,
        textureXMultiplier = 17,
        primaryColor = 0xDB28FF,
        secondaryColor = 0xFF9F8C
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
