/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.world.gamerules

import net.minecraft.world.level.gamerules.GameRule
import net.minecraft.world.level.gamerules.GameRules

object CobblemonGameRules {
    private val STUB_TRUE: GameRule<Boolean> = GameRules.MOB_GRIEFING
    private val STUB_FALSE: GameRule<Boolean> = GameRules.MOB_GRIEFING

    @JvmField val BATTLE_INVULNERABILITY: GameRule<Boolean> = STUB_FALSE
    @JvmField val DO_POKEMON_SPAWNING: GameRule<Boolean> = STUB_TRUE
    @JvmField val DO_POKEMON_LOOT: GameRule<Boolean> = STUB_TRUE
    @JvmField val MOB_TARGET_IN_BATTLE: GameRule<Boolean> = STUB_TRUE
    @JvmField val SHINY_STARTERS: GameRule<Boolean> = STUB_FALSE
    @JvmField val HEALERS_HEAL_PC: GameRule<Boolean> = STUB_FALSE
}
