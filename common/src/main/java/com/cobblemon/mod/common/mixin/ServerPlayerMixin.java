/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.OrientationControllable;
import com.cobblemon.mod.common.PlayerSpawnerAccessor;
import com.cobblemon.mod.common.api.riding.RidingStyle;
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawner;
import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawnerFactory;
import com.cobblemon.mod.common.api.stats.CobblemonStats;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.CompoundTagExtensionsKt;
import com.cobblemon.mod.common.util.CompoundTagUtilities;
import com.cobblemon.mod.common.world.gamerules.CobblemonGameRules;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * port/26.2: serverLevel() is gone (level() is typed ServerLevel), stopRiding's ServerPlayer override is
 * removeVehicle, and the vehicle is saved in saveParentVehicle instead of addAdditionalSaveData.
 *
 * The shoulder handling that used to live in PlayerMixin is here now too: 26.2 keeps the shoulder NBT and
 * respawnEntityOnShoulder/removeEntitiesOnShoulder only on ServerPlayer.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements PlayerSpawnerAccessor {
    @Shadow
    public abstract ServerLevel level();

    @Shadow public abstract CompoundTag getShoulderEntityLeft();
    @Shadow public abstract CompoundTag getShoulderEntityRight();
    // widened to public by cobblemon-common.accesswidener
    @Shadow public abstract void setShoulderEntityLeft(CompoundTag tag);
    @Shadow public abstract void setShoulderEntityRight(CompoundTag tag);
    @Shadow public abstract void respawnEntityOnShoulder(CompoundTag tag);

    @Unique
    public PlayerSpawner cobblemon$spawner;

    @Override
    public PlayerSpawner getPlayerSpawner() {
        ServerPlayer player = (ServerPlayer)(Object)this;
        if (cobblemon$spawner == null) {
            cobblemon$spawner = PlayerSpawnerFactory.INSTANCE.create(player);
        }
        return cobblemon$spawner;
    }

    @Override
    public void setPlayerSpawner(PlayerSpawner spawner) {
        this.cobblemon$spawner = spawner;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void cobblemon$tickSpawner(CallbackInfo ci) {
        if (!Cobblemon.config.getEnableSpawning()) {
            return;
        } else if (!level().getGameRules().get(CobblemonGameRules.DO_POKEMON_SPAWNING)) {
            return;
        }
        getPlayerSpawner().tick();
    }

    @Inject(method = "rideTick", at = @At("HEAD"))
    private void cobblemon$updateOrientationControllerRideTick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer)(Object)this;
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof OrientationControllable controllableVehicle)) return;
        var shouldUseCustomOrientation = cobblemon$shouldUseCustomOrientation(vehicle);
        controllableVehicle.getOrientationController().setActive(shouldUseCustomOrientation);
    }

    @Unique
    private boolean cobblemon$shouldUseCustomOrientation(Entity entity) {
        if (entity == null) return false;
        if (!(entity instanceof PokemonEntity pokemonEntity)) return false;
        return pokemonEntity.ifRidingAvailableSupply(false, (behaviour, settings, state) -> {
            return behaviour.shouldRoll(settings, state, pokemonEntity);
        });
    }

    @Inject(method = "removeVehicle", at = @At("HEAD"))
    public void cobblemon$resetOrientationOnDismount(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer)(Object)this;
        if (!(player.getVehicle() instanceof OrientationControllable controllableVehicle)) return;
        controllableVehicle.getOrientationController().setActive(false);
    }

    //TODO: Switch to sending out on load instead of preventing saving
    @ModifyExpressionValue(method = "saveParentVehicle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hasExactlyOnePlayerPassenger()Z"))
    private boolean cobblemon$cancelSavingPokemonMounts(boolean original, @Local(name = "rootVehicle") Entity entity) {
        return original && !(entity instanceof PokemonEntity);
    }

    @WrapOperation(method = "checkRidingStatistics", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getVehicle()Lnet/minecraft/world/entity/Entity;"))
    public Entity cobblemon$checkRidingStatistics(ServerPlayer player, Operation<Entity> original, @Local(name = "distance") int distance) {
        var entity = original.call(player);
        if (!(entity instanceof PokemonEntity pokemonEntity)) {
            return entity;
        }

        var ridingController = pokemonEntity.getRidingController();
        if (ridingController == null) return entity;

        var activeContext = ridingController.getContext();
        if (activeContext == null) return entity;

        var ridingStyle = activeContext.getStyle();
        CobblemonStats.CobblemonStat stat = switch (ridingStyle) {
            case RidingStyle.LAND -> CobblemonStats.RIDING_LAND;
            case RidingStyle.AIR -> CobblemonStats.RIDING_AIR;
            case RidingStyle.LIQUID -> CobblemonStats.RIDING_LIQUID;
            default -> null;
        };

        if (stat != null) {
            player.awardStat(CobblemonStats.getStat(stat), distance);
        }

        return entity;
    }

    // Formerly PlayerMixin#cobblemon$removePokemon, which injected at EntityType.create after the empty check;
    // an empty tag is never a Pokémon, so checking at HEAD is equivalent.
    @Inject(method = "respawnEntityOnShoulder", at = @At("HEAD"), cancellable = true)
    private void cobblemon$removePokemon(CompoundTag nbt, CallbackInfo ci) {
        if (CompoundTagExtensionsKt.isPokemonEntity(nbt)) {
            final UUID uuid = CompoundTagUtilities.getPokemonID(nbt);
            if (CompoundTagUtilities.isShoulderPokemon(this.getShoulderEntityRight())) {
                final UUID uuidRight = CompoundTagUtilities.getPokemonID(this.getShoulderEntityRight());
                if (uuid.equals(uuidRight)) {
                    this.cobblemon$recallPokemon(uuidRight);
                    this.setShoulderEntityRight(new CompoundTag());
                }
            }
            if (CompoundTagUtilities.isShoulderPokemon(this.getShoulderEntityLeft())) {
                final UUID uuidLeft = CompoundTagUtilities.getPokemonID(this.getShoulderEntityLeft());
                if (uuid.equals(uuidLeft)) {
                    this.cobblemon$recallPokemon(uuidLeft);
                    this.setShoulderEntityLeft(new CompoundTag());
                }
            }
            ci.cancel();
        }
    }

    // Formerly PlayerMixin#cobblemon$preventPokemonDropping: jumps past the "sat long enough" check.
    @Inject(
        method = "removeEntitiesOnShoulder",
        at = @At(
            value = "JUMP",
            opcode = Opcodes.IFGE,
            ordinal = 0,
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void cobblemon$preventPokemonDropping(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer)(Object)this;
        // We want to allow both of these to forcefully remove the entities
        if (player.isSpectator() || player.isDeadOrDying())
            return;
        if (!CompoundTagUtilities.isShoulderPokemon(this.getShoulderEntityLeft())) {
            this.respawnEntityOnShoulder(this.getShoulderEntityLeft());
            this.setShoulderEntityLeft(new CompoundTag());
        }
        if (!CompoundTagUtilities.isShoulderPokemon(this.getShoulderEntityRight())) {
            this.respawnEntityOnShoulder(this.getShoulderEntityRight());
            this.setShoulderEntityRight(new CompoundTag());
        }
        ci.cancel();
    }

    @Unique
    private void cobblemon$recallPokemon(UUID uuid) {
        // We need to do this cause the Entity doesn't store a reference to its storage
        ServerPlayer player = (ServerPlayer)(Object)this;
        final PartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player.getUUID(), player.registryAccess());
        for (Pokemon pokemon : party) {
            if (pokemon.getUuid().equals(uuid)) {
                pokemon.recall();
            }
        }
    }
}
