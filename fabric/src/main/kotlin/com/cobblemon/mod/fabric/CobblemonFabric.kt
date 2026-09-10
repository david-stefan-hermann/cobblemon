/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.fabric

import com.cobblemon.mod.common.*
import com.cobblemon.mod.common.CobblemonMobEffects
import com.cobblemon.mod.common.advancement.CobblemonCriteria
import com.cobblemon.mod.common.advancement.predicate.CobblemonEntitySubPredicates
import com.cobblemon.mod.common.api.net.serializers.*
import com.cobblemon.mod.common.item.group.CobblemonItemGroups
import com.cobblemon.mod.common.loot.LootInjector
import com.cobblemon.mod.common.particle.CobblemonParticles
import com.cobblemon.mod.common.platform.events.*
import com.cobblemon.mod.common.sherds.CobblemonSherds
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.didSleep
import com.cobblemon.mod.common.world.CobblemonStructures
import com.cobblemon.mod.common.world.feature.CobblemonFeatures
import com.cobblemon.mod.common.world.placementmodifier.CobblemonPlacementModifierTypes
import com.cobblemon.mod.common.world.predicate.CobblemonBlockPredicates
import com.cobblemon.mod.common.world.structureprocessors.CobblemonProcessorTypes
import com.cobblemon.mod.common.world.structureprocessors.CobblemonStructureProcessorListOverrides
import com.cobblemon.mod.fabric.net.CobblemonFabricNetworkManager
import com.cobblemon.mod.fabric.permission.FabricPermissionValidator
import com.mojang.brigadier.arguments.ArgumentType
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.biome.v1.BiomeModifications
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricEntityDataRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.world.poi.PoiHelper
import net.fabricmc.fabric.api.registry.CompostableRegistry
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType
import net.fabricmc.fabric.impl.resource.ResourceLoaderImpl
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import net.minecraft.commands.synchronization.ArgumentTypeInfo
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.stats.Stats
import net.minecraft.tags.TagKey
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.gamerules.GameRule
import net.minecraft.world.level.gamerules.GameRuleCategory
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.reflect.KClass

/**
 * port/26.2: this was reduced to a no-op stub (PT150) while fabric-api had no 26.x build, which meant
 * the mod registered nothing at all - no blocks, no items, no entities, no commands - and a server
 * refused to load because every Cobblemon tag pointed at something that did not exist.
 *
 * fabric-api 0.160.0+26.2 has everything back, mostly under new names:
 *
 *   ServerEntityWorldChangeEvents -> ServerEntityLevelChangeEvents (AFTER_PLAYER_CHANGE_LEVEL)
 *   FabricItemGroup               -> FabricCreativeModeTab
 *   ItemGroupEvents               -> CreativeModeTabEvents (modifyOutputEvent)
 *   FabricItemGroupEntries        -> FabricCreativeModeTabOutput
 *   PointOfInterestHelper         -> PoiHelper
 *   CompostingChanceRegistry      -> CompostableRegistry
 *   GameRuleRegistry              -> GameRuleBuilder (26.2 unified game rules into GameRule<T>)
 *   loot v2                       -> loot v3 (Modify now takes the table key and a registry lookup)
 *
 * The one thing with no counterpart is TradeOfferHelper: fabric-api 26.2 ships no villager trade API,
 * so Cobblemon's trade offers cannot be registered yet - see [registerVillagers].
 */
object CobblemonFabric : CobblemonImplementation {

    override val modAPI = ModAPI.FABRIC

    private var server: MinecraftServer? = null

    override val networkManager = CobblemonFabricNetworkManager

    fun initialize() {
        Cobblemon.preInitialize(this)

        Cobblemon.statistics.registerStats()
        Cobblemon.statistics.stats.forEach { entry ->
            val cobblemonStat = entry.value
            Registry.register(BuiltInRegistries.CUSTOM_STAT, cobblemonStat.resourceLocation, cobblemonStat.resourceLocation)
            Stats.CUSTOM.get(cobblemonStat.resourceLocation, cobblemonStat.formatter)
        }

        Cobblemon.initialize()
        networkManager.registerMessages()
        networkManager.registerServerHandlers()

        //This has to be registered elsewhere on forge so we cant do it in common
        CobblemonSherds.registerSherds()
        CobblemonBlockPredicates.touch()
        CobblemonPlacementModifierTypes.touch()
        CobblemonProcessorTypes.touch()
        CobblemonActivities.activities.forEach { Registry.register(BuiltInRegistries.ACTIVITY, cobblemonResource(it.name), it) }
        CobblemonSensors.sensors.forEach { (key, sensorType) -> Registry.register(BuiltInRegistries.SENSOR_TYPE, cobblemonResource(key), sensorType) }
        CobblemonMemories.memories.forEach { (key, memoryModuleType) -> Registry.register(BuiltInRegistries.MEMORY_MODULE_TYPE, cobblemonResource(key), memoryModuleType) }

        EntitySleepEvents.STOP_SLEEPING.register { playerEntity, _ ->
            if (playerEntity !is ServerPlayer) {
                return@register
            }
            playerEntity.didSleep()
        }

        Cobblemon.builtinPacks
            .filter { it.neededMods.all(Cobblemon.implementation::isModInstalled) }
            .forEach {
                val mod = FabricLoader.getInstance().getModContainer(Cobblemon.MODID).get()
                val resourcePackActivationType = when (it.activationBehaviour) {
                    ResourcePackActivationBehaviour.NORMAL -> PackActivationType.NORMAL
                    ResourcePackActivationBehaviour.DEFAULT_ENABLED -> PackActivationType.DEFAULT_ENABLED
                    ResourcePackActivationBehaviour.ALWAYS_ENABLED -> PackActivationType.ALWAYS_ENABLED
                }
                val id = cobblemonResource(it.id)
                val subPath = "${ if (it.packType == PackType.CLIENT_RESOURCES) "resourcepacks" else "datapacks" }/${id.path}"
                // port/26.2: the public registerBuiltinPack derives the folder from the pack id, which only
                // works for resource packs. Cobblemon ships data packs too, so the path stays explicit and
                // this goes through the same implementation the public overloads call.
                ResourceLoaderImpl.registerBuiltinPack(id, subPath, mod, it.displayName, resourcePackActivationType)
            }

        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register { player, isLogin ->
            if (isLogin) {
                Cobblemon.dataProvider.sync(player)
            }
        }
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            this.server = server
            PlatformEvents.SERVER_STARTING.post(ServerEvent.Starting(server))
            CobblemonStructures.registerJigsaws(server)
            CobblemonStructureProcessorListOverrides.register(server)
        }
        ServerLifecycleEvents.SERVER_STARTED.register { server -> PlatformEvents.SERVER_STARTED.post(ServerEvent.Started(server)) }
        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            server.playerList.players.forEach { player -> PlatformEvents.SERVER_PLAYER_LOGOUT.post(ServerPlayerEvent.Logout(player)) }
            PlatformEvents.SERVER_STOPPING.post(ServerEvent.Stopping(server))
        }
        ServerLifecycleEvents.SERVER_STOPPED.register { server -> PlatformEvents.SERVER_STOPPED.post(ServerEvent.Stopped(server)) }
        ServerTickEvents.START_SERVER_TICK.register { server -> PlatformEvents.SERVER_TICK_PRE.post(ServerTickEvent.Pre(server)) }
        ServerTickEvents.END_SERVER_TICK.register { server -> PlatformEvents.SERVER_TICK_POST.post(ServerTickEvent.Post(server)) }
        ServerPlayConnectionEvents.JOIN.register { handler, _, server -> server.executeIfPossible { PlatformEvents.SERVER_PLAYER_LOGIN.post(ServerPlayerEvent.Login(handler.player)) } }
        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            if (!server.isStopped) server.executeIfPossible { PlatformEvents.SERVER_PLAYER_LOGOUT.post(ServerPlayerEvent.Logout(handler.player)) }
        }
        ServerLivingEntityEvents.ALLOW_DEATH.register { entity, _, _ ->
            if (entity is ServerPlayer) {
                PlatformEvents.PLAYER_DEATH.postThen(
                    event = ServerPlayerEvent.Death(entity),
                    ifSucceeded = {},
                    ifCanceled = { return@register false }
                )
            }
            return@register true
        }

        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register { player, origin, destination ->
            PlatformEvents.CHANGE_DIMENSION.post(ChangeDimensionEvent(player, origin, destination))
        }

        UseBlockCallback.EVENT.register { player, _, hand, hitResult ->
            val serverPlayer = player as? ServerPlayer ?: return@register InteractionResult.PASS
            PlatformEvents.RIGHT_CLICK_BLOCK.postThen(
                event = ServerPlayerEvent.RightClickBlock(serverPlayer, hitResult.blockPos, hand, hitResult.direction),
                ifSucceeded = {},
                ifCanceled = { return@register InteractionResult.FAIL }
            )
            return@register InteractionResult.PASS
        }

        UseEntityCallback.EVENT.register { player, _, hand, entity, _ ->
            val item = player.getItemInHand(hand)
            val serverPlayer = player as? ServerPlayer ?: return@register InteractionResult.PASS

            PlatformEvents.RIGHT_CLICK_ENTITY.postThen(
                event = ServerPlayerEvent.RightClickEntity(serverPlayer, item, hand, entity),
                ifSucceeded = {},
                ifCanceled = { return@register InteractionResult.FAIL }
            )

            return@register InteractionResult.PASS
        }

        // port/26.2: loot v3 hands over the table's ResourceKey rather than a bare id, plus the source
        // and a registry lookup the injector does not need.
        LootTableEvents.MODIFY.register { key, tableBuilder, _, _ ->
            LootInjector.attemptInjection(key.identifier()) { pool -> tableBuilder.withPool(pool) }
        }

        CommandRegistrationCallback.EVENT.register(CobblemonCommands::register)
    }

    override fun isModInstalled(id: String) = FabricLoader.getInstance().isModLoaded(id)

    override fun environment(): Environment {
        return when(FabricLoader.getInstance().environmentType) {
            EnvType.CLIENT -> Environment.CLIENT
            EnvType.SERVER -> Environment.SERVER
            else -> throw IllegalStateException("Fabric implementation cannot resolve environment yet")
        }
    }

    override fun registerPermissionValidator() {
        if (this.isModInstalled("fabric-permissions-api-v0")) {
            Cobblemon.permissionValidator = FabricPermissionValidator()
        }
    }

    override fun registerSoundEvents() {
        CobblemonSounds.register { identifier, sound -> Registry.register(CobblemonSounds.registry, identifier, sound) }
    }

    override fun registerDataComponents() {
        CobblemonItemComponents.register { identifier, component -> Registry.register(CobblemonItemComponents.registry, identifier, component) }
    }

    /**
     * port/26.2: fabric-api rejects EntityDataSerializers.registerSerializer from a mod outright - raw
     * registration hands out ids by insertion order, which desynchronises as soon as two sides load
     * mods differently. Serializers are registered under an id instead, and that id is what goes over
     * the wire.
     */
    override fun registerEntityDataSerializers() {
        registerEntityDataSerializer("vec3", Vec3DataSerializer)
        registerEntityDataSerializer("string_set", StringSetDataSerializer)
        registerEntityDataSerializer("pose_type", PoseTypeDataSerializer)
        registerEntityDataSerializer("platform_type", PlatformTypeDataSerializer)
        registerEntityDataSerializer("identifier", IdentifierDataSerializer)
        registerEntityDataSerializer("uuid_set", UUIDSetDataSerializer)
        registerEntityDataSerializer("npc_player_texture", NPCPlayerTextureSerializer)
        registerEntityDataSerializer("ride_boosts", RideBoostsDataSerializer)
        // port/26.2: EntityDataSerializers.OPTIONAL_UUID is gone from vanilla, so Cobblemon carries its
        // own - see OptionalUUIDDataSerializer.
        registerEntityDataSerializer("optional_uuid", OptionalUUIDDataSerializer)
    }

    private fun registerEntityDataSerializer(name: String, serializer: EntityDataSerializer<*>) {
        FabricEntityDataRegistry.register(cobblemonResource(name), serializer)
    }

    override fun registerItems() {
        CobblemonItems.register { identifier, item -> Registry.register(CobblemonItems.registry, identifier, item) }
        CobblemonItemGroups.register { provider ->
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, provider.key, FabricCreativeModeTab.builder()
                .title(provider.displayName)
                .icon(provider.displayIconProvider)
                .displayItems(provider.entryCollector)
                .build())
        }

        CobblemonItemGroups.injectorKeys().forEach { key ->
            CreativeModeTabEvents.modifyOutputEvent(key).register { output ->
                CobblemonItemGroups.inject(key, FabricItemGroupInjector(output))
            }
        }
    }

    override fun registerBlocks() {
        CobblemonBlocks.register { identifier, item -> Registry.register(CobblemonBlocks.registry, identifier, item) }
        CobblemonBlocks.strippedBlocks().forEach(StrippableBlockRegistry::register)
    }

    override fun registerEntityTypes() {
        CobblemonEntities.register { identifier, type -> Registry.register(CobblemonEntities.registry, identifier, type) }
    }

    override fun registerEntityAttributes() {
        CobblemonEntities.registerAttributes { entityType, builder -> FabricDefaultAttributeRegistry.register(entityType, builder) }
    }

    override fun registerBlockEntityTypes() {
        CobblemonBlockEntities.register { identifier, type -> Registry.register(CobblemonBlockEntities.registry, identifier, type) }
    }

    override fun registerPoiTypes() {
        CobblemonPoiTypes.register { identifier, type -> PoiHelper.register(identifier, type.maxTickets(), type.validRange(), type.matchingStates()) }
    }

    override fun registerVillagers() {
        CobblemonVillagerProfessions.register { identifier, profession -> Registry.register(CobblemonVillagerProfessions.registry, identifier, profession) }

        // port/26.2: fabric-api 26.2 ships no replacement for TradeOfferHelper, so the nurse's trades and
        // Cobblemon's wandering trader offers are not registered yet. The profession itself exists, so
        // villagers can still take the job; they just have nothing Cobblemon-specific to sell.
        Cobblemon.LOGGER.info("Cobblemon trade offers are not registered: fabric-api 26.2 has no villager trade API yet.")
    }

    override fun registerRecipeSerializers() {
        CobblemonRecipeSerializers.register { identifier, factory -> Registry.register(CobblemonRecipeSerializers.registry, identifier, factory) }
    }

    override fun registerRecipeTypes() {
        CobblemonRecipeTypes.register { identifier, factory -> Registry.register(CobblemonRecipeTypes.registry, identifier, factory) }
    }

    override fun registerWorldGenFeatures() {
        CobblemonFeatures.register { identifier, feature -> Registry.register(CobblemonFeatures.registry, identifier, feature) }
    }

    override fun registerParticles() {
        CobblemonParticles.register { identifier, particleType -> Registry.register(CobblemonParticles.registry, identifier, particleType) }
    }

    override fun registerMenu() {
        CobblemonMenuType.register { identifier, factory -> Registry.register(CobblemonMenuType.registry, identifier, factory) }
    }

    override fun addFeatureToWorldGen(feature: ResourceKey<PlacedFeature>, step: GenerationStep.Decoration, validTag: TagKey<Biome>?) {
        val predicate: (BiomeSelectionContext) -> Boolean = { context -> validTag == null || context.hasTag(validTag) }
        BiomeModifications.addFeature(predicate, step, feature)
    }

    override fun <A : ArgumentType<*>, T : ArgumentTypeInfo.Template<A>> registerCommandArgument(identifier: Identifier, argumentClass: KClass<A>, serializer: ArgumentTypeInfo<A, T>) {
        ArgumentTypeRegistry.registerArgumentType(identifier, argumentClass.java, serializer)
    }

    /**
     * port/26.2: game rules became a single GameRule<T> built by a builder rather than a Key/Type pair
     * handed to a registry, so the rule is built and registered in one go under a Cobblemon id.
     */
    override fun registerGameRule(name: String, category: GameRuleCategory, defaultValue: Boolean): GameRule<Boolean> =
        GameRuleBuilder.forBoolean(defaultValue)
            .category(category)
            .buildAndRegister(cobblemonResource(name))

    override fun registerCriteria() {
        CobblemonCriteria.register { id, obj ->
            Registry.register(CobblemonCriteria.registry, id, obj)
        }
    }

    override fun registerEntitySubPredicates() {
        CobblemonEntitySubPredicates.register { identifier, mapCodec ->
            Registry.register(BuiltInRegistries.ENTITY_SUB_PREDICATE_TYPE, identifier, mapCodec)
        }
    }

    override fun registerMobEffects() {
        CobblemonMobEffects.register { identifier, effect ->
            Registry.register(CobblemonMobEffects.registry, identifier, effect)
        }
    }

    override fun registerResourceReloader(identifier: Identifier, reloader: PreparableReloadListener, type: PackType, dependencies: Collection<Identifier>) {
        ResourceManagerHelper.get(type).registerReloadListener(CobblemonReloadListener(identifier, reloader, dependencies))
    }

    override fun server(): MinecraftServer? = if (this.environment() == Environment.CLIENT) Minecraft.getInstance().singleplayerServer else this.server

    override fun registerCompostable(item: ItemLike, chance: Float) {
        CompostableRegistry.INSTANCE.add(item, chance)
    }

    private class CobblemonReloadListener(private val identifier: Identifier, private val reloader: PreparableReloadListener, private val dependencies: Collection<Identifier>) : IdentifiableResourceReloadListener {

        // port/26.2: reload lost the resource manager and both profilers - listeners read what they need
        // off the shared state instead - and the barrier moved behind the preparation executor.
        override fun reload(currentReload: PreparableReloadListener.SharedState, taskExecutor: Executor, preparationBarrier: PreparableReloadListener.PreparationBarrier, reloadExecutor: Executor): CompletableFuture<Void> =
            this.reloader.reload(currentReload, taskExecutor, preparationBarrier, reloadExecutor)

        override fun prepareSharedState(sharedState: PreparableReloadListener.SharedState) = this.reloader.prepareSharedState(sharedState)

        override fun getFabricId(): Identifier = this.identifier

        override fun getName(): String = this.reloader.name

        override fun getFabricDependencies(): MutableCollection<Identifier> = this.dependencies.toMutableList()
    }

    private class FabricItemGroupInjector(private val output: FabricCreativeModeTabOutput) : CobblemonItemGroups.Injector {
        override fun putFirst(item: ItemLike) {
            this.output.prepend(item)
        }

        override fun putBefore(item: ItemLike, target: ItemLike) {
            this.output.insertBefore(target, item)
        }

        override fun putAfter(item: ItemLike, target: ItemLike) {
            this.output.insertAfter(target, item)
        }

        override fun putLast(item: ItemLike) {
            this.output.accept(item)
        }
    }
}
