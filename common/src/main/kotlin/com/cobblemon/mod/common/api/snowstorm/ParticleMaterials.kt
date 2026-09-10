/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.snowstorm

import net.minecraft.client.particle.ParticleRenderType

// PT131: ParticleRenderType became `final class extends Record` in MC 26.1 — cannot be subclassed.
// Use plain record-style instances; rendering pipeline migrated to GpuRenderPipeline (begin/depthMask/cull are no-ops).
// port/26.2: the record gained a second component, `shorthand`, used as the short display/debug name.
object ParticleMaterials {
    val ALPHA: ParticleRenderType = ParticleRenderType("ALPHA", "alpha")
    val ADD: ParticleRenderType = ParticleRenderType("ADD", "add")
    val BLEND: ParticleRenderType = ParticleRenderType("BLEND", "blend")
    val OPAQUE: ParticleRenderType = ParticleRenderType("OPAQUE", "opaque")
}
