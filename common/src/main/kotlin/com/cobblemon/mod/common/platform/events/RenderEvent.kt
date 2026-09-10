/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.platform.events

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.DeltaTracker
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.SubmitNodeCollector

/**
 * Event fired during the various [Stage]s of the [LevelRenderer].
 *
 * @author Segfault Guy
 * @since September 11th, 2024
 */
data class RenderEvent(
    val stage: Stage,
    val levelRenderer: LevelRenderer,
    val poseStack: PoseStack,
    // port/26.2: the level render context no longer exposes the model-view and projection matrices, which
    // nothing here read anyway. It does expose the collector, which subscribers now need in order to draw
    // at all, so that replaces them.
    val collector: SubmitNodeCollector,
    val tickCounter: DeltaTracker,
    val camera: Camera
) {

    /** Represents when a layer is rendered by [LevelRenderer.renderLevel]. Ordinal corresponds with its render order. */
    enum class Stage {
        TRANSLUCENT
    }
}