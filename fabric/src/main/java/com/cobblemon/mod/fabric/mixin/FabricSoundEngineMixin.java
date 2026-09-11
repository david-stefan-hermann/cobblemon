/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric.mixin;

import com.cobblemon.mod.common.api.riding.sound.RideAttenuationModel;
import com.cobblemon.mod.common.api.riding.sound.RideLoopSound;
import com.cobblemon.mod.common.client.sound.instances.AlphaCrySoundInstance;
import com.cobblemon.mod.common.duck.ChannelDuck;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * port/26.2: play now returns a PlayResult, tickNonPaused became tickInGameSound, and the unobfuscated
 * jar carries Mojang's local names (h/i/g/bl/bl2/bl3/vec3/channelHandle were loom's placeholders) - the
 * handlers keep their old parameter names and bind them to the 26.2 locals.
 */
@Mixin(SoundEngine.class)
public class FabricSoundEngineMixin {
    @Inject(method = "play",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/ChannelAccess$ChannelHandle;execute(Ljava/util/function/Consumer;)V"
            ),
            cancellable = true
    )
    private void cobblemon$overrideChannelExecute(
            SoundInstance sound,
            CallbackInfoReturnable<SoundEngine.PlayResult> ci,
            @Local(name = "volume") float h,
            @Local(name = "pitch") float i,
            @Local(name = "attenuationDistance") float g,
            @Local(name = "isLooping") boolean bl2,
            @Local(name = "isStreaming") boolean bl3,
            @Local(name = "isRelative") boolean bl,
            @Local(name = "position") Vec3 vec3,
            @Local(name = "handle") ChannelAccess.ChannelHandle handle
    ) {
        if (!(sound instanceof RideLoopSound rideSound)) return;
        if (handle == null) return;

        handle.execute((channel) -> {
            channel.setPitch(i);
            channel.setVolume(h);

            if (rideSound.getRideAttenuation() == RideAttenuationModel.LINEAR) {
                channel.linearAttenuation(g);
            } else if (rideSound.getRideAttenuation() == RideAttenuationModel.EXPONENTIAL){
                ((ChannelDuck) channel).cobblemon$inverseAttenuation(g);
            } else {
                channel.disableAttenuation();
            }

            // Apply low pass filter if the rideSound dictate that it should be muffled
            if (rideSound.getShouldMuffle()) {
                ((ChannelDuck) channel).cobblemon$applyLowPassFilter(1.0f, rideSound.getMuffleAmount());
            } else {
                ((ChannelDuck) channel).cobblemon$clearFilters();
            }

            channel.setLooping(bl2 && !bl3);
            channel.setSelfPosition(vec3);
            channel.setRelative(bl);
        });
    }

    @Inject(method = "play",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/ChannelAccess$ChannelHandle;execute(Ljava/util/function/Consumer;)V"
            )
    )
    private void cobblemon$applyAlphaReverb(
            SoundInstance sound,
            CallbackInfoReturnable<SoundEngine.PlayResult> ci,
            @Local(name = "handle") ChannelAccess.ChannelHandle handle
    ) {
        if (!(sound instanceof AlphaCrySoundInstance)) return;
        if (handle == null) return;

        // TODO: probably not make these values hardcoded? Maybe add to alphaCrySoundInstance?
        handle.execute(channel ->
                ((ChannelDuck) channel).cobblemon$applyReverb(1.8f, 0.25f, 0.1f, 0.9f)
        );
    }

    @Inject(
            method = "tickInGameSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/ChannelAccess$ChannelHandle;execute(Ljava/util/function/Consumer;)V",
                    ordinal = 0
            )
    )
    private void cobblemon$tickableSoundTick(
            CallbackInfo ci,
            @Local(name = "instance") TickableSoundInstance instance,
            @Local(name = "handle") ChannelAccess.ChannelHandle handle,
            @Local(name = "volume") float volume,
            @Local(name = "pitch") float pitch,
            @Local(name = "position") Vec3 vec3
    ) {
        if (!(instance instanceof RideLoopSound rideSound)) return;

        handle.execute(channel -> {
            channel.setVolume(volume);
            channel.setPitch(pitch);
            channel.setSelfPosition(vec3);

            // Apply low pass filter if the rideSound dictate that it should be muffled
            if (rideSound.getShouldMuffle()) {
                ((ChannelDuck) channel).cobblemon$applyLowPassFilter(1.0f, rideSound.getMuffleAmount());
            } else {
                ((ChannelDuck) channel).cobblemon$clearFilters();
            }
        });
    }
}
