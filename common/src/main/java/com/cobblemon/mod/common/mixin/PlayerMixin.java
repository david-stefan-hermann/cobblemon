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
import com.cobblemon.mod.common.api.orientation.OrientationController;
import com.cobblemon.mod.common.api.riding.RidingStyle;
import com.cobblemon.mod.common.api.riding.behaviour.types.air.HoverBehaviour;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.duck.PlayerDuck;
import com.cobblemon.mod.common.duck.RidePassenger;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokedex.scanner.PokedexEntityData;
import com.cobblemon.mod.common.pokedex.scanner.ScannableEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.CompoundTagExtensionsKt;
import com.cobblemon.mod.common.util.CompoundTagUtilities;
import com.cobblemon.mod.common.util.DataKeys;
import com.cobblemon.mod.common.world.gamerules.CobblemonGameRules;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * port/26.2 changes against the 1.21.1 mixin:
 * - Player extends the new Avatar, which owns POSES/STANDING_DIMENSIONS and getDefaultDimensions.
 * - The shoulder slots and respawnEntityOnShoulder/removeEntitiesOnShoulder exist only on ServerPlayer; those
 *   injectors moved to ServerPlayerMixin. The PlayerDuck shoulder accessors delegate to ServerPlayer and are
 *   empty on the client, which no longer receives shoulder NBT (only parrot variants are synced) - so client
 *   Pokédex scans of shoulder Pokémon find nothing until Cobblemon syncs that data itself.
 * - Player.eat is gone (food runs through FoodProperties.onConsume) - see FoodPropertiesMixin.
 * - isInvulnerableTo takes the ServerLevel; absMoveTo/absRotateTo are absSnapTo/absSnapRotationTo.
 */
@Mixin(Player.class)
public abstract class PlayerMixin extends Avatar implements ScannableEntity, OrientationControllable, PlayerDuck, RidePassenger {

    @Unique
    private Vector3f cobblemon$driverInput;
    @Unique
    private Vector3f cobblemon$lastSentDriverInput;

    @Unique private final OrientationController cobblemon$orientationController = new OrientationController(this);

    @Unique private float cobblemon$rideXRot = 0.0f;

    @Unique private float cobblemon$rideYRot = 0.0f;

    @Unique private Vec3 cobblemon$rideEyePos = Vec3.ZERO;

    protected PlayerMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        if (this.getVehicle() instanceof PokemonEntity) {
            return STANDING_DIMENSIONS;
        }
        return super.getDefaultDimensions(pose);
    }

    @Redirect(
            method = "getDestroySpeed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;onGround()Z"
            )
    )
    private boolean cobblemon$considerHoverOnGround(Player player) {
        // If riding a Hover Mount, return true to bypass the 5x slowdown
        if (player.getVehicle() instanceof PokemonEntity vehicle) {

            // port/26.2: the 1.21.1 code fell through to getContext() on a null controller/context here
            var ridingController = vehicle.getRidingController();
            if (ridingController == null) return player.onGround();

            var activeContext = ridingController.getContext();
            if (activeContext == null) return player.onGround();

            if (activeContext.getStyle() == RidingStyle.AIR &&
                activeContext.getBehaviour().equals(HoverBehaviour.Companion.getKEY())) {
                return true;
            }
        }
        // Otherwise, return the actual value
        return player.onGround();
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    public void isInvulnerableTo(ServerLevel level, DamageSource source, CallbackInfoReturnable<Boolean> ci) {
        if ((Object) this instanceof ServerPlayer player) {
            boolean invulnerableInBattle = level.getGameRules().get(CobblemonGameRules.BATTLE_INVULNERABILITY);
            boolean inBattle = Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player) != null;
            if (invulnerableInBattle && inBattle) {
                ci.setReturnValue(true);
            }
        }
    }

    @Override @Nullable
    public PokedexEntityData resolvePokemonScan() {
        if (CompoundTagUtilities.isShoulderPokemon(this.cobblemon$getShoulderEntityRight())){
            return getDataFromShoulderPokemon(this.cobblemon$getShoulderEntityRight());
        }
        if (CompoundTagUtilities.isShoulderPokemon(this.cobblemon$getShoulderEntityLeft())){
            return getDataFromShoulderPokemon(this.cobblemon$getShoulderEntityLeft());
        }
        return null;
    }

    @Nullable @Unique
    private PokedexEntityData getDataFromShoulderPokemon(CompoundTag shoulderTag) {
        // port/26.2: only reachable server-side (see class comment); the 1.21.1 client branch rebuilt the
        // Pokémon from the synced shoulder NBT, which 26.2 no longer sends.
        if (!((Object) this instanceof ServerPlayer)) return null;
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(this.getUUID(), this.registryAccess());
        Pokemon pokemon = party.get(CompoundTagExtensionsKt.getUUID(shoulderTag, DataKeys.SHOULDER_UUID));
        return (pokemon == null) ? null : new PokedexEntityData(pokemon, null);
    }

    @Override
    public LivingEntity resolveEntityScan() {
        return this;
    }

    @Override
    public void absSnapTo(double x, double y, double z, float yaw, float pitch) {
        if (cobblemon$orientationController.getActive()) {
            this.absSnapTo(x, y, z);
            this.setYRot(yaw % 360.0f);
            this.setXRot(pitch % 360.0f);
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }
        else {
            this.absSnapTo(x, y, z);
            this.absSnapRotationTo(yaw, pitch);
        }
    }

    @WrapOperation(
            method = "rideTick()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;wantsToStopRiding()Z")
    )
    public boolean delegateDismountToController(Player instance, Operation<Boolean> original) {
        if (this.getVehicle() instanceof PokemonEntity pokemonEntity) {
            return pokemonEntity.ifRidingAvailableSupply(false, (behaviour, settings, state) -> {
                return behaviour.dismountOnShift(settings, state, pokemonEntity) && original.call(instance);
            });
        }
        return original.call(instance);
    }

    @Override
    public OrientationController getOrientationController() {
        return cobblemon$orientationController;
    }

    @Override
    public void setDriverInput(Vector3f driverInput) {
        cobblemon$driverInput = driverInput;
    }

    @Override
    public Vector3f getDriverInput() {
        return cobblemon$driverInput;
    }

    @Override
    public void setLastSentDriverInput(Vector3f lastSentDriverInput) {
        cobblemon$lastSentDriverInput = lastSentDriverInput;
    }

    @Override
    public Vector3f getLastSentDriverInput() {
        return cobblemon$lastSentDriverInput;
    }

    @Override
    public CompoundTag cobblemon$getShoulderEntityLeft() {
        return (Object) this instanceof ServerPlayer player ? player.getShoulderEntityLeft() : new CompoundTag();
    }

    @Override
    public CompoundTag cobblemon$getShoulderEntityRight() {
        return (Object) this instanceof ServerPlayer player ? player.getShoulderEntityRight() : new CompoundTag();
    }

    @Override
    public void cobblemon$setShoulderEntityLeft(CompoundTag tag) {
        if ((Object) this instanceof ServerPlayer player) player.setShoulderEntityLeft(tag);
    }

    @Override
    public void cobblemon$setShoulderEntityRight(CompoundTag tag) {
        if ((Object) this instanceof ServerPlayer player) player.setShoulderEntityRight(tag);
    }

    @Override
    public void cobblemon$respawnEntityOnShoulder(CompoundTag tag) {
        if ((Object) this instanceof ServerPlayer player) player.respawnEntityOnShoulder(tag);
    }

    @Override
    public float cobblemon$getRideXRot() {
        return this.cobblemon$rideXRot;
    }

    @Override
    public void cobblemon$setRideXRot(float rideXRot) {
        this.cobblemon$rideXRot = Mth.wrapDegrees(rideXRot);
    }

    @Override
    public float cobblemon$getRideYRot() {
        return this.cobblemon$rideYRot;
    }

    @Override
    public void cobblemon$setRideYRot(float rideYRot) {
        this.cobblemon$rideYRot = Mth.wrapDegrees(rideYRot);
    }

    @Override
    public Vec3 cobblemon$getRideEyePos() {
        return this.cobblemon$rideEyePos;
    }

    @Override
    public void cobblemon$setRideEyePos(Vec3 rideEyePos) {
        this.cobblemon$rideEyePos = rideEyePos;
    }

}
