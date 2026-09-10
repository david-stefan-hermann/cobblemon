/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.invoker;

import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * port/26.2: Blocks.woodenButton() is gone and ButtonBlock's constructor is protected, so building one
 * outside the vanilla package needs an invoker.
 *
 * Note that an interface mixin may only declare @Shadow members - a plain constant here is rejected at
 * apply time - so the tick count lives with the caller.
 */
@SuppressWarnings("unused")
@Mixin(ButtonBlock.class)
public interface ButtonBlockInvoker {

    @Invoker("<init>")
    static ButtonBlock create(BlockSetType type, int ticksToStayPressed, BlockBehaviour.Properties properties) {
        throw new UnsupportedOperationException();
    }

}
