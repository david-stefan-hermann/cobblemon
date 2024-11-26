package com.cobblemon.mod.common.gui

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.mixin.invoker.MenuTypeInvoker
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack

object CobblemonScreenHandlers {
    val TMM_SCREEN = register(cobblemonResource("tmm_screen"), ::TMMScreenHandler)

    fun <T : AbstractContainerMenu> register(identifier: ResourceKey<MenuType<T>>, factory: MenuType.MenuSupplier<T>): MenuType<T> {
        val result = MenuTypeInvoker.create(factory, BuiltInRegistries.FEATURES)
        Cobblemon.implementation.registerScreenHandlerType(identifier.location(), result)
        return result
    }
}
