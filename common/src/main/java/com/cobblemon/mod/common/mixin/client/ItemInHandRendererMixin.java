/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.OrientationControllable;
import com.cobblemon.mod.common.item.PokedexItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    // port/26.2: renderArmWithItem is gone - hand rendering goes through submitHandsWithItems, which is
    // where the held items are drawn from and which carries the local player, so the Pokedex check moves
    // here.
    @Inject(method = "submitHandsWithItems", at = @At(value = "HEAD"), cancellable = true)
    private void cobblemon$submitHandsWithItems(float partialTick, PoseStack poseStack, SubmitNodeCollector collector, LocalPlayer player, int light, CallbackInfo ci) {
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof PokedexItem) {
            ci.cancel();
        }
    }

    // port/26.2: renamed to submitHandsWithItems, and PoseStack.mulPose takes the Quaternionfc interface.
    @Redirect(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"))
    private void cobblemon$renderHandswithItems(PoseStack instance, Quaternionfc quaternion) {
        if (!(Minecraft.getInstance().player != null && Minecraft.getInstance().player.getVehicle() instanceof OrientationControllable controllable && controllable.getOrientationController().isActive())) {
            instance.mulPose(quaternion);
        }
    }
}
