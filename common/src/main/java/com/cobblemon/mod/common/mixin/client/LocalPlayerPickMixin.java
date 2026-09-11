/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

/**
 * port/26.2: entity picking moved from GameRenderer to the static LocalPlayer.pick with the same
 * descriptor; this is the former GameRendererMixin#filterPokemonVehicle, unchanged apart from being static.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerPickMixin {
    @ModifyArg(
        method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"
        ),
        index = 4
    )
    private static Predicate<Entity> cobblemon$filterPokemonVehicle(Predicate<Entity> original, @Local(ordinal = 0, argsOnly = true) Entity entity) {
        return (entityX) -> !(entity.getVehicle() == entityX && entityX instanceof PokemonEntity) && original.test(entityX);
    }
}
