/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.gui

import java.lang.ref.WeakReference
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Matrix3x2fStack

/**
 * port/26.2: finds the GuiGraphicsExtractor a GUI pose stack belongs to.
 *
 * Cobblemon's ~380 blitk calls only hand over `context.pose()`. In 1.21.1 that was enough, because blitk
 * drew immediately through the Tesselator; in 26.2 a textured quad has to be added to the extractor's
 * GUI render state. GuiGraphicsExtractorTrackerMixin registers every extractor on construction, and the
 * pose stack is matched by identity (Matrix3x2fStack.equals compares values, so no hash map).
 */
object GuiExtractorTracker {
    private const val CAPACITY = 8
    private val recent = ArrayDeque<WeakReference<GuiGraphicsExtractor>>()

    @JvmStatic
    fun track(extractor: GuiGraphicsExtractor) {
        synchronized(recent) {
            recent.removeAll { it.get() == null }
            recent.addFirst(WeakReference(extractor))
            while (recent.size > CAPACITY) recent.removeLast()
        }
    }

    fun forPose(pose: Matrix3x2fStack): GuiGraphicsExtractor? = synchronized(recent) {
        recent.firstNotNullOfOrNull { ref -> ref.get()?.takeIf { it.pose() === pose } }
    }
}
