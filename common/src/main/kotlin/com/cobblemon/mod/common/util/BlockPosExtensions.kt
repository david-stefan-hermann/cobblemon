/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.util

import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec3

/**
 * For conversion from BlockPos to Vec3d */
fun BlockPos.toVec3d(): Vec3 {
    return Vec3(this.x.toDouble(), this.y.toDouble(), this.z.toDouble())
}

// port/26.2: Vec3i.getCenter()/getBottomCenter() were removed; the equivalents are now the static
// factories Vec3.atCenterOf / Vec3.atBottomCenterOf. Re-exposed as extensions so call sites read the same.
val Vec3i.center: Vec3
    get() = Vec3.atCenterOf(this)

val Vec3i.bottomCenter: Vec3
    get() = Vec3.atBottomCenterOf(this)
