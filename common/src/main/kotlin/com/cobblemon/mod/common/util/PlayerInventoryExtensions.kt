/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util

import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import java.util.function.Predicate
import java.util.stream.Collectors

fun Inventory.removeAmountIf(amount: Int, rule: Predicate<ItemStack>) {
    val items = this.nonEquipmentItems
    var index = 0
    val matches = items.stream()
        .map { a -> Pair(index++, a) }
        .filter { rule.test(it.second) }
        .collect(Collectors.toList())

    var remaining = amount
    while (remaining > 0) {
        val element = matches.removeFirstOrNull() ?: return

        val result = this.removeItem(element.first, amount)
        remaining -= result.count
    }
}