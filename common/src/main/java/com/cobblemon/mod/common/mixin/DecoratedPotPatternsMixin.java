/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.sherds.CobblemonSherds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

/**
 * port/26.2: getPatternFromItem is gone. Vanilla now feeds every item -> pattern pair through
 * itemToPatternMappings, and DecoratedPotRenderer builds its sprite map from that once, so the Cobblemon
 * sherds are appended to the same stream.
 */
@Mixin(DecoratedPotPatterns.class)
public abstract class DecoratedPotPatternsMixin {
    @Inject(method = "itemToPatternMappings", at = @At("TAIL"))
    private static void cobblemon$addCobblemonSherdPatterns(
        BiConsumer<ResourceKey<Item>, ResourceKey<DecoratedPotPattern>> output,
        CallbackInfo ci
    ) {
        CobblemonSherds.INSTANCE.getSherdToPattern().forEach((sherd, pattern) ->
            output.accept(BuiltInRegistries.ITEM.getResourceKey(sherd).orElseThrow(), pattern)
        );
    }
}
