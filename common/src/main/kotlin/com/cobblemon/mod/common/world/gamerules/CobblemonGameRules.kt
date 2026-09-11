/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.world.gamerules

import com.cobblemon.mod.common.Cobblemon
import net.minecraft.world.level.gamerules.GameRule
import net.minecraft.world.level.gamerules.GameRuleCategory

/**
 * port/26.2: the reference port pointed every one of these at vanilla's mobGriefing, so none of them was
 * registered and all six silently followed that rule. They are registered for real again; 26.2 game rules
 * are identifiers, so the old camelCase names became snake_case (gamerule.cobblemon.<id> in the lang files).
 */
object CobblemonGameRules {
    @JvmField
    val BATTLE_INVULNERABILITY: GameRule<Boolean> = Cobblemon.implementation.registerGameRule("battle_invulnerability", GameRuleCategory.PLAYER, false)
    @JvmField
    val DO_POKEMON_SPAWNING: GameRule<Boolean> = Cobblemon.implementation.registerGameRule("do_pokemon_spawning", GameRuleCategory.SPAWNING, true)
    @JvmField
    val DO_POKEMON_LOOT: GameRule<Boolean> = Cobblemon.implementation.registerGameRule("do_pokemon_loot", GameRuleCategory.DROPS, true)
    @JvmField
    val MOB_TARGET_IN_BATTLE: GameRule<Boolean> = Cobblemon.implementation.registerGameRule("mob_target_in_battle", GameRuleCategory.MOBS, true)
    @JvmField
    val SHINY_STARTERS: GameRule<Boolean> = Cobblemon.implementation.registerGameRule("do_shiny_starters", GameRuleCategory.MISC, false)
    @JvmField
    val HEALERS_HEAL_PC: GameRule<Boolean> = Cobblemon.implementation.registerGameRule("healers_heal_pc", GameRuleCategory.MISC, false)
}
