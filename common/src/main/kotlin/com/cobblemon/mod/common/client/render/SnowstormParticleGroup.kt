/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render

import com.cobblemon.mod.common.api.snowstorm.ParticleMaterials
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.particle.ParticleEngine
import net.minecraft.client.particle.ParticleGroup
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState
import net.minecraft.client.renderer.texture.TextureAtlas

/**
 * port/26.2: the particle group Snowstorm particles live in.
 *
 * The engine keeps one group per ParticleRenderType, and the port had registered Cobblemon's types against
 * vanilla's QuadParticleGroup - which casts every particle to SingleQuadParticle and crashed the frame the
 * first Snowstorm effect was drawn. Snowstorm particles build their own quads (non-square, custom UVs and
 * camera modes), so this group lets each one write them into a [RecordedGeometry] during extraction and
 * submits the result with the matching particle pipeline on the particle atlas.
 *
 * Additive blending (ParticleMaterial.ADD) still draws translucent: 26.2 has no additive particle pipeline
 * and Cobblemon's old shader route is gone.
 */
class SnowstormParticleGroup(
    engine: ParticleEngine,
    particleType: ParticleRenderType
) : ParticleGroup<SnowstormParticle>(engine) {
    private val renderType = renderTypeFor(particleType)
    private val renderState = SnowstormParticleRenderState(renderType)

    override fun extractRenderState(frustum: Frustum, camera: Camera, partialTicks: Float): ParticleGroupRenderState {
        val geometry = RecordedGeometry()
        for (particle in particles) {
            particle.render(geometry, camera, partialTicks)
        }
        renderState.geometry = geometry
        return renderState
    }

    class SnowstormParticleRenderState(private val renderType: RenderType) : ParticleGroupRenderState {
        var geometry: RecordedGeometry? = null

        override fun submit(collector: SubmitNodeCollector, camera: CameraRenderState) {
            val recorded = geometry ?: return
            if (recorded.isEmpty) return
            // Vertices are already camera-relative, which is what the level pass expects at identity.
            collector.submitCustomGeometry(PoseStack(), renderType) { _, consumer -> recorded.replay(consumer) }
        }

        override fun clear() {
            geometry = null
        }
    }

    companion object {
        private val OPAQUE: RenderType = RenderType.create(
            "cobblemon_snowstorm_opaque",
            RenderSetup.builder(RenderPipelines.OPAQUE_PARTICLE)
                .withTexture("Sampler0", TextureAtlas.LOCATION_PARTICLES)
                .useLightmap()
                .createRenderSetup()
        )

        private val TRANSLUCENT: RenderType = RenderType.create(
            "cobblemon_snowstorm_translucent",
            RenderSetup.builder(RenderPipelines.TRANSLUCENT_PARTICLE)
                .withTexture("Sampler0", TextureAtlas.LOCATION_PARTICLES)
                .useLightmap()
                .createRenderSetup()
        )

        fun renderTypeFor(particleType: ParticleRenderType): RenderType = when (particleType) {
            ParticleMaterials.OPAQUE, ParticleMaterials.ALPHA -> OPAQUE
            else -> TRANSLUCENT
        }
    }
}
