/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric.client

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonClientImplementation
import java.util.function.Supplier
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.color.block.BlockTintSource
import net.minecraft.client.color.item.ItemTintSource
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SpriteSet
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType

/**
 * PT150: fabric-api client modules required by the original CobblemonFabricClient are not yet
 * remapped for MC 26.1.x (BlockRenderLayerMap, ClientEntityEvents, ClientTickEvents,
 * ItemTooltipCallback, KeyBindingHelper, ModelLoadingPlugin, ClientPlayConnectionEvents,
 * ParticleFactoryRegistry, ColorProviderRegistry, EntityModelLayerRegistry, EntityRendererRegistry,
 * HudRenderCallback, WorldRenderEvents, IdentifiableResourceReloadListener, ResourceManagerHelper).
 *
 * Stubbed pending upstream port — onInitializeClient is a no-op and CobblemonClientImplementation
 * methods become no-ops so the Fabric client jar can compile. Reintroduce full client wiring in
 * PT15X+ when fabric-api ships a 26.1.x build.
 */
class CobblemonFabricClient : ClientModInitializer, CobblemonClientImplementation {
    override fun onInitializeClient() {
        Cobblemon.LOGGER.warn("CobblemonFabricClient.onInitializeClient stubbed (PT150) — fabric-api client 26.1.x not yet shipped.")
    }

    override fun registerLayer(modelLayer: ModelLayerLocation, supplier: Supplier<LayerDefinition>) {}

    override fun <T : ParticleOptions> registerParticleFactory(
        type: ParticleType<T>,
        factory: (SpriteSet) -> ParticleProvider<T>
    ) {
    }

    override fun registerBlockRenderType(layer: RenderType, vararg blocks: Block) {}

    override fun registerItemColors(provider: ItemTintSource, vararg items: Item) {}

    override fun registerBlockColors(provider: BlockTintSource, vararg blocks: Block) {}

    override fun <T : BlockEntity> registerBlockEntityRenderer(
        type: BlockEntityType<out T>,
        factory: BlockEntityRendererProvider<T, BlockEntityRenderState>
    ) {
    }

    override fun <T : Entity> registerEntityRenderer(
        type: EntityType<out T>,
        factory: EntityRendererProvider<T>
    ) {
    }
}
