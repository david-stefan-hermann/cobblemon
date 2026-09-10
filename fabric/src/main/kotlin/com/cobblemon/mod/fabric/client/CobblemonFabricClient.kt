/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric.client

import com.cobblemon.mod.common.CobblemonClientImplementation
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.pokeball.PokeBalls
import com.cobblemon.mod.common.client.CobblemonBakingOverrides
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.CobblemonClient.pokedexUsageContext
import com.cobblemon.mod.common.client.CobblemonClient.reloadCodedAssets
import com.cobblemon.mod.common.client.keybind.CobblemonKeyBinds
import com.cobblemon.mod.common.client.pokedex.PokedexType
import com.cobblemon.mod.common.client.render.atlas.CobblemonAtlases
import com.cobblemon.mod.common.client.render.item.CobblemonModelPredicateRegistry
import com.cobblemon.mod.common.item.PokedexItem
import com.cobblemon.mod.common.particle.CobblemonParticles
import com.cobblemon.mod.common.particle.SnowstormParticleType
import com.cobblemon.mod.common.platform.events.ClientEntityEvent
import com.cobblemon.mod.common.platform.events.ClientPlayerEvent
import com.cobblemon.mod.common.platform.events.ClientTickEvent
import com.cobblemon.mod.common.platform.events.ItemTooltipEvent
import com.cobblemon.mod.common.platform.events.PlatformEvents
import com.cobblemon.mod.common.platform.events.RenderEvent
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.isUsingPokedex
import com.cobblemon.mod.fabric.CobblemonFabric
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey
import net.fabricmc.fabric.api.client.model.loading.v1.FabricModelManager
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.color.block.BlockTintSource
import net.minecraft.client.color.item.ItemTintSource
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SpriteSet
import net.minecraft.client.renderer.block.dispatch.BlockStateModel
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType

/**
 * port/26.2: this class had been reduced to a no-op on the grounds that fabric-api shipped no client
 * modules for MC 26.1.x. That no longer holds on 26.2 - the modules exist, several under new names:
 *
 *   HudRenderCallback        -> HudElementRegistry (rendering v1 hud)
 *   ParticleFactoryRegistry  -> ParticleProviderRegistry
 *   ColorProviderRegistry    -> BlockColorRegistry
 *   EntityModelLayerRegistry -> ModelLayerRegistry
 *   KeyBindingHelper         -> KeyMappingHelper
 *   WorldRenderEvents        -> LevelRenderEvents (rendering v1 level)
 *   BlockEntityRenderers     -> BlockEntityRendererRegistry
 *
 * The client wiring is therefore restored. Without it the mod registers no renderers, particles, key
 * binds, model layers or HUD overlays at all, and nothing of Cobblemon appears in game.
 */
class CobblemonFabricClient : ClientModInitializer, CobblemonClientImplementation {
    override fun onInitializeClient() {
        registerParticleFactory(CobblemonParticles.SNOWSTORM_PARTICLE_TYPE, SnowstormParticleType::Factory)
        CobblemonClient.initialize(this)

        ModelLoadingPlugin.register { context ->
            registerBakingOverrides(context)
            context.addModels(*PokeBalls.all().map { pokeBall -> pokeBall.model3d }.toTypedArray())
            PokedexType.entries.toList().forEach { pokedex ->
                context.addModels(
                    pokedex.getItemModelPath(),
                    pokedex.getItemModelPath("scanning"),
                    pokedex.getItemModelPath("flat"),
                    pokedex.getItemModelPath("flat_off"),
                    pokedex.getItemModelPath("off")
                )
            }
            CobblemonItems.wearables.forEach { wearable -> context.addModels(wearable.getModel3d()) }
        }

        CobblemonFabric.networkManager.registerClientHandlers()

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(object : IdentifiableResourceReloadListener {
            override fun reload(
                synchronizer: PreparableReloadListener.PreparationBarrier,
                manager: ResourceManager,
                prepareProfiler: ProfilerFiller,
                applyProfiler: ProfilerFiller,
                prepareExecutor: Executor,
                applyExecutor: Executor
            ): CompletableFuture<Void> {
                val atlasFutures = mutableListOf<CompletableFuture<Void>>()
                CobblemonAtlases.atlases.forEach {
                    atlasFutures.add(it.reload(synchronizer, manager, prepareProfiler, applyProfiler, prepareExecutor, applyExecutor))
                }
                return CompletableFuture.allOf(*atlasFutures.toTypedArray()).thenRun {
                    reloadCodedAssets(manager)
                }
            }

            override fun getFabricId() = cobblemonResource("atlases")
        })

        registerHudElements()

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
            val player = client.player
            if (player != null) {
                val itemStack = player.mainHandItem
                val offhandStack = player.offhandItem
                if (((itemStack.item is PokedexItem && player.usedItemHand == InteractionHand.MAIN_HAND) ||
                    (offhandStack.item is PokedexItem && player.usedItemHand == InteractionHand.OFF_HAND)) &&
                    player.isUsingItem &&
                    pokedexUsageContext.scanningGuiOpen
                ) {
                    pokedexUsageContext.attackKeyHeld(client.options.keyAttack.isDown)
                }
            }
        })

        CobblemonKeyBinds.register(KeyMappingHelper::registerKeyMapping)

        ClientEntityEvents.ENTITY_LOAD.register { entity, level -> PlatformEvents.CLIENT_ENTITY_LOAD.post(ClientEntityEvent.Load(entity, level)) }
        ClientEntityEvents.ENTITY_UNLOAD.register { entity, level -> PlatformEvents.CLIENT_ENTITY_UNLOAD.post(ClientEntityEvent.Unload(entity, level)) }
        ClientTickEvents.START_CLIENT_TICK.register { client -> PlatformEvents.CLIENT_TICK_PRE.post(ClientTickEvent.Pre(client)) }
        ClientTickEvents.END_CLIENT_TICK.register { client -> PlatformEvents.CLIENT_TICK_POST.post(ClientTickEvent.Post(client)) }
        ClientPlayConnectionEvents.JOIN.register { _, _, client -> client.player?.let { PlatformEvents.CLIENT_PLAYER_LOGIN.post(ClientPlayerEvent.Login(it)) } }
        ClientPlayConnectionEvents.DISCONNECT.register { _, client -> client.player?.let { PlatformEvents.CLIENT_PLAYER_LOGOUT.post(ClientPlayerEvent.Logout(it)) } }
        ItemTooltipCallback.EVENT.register { stack, context, type, lines -> PlatformEvents.CLIENT_ITEM_TOOLTIP.post(ItemTooltipEvent(stack, context, type, lines)) }

        // port/26.2: WorldRenderEvents.AFTER_TRANSLUCENT is now LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.
        // The context no longer carries the matrices or the camera; drawing goes through the collector, and
        // the live camera and delta tracker are read off Minecraft.
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register { context ->
            val minecraft = Minecraft.getInstance()
            PlatformEvents.RENDER.post(
                RenderEvent(
                    stage = RenderEvent.Stage.TRANSLUCENT,
                    levelRenderer = context.levelRenderer(),
                    poseStack = context.poseStack(),
                    collector = context.submitNodeCollector(),
                    tickCounter = minecraft.deltaTracker,
                    camera = minecraft.gameRenderer.mainCamera()
                )
            )
        }

        CobblemonModelPredicateRegistry.registerPredicates()
    }

    /**
     * port/26.2: models that belong to no block state used to be baked through a ModelResourceLocation
     * registered by a model loader mixin. 26.2 removed that registry; fabric-api's replacement keys each
     * extra model by an opaque ExtraModelKey, so each override gets a key here and is handed a resolver
     * that looks the baked model up on demand. Lazy lookup matters because models are re-baked on every
     * resource reload - a value captured once would go stale.
     */
    private fun registerBakingOverrides(context: ModelLoadingPlugin.Context) {
        CobblemonBakingOverrides.models.forEach { override ->
            val key = ExtraModelKey.create<BlockStateModel> { override.modelLocation.toString() }
            context.addModel(key, SimpleUnbakedExtraModel.blockStateModel(override.modelLocation))
            override.modelResolver = {
                (Minecraft.getInstance().modelManager as FabricModelManager).getModel(key)
            }
        }
    }

    /**
     * port/26.2: the mod's HUD overlays are fabric-api HudElements now rather than Gui subclasses. They
     * are attached before the chat element so they keep sitting underneath chat, which is what the old
     * mixin into Gui.renderCameraOverlays achieved.
     */
    private fun registerHudElements() {
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            cobblemonResource("party_overlay"),
            HudElement { context, tickCounter -> CobblemonClient.overlay.extractRenderState(context, tickCounter) }
        )
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            cobblemonResource("battle_overlay"),
            HudElement { context, tickCounter -> CobblemonClient.battleOverlay.extractRenderState(context, tickCounter) }
        )
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            cobblemonResource("ride_controls_overlay"),
            HudElement { context, tickCounter -> CobblemonClient.rideControlsOverlay.extractRenderState(context, tickCounter) }
        )
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            cobblemonResource("pokedex_overlay"),
            HudElement { context, tickCounter ->
                val player = Minecraft.getInstance().player
                if (player != null) {
                    if (player.isUsingPokedex() || pokedexUsageContext.transitionIntervals > 0) {
                        if (!player.isUsingItem) pokedexUsageContext.resetState(false)
                        pokedexUsageContext.renderUpdate(context, tickCounter)
                    } else {
                        pokedexUsageContext.resetState()
                    }
                }
            }
        )
    }

    override fun registerLayer(modelLayer: ModelLayerLocation, supplier: Supplier<LayerDefinition>) {
        ModelLayerRegistry.registerModelLayer(modelLayer) { supplier.get() }
    }

    override fun <T : ParticleOptions> registerParticleFactory(
        type: ParticleType<T>,
        factory: (SpriteSet) -> ParticleProvider<T>
    ) {
        ParticleProviderRegistry.getInstance().register(type) { spriteSet: SpriteSet -> factory(spriteSet) }
    }

    // port/26.2: BlockRenderLayerMap is gone - in 26.2 a block declares its chunk render layer through its
    // model data rather than having one registered from code, so there is nothing to wire up here.
    override fun registerBlockRenderType(layer: RenderType, vararg blocks: Block) {}

    // port/26.2: fabric-api no longer has an item colour registry; item tinting is driven by the
    // ItemTintSource components carried on the item itself.
    override fun registerItemColors(provider: ItemTintSource, vararg items: Item) {}

    override fun registerBlockColors(provider: BlockTintSource, vararg blocks: Block) {
        BlockColorRegistry.register(listOf(provider), *blocks)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity, S : BlockEntityRenderState> registerBlockEntityRenderer(
        type: BlockEntityType<out T>,
        factory: BlockEntityRendererProvider<T, S>
    ) {
        BlockEntityRendererRegistry.register(type as BlockEntityType<T>, factory)
    }

    override fun <T : Entity> registerEntityRenderer(
        type: EntityType<out T>,
        factory: EntityRendererProvider<T>
    ) {
        EntityRendererRegistry.register(type, factory)
    }
}
