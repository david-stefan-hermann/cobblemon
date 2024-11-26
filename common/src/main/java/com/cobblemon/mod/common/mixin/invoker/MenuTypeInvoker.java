package com.cobblemon.mod.common.mixin.invoker;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType.MenuSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MenuType.class)
public interface MenuTypeInvoker {
    @Invoker("<init>")
    static <T extends AbstractContainerMenu> MenuType<T> create(MenuSupplier<T> factory, FeatureFlagSet requiredFeatures) {
        throw new UnsupportedOperationException();
    }
}
