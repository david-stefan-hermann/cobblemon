/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.api.snowstorm

import net.minecraft.client.particle.ParticleRenderType

/*
 * port/26.2: ParticleRenderType is a final record now and can no longer be subclassed to carry a shader.
 * Custom per-material shaders are gone with ShaderInstance; in 26.2 a quad particle's blending comes
 * from the SingleQuadParticle.Layer it draws under, and a render type only names a particle group.
 *
 * These four still have to be registered with the particle engine - see CobblemonFabricClient - or the
 * engine has no group to put Snowstorm particles in and they never draw.
 */
object ParticleMaterials {
    val ALPHA: ParticleRenderType = ParticleRenderType("ALPHA", "alpha")
    val ADD: ParticleRenderType = ParticleRenderType("ADD", "add")
    val BLEND: ParticleRenderType = ParticleRenderType("BLEND", "blend")
    val OPAQUE: ParticleRenderType = ParticleRenderType("OPAQUE", "opaque")
}
