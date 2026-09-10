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
 * outside the vanilla package needs an invoker. Vanilla passes 30 ticks for wooden buttons and 20 for
 * stone.
 */
@SuppressWarnings("unused")
@Mixin(ButtonBlock.class)
public interface ButtonBlockInvoker {

    /** How long a wooden button stays pressed, matching vanilla's wooden button blocks. */
    int WOODEN_TICKS_TO_STAY_PRESSED = 30;

    @Invoker("<init>")
    static ButtonBlock create(BlockSetType type, int ticksToStayPressed, BlockBehaviour.Properties properties) {
        throw new UnsupportedOperationException();
    }

}
