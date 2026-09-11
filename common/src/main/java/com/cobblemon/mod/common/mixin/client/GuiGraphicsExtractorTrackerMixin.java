/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.client;

import com.cobblemon.mod.common.client.render.gui.GuiExtractorTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * port/26.2: lets blitk find the extractor behind a pose stack - see GuiExtractorTracker. The public
 * constructor delegates to this private one, so every extractor passes through here.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorTrackerMixin {
    @Inject(
        method = "<init>(Lnet/minecraft/client/Minecraft;Lorg/joml/Matrix3x2fStack;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;II)V",
        at = @At("RETURN")
    )
    private void cobblemon$track(Minecraft minecraft, Matrix3x2fStack pose, GuiRenderState state, int mouseX, int mouseY, CallbackInfo ci) {
        GuiExtractorTracker.track((GuiGraphicsExtractor) (Object) this);
    }
}
