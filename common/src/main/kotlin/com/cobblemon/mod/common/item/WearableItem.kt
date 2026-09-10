/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.item

import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EquipmentSlot

// PT143: Equipable interface removed in MC 26.1.x → replaced by Equippable data component on item registration.
// Slot info retained as helper for downstream registration code.
class WearableItem(val name: String): CobblemonItem(Properties()) {
    companion object {
        const val MODEL_PATH = "item/wearable"
    }

    fun getModel3d(): Identifier = cobblemonResource("${MODEL_PATH}/${this.name}")
    fun getModel2d(): Identifier = cobblemonResource(this.name)

    fun getEquipmentSlot(): EquipmentSlot = EquipmentSlot.HEAD
}