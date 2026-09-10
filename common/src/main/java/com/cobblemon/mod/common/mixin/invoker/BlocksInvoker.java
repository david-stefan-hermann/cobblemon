/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.invoker;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * port/26.2: these helpers used to build whole blocks - log(MapColor, MapColor), leaves(SoundType),
 * woodenButton(BlockSetType), flowerPot(Block). In 26.2 they only assemble the block's properties and
 * the caller constructs the block itself, so the invokers return Properties and log additionally takes
 * the sound type that used to be baked in.
 */
@SuppressWarnings("unused")
@Mixin(Blocks.class)
public interface BlocksInvoker {

    @Invoker("logProperties")
    static BlockBehaviour.Properties createLogProperties(MapColor topMapColor, MapColor sideMapColor, SoundType soundType) {
        throw new UnsupportedOperationException();
    }

    @Invoker("leavesProperties")
    static BlockBehaviour.Properties createLeavesProperties(SoundType soundType) {
        throw new UnsupportedOperationException();
    }

    @Invoker("buttonProperties")
    static BlockBehaviour.Properties createButtonProperties() {
        throw new UnsupportedOperationException();
    }

    @Invoker("flowerPotProperties")
    static BlockBehaviour.Properties createFlowerPotProperties() {
        throw new UnsupportedOperationException();
    }

}
