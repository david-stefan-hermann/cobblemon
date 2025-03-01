/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.gui

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.platform.PlatformRegistry
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.SimpleContainer
import net.minecraft.world.inventory.MenuType

import net.minecraft.world.flag.FeatureFlags

object CobblemonMenuHandlers : PlatformRegistry<Registry<MenuType<*>>, ResourceKey<Registry<MenuType<*>>>, MenuType<*>>() {

    val TMM_SCREEN: MenuType<TMMScreenHandler> = create(
        "tmm_screen",
        MenuType({ syncId, inventory -> TMMScreenHandler(syncId, inventory, SimpleContainer(4)) }, FeatureFlags.VANILLA_SET)
    )

    fun register() {
        Registry.register(BuiltInRegistries.MENU, cobblemonResource("tmm_screen"), TMM_SCREEN)
    }

    override val registry = BuiltInRegistries.MENU
    override val resourceKey = Registries.MENU
}
