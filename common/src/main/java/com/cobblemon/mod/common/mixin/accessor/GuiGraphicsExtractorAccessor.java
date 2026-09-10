/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.mixin.accessor;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * port/26.2: the GUI draws in 2D and 3D content goes through the picture-in-picture pipeline, which is
 * fed by adding a render state to the GuiRenderState. GuiGraphicsExtractor keeps that state private and
 * exposes no getter, so this accessor reaches it - it is the only way for a mod to draw a model into a
 * screen.
 */
@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsExtractorAccessor {

    @Accessor("guiRenderState")
    GuiRenderState cobblemon$getGuiRenderState();

}
