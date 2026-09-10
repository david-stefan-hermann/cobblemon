/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util

import org.joml.Matrix3x2f
import org.joml.Matrix3x2fStack

/*
 * port/26.2: the GUI pose stack changed from the 3D PoseStack to JOML's 2D Matrix3x2fStack, so the
 * three-argument transforms no longer exist - a third numeric argument now binds to the `dest` matrix
 * parameter of translate(float, float, Matrix3x2f) instead.
 *
 * Dropping the z component is the correct translation rather than a shortcut: in the old immediate-mode
 * GUI, z was only ever used to order draws (hence the recurring translate(0.0, 0.0, 100.0) calls). 26.2
 * orders GUI draws by stratum instead, so the z offset carries no meaning in the new pipeline.
 *
 * These also accept Double, which the 2D stack no longer does, so the many double-typed layout
 * expressions in the GUI code keep working without a cast at every call site.
 */

fun Matrix3x2fStack.translate(x: Double, y: Double): Matrix3x2f =
    this.translate(x.toFloat(), y.toFloat())

fun Matrix3x2fStack.translate(x: Double, y: Double, z: Double): Matrix3x2f =
    this.translate(x.toFloat(), y.toFloat())

fun Matrix3x2fStack.translate(x: Float, y: Float, z: Float): Matrix3x2f =
    this.translate(x, y)

fun Matrix3x2fStack.scale(x: Double, y: Double): Matrix3x2f =
    this.scale(x.toFloat(), y.toFloat())

fun Matrix3x2fStack.scale(x: Double, y: Double, z: Double): Matrix3x2f =
    this.scale(x.toFloat(), y.toFloat())

fun Matrix3x2fStack.scale(x: Float, y: Float, z: Float): Matrix3x2f =
    this.scale(x, y)
