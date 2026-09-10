/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client

import com.cobblemon.mod.common.util.hasShiftDown
import net.minecraft.client.renderer.rendertype.RenderTypes

import com.cobblemon.mod.common.Cobblemon.LOGGER
import com.cobblemon.mod.common.CobblemonBlockEntities
import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.CobblemonClientImplementation
import com.cobblemon.mod.common.CobblemonEntities
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.CobblemonMenuType
import com.cobblemon.mod.common.api.berry.Berries
import com.cobblemon.mod.common.api.scheduling.ClientTaskTracker
import com.cobblemon.mod.common.api.storage.player.client.ClientGeneralPlayerData
import com.cobblemon.mod.common.api.storage.player.client.ClientPokedexManager
import com.cobblemon.mod.common.api.storage.player.client.ClientTMMoveManager
import com.cobblemon.mod.common.api.tags.CobblemonItemTags
import com.cobblemon.mod.common.block.entity.TintBlockEntity
import com.cobblemon.mod.common.client.battle.ClientBattle
import com.cobblemon.mod.common.client.gui.PartyOverlay
import com.cobblemon.mod.common.client.gui.PartyOverlayDataControl
import com.cobblemon.mod.common.client.gui.RideControlsOverlay
import com.cobblemon.mod.common.client.gui.battle.BattleOverlay
import com.cobblemon.mod.common.client.gui.cookingpot.CookingPotScreen
import com.cobblemon.mod.common.client.gui.party.PartyTutorialToasts
import com.cobblemon.mod.common.client.gui.tmmachine.TMMachineScreen
import com.cobblemon.mod.common.client.particle.BedrockParticleOptionsRepository
import com.cobblemon.mod.common.client.render.ClientPlayerIcon
import com.cobblemon.mod.common.client.render.DeferredRenderer
import com.cobblemon.mod.common.client.render.block.*
import com.cobblemon.mod.common.client.render.boat.CobblemonBoatRenderer
import com.cobblemon.mod.common.client.render.color.AprijuiceItemColorProvider
import com.cobblemon.mod.common.client.render.color.PokeBaitItemColorProvider
import com.cobblemon.mod.common.client.render.color.PokeSnackItemColorProvider
import com.cobblemon.mod.common.client.render.color.PonigiriItemColorProvider
import com.cobblemon.mod.common.client.render.color.SinisterTeaItemColorProvider
import com.cobblemon.mod.common.client.render.entity.PokeBobberEntityRenderer
import com.cobblemon.mod.common.client.render.generic.GenericBedrockRenderer
import com.cobblemon.mod.common.client.render.item.CobblemonBuiltinItemRendererRegistry
import com.cobblemon.mod.common.client.render.item.PokemonItemRenderer
import com.cobblemon.mod.common.client.render.layer.PokemonOnShoulderRenderer
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockAnimationRepository
import com.cobblemon.mod.common.client.render.models.blockbench.repository.BerryModelRepository
import com.cobblemon.mod.common.client.render.models.blockbench.repository.MiscModelRepository
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.client.render.npc.NPCRenderer
import com.cobblemon.mod.common.client.render.pokeball.PokeBallRenderer
import com.cobblemon.mod.common.client.render.pokemon.PokemonRenderer
import com.cobblemon.mod.common.client.requests.ClientPlayerActionRequests
import com.cobblemon.mod.common.client.sound.BattleMusicController
import com.cobblemon.mod.common.client.sound.EntitySoundTracker
import com.cobblemon.mod.common.client.storage.ClientStorageManager
import com.cobblemon.mod.common.client.tooltips.AprijuiceTooltipGenerator
import com.cobblemon.mod.common.client.tooltips.CobblemonTooltipGenerator
import com.cobblemon.mod.common.client.tooltips.FishingBaitTooltipGenerator
import com.cobblemon.mod.common.client.tooltips.FishingRodTooltipGenerator
import com.cobblemon.mod.common.client.tooltips.PokePuffTooltipGenerator
import com.cobblemon.mod.common.client.tooltips.RecipeSeasoningAbsorptionTooltipGenerator
import com.cobblemon.mod.common.client.tooltips.SeasoningTooltipGenerator
import com.cobblemon.mod.common.client.tooltips.TechnicalMachineTooltipGenerator
import com.cobblemon.mod.common.client.tooltips.TooltipManager
import com.cobblemon.mod.common.client.trade.ClientTrade
import com.cobblemon.mod.common.entity.boat.CobblemonBoatType
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.platform.events.PlatformEvents
import com.cobblemon.mod.common.pokedex.scanner.PokedexUsageContext
import com.cobblemon.mod.common.util.isLookingAt
import net.minecraft.client.Minecraft
import net.minecraft.client.color.block.BlockTintSource
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.model.`object`.boat.BoatModel
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.client.renderer.rendertype.RenderType
// PT145: HangingSignRenderer/StandingSignRenderer require (Context, Models) constructor in MC 26.1.x — registrations commented out below.
// import net.minecraft.client.renderer.blockentity.HangingSignRenderer
// import net.minecraft.client.renderer.blockentity.StandingSignRenderer
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.world.entity.player.PlayerSkin
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB

object CobblemonClient {

    lateinit var implementation: CobblemonClientImplementation
    val storage = ClientStorageManager()
    var trade: ClientTrade? = null
    var battle: ClientBattle? = null
    var clientPlayerData = ClientGeneralPlayerData()
    var clientPokedexData = ClientPokedexManager(mutableMapOf())
    var clientTMMoveData = ClientTMMoveManager(mutableSetOf())

    /** If true then we won't bother them anymore about choosing a starter even if it's a thing they can do. */
    var checkedStarterScreen = false
    var lastPcBoxViewed = 0
    var requests = ClientPlayerActionRequests()
    var teamData = ClientPlayerTeamData()
    val overlay: PartyOverlay by lazy { PartyOverlay() }
    val battleOverlay: BattleOverlay by lazy { BattleOverlay() }
    val pokedexUsageContext: PokedexUsageContext by lazy { PokedexUsageContext() }
    val rideControlsOverlay: RideControlsOverlay by lazy { RideControlsOverlay() }

    fun onLogin() {
        clientPlayerData = ClientGeneralPlayerData()
        requests = ClientPlayerActionRequests()
        teamData = ClientPlayerTeamData()
        clientPokedexData = ClientPokedexManager(mutableMapOf())
        clientTMMoveData = ClientTMMoveManager(mutableSetOf())
        storage.onLogin()
//        CobblemonDataProvider.canReload = false
    }

    fun onLogout() {
        storage.onLogout()
        battle = null
        battleOverlay.onLogout()
        ClientTaskTracker.clear()
        checkedStarterScreen = false
        overlay.resetAttachedToast()
        PartyTutorialToasts.reset()
//        CobblemonDataProvider.canReload = true
        DeferredRenderer.clearAll()
        ClientPlayerIcon.clear()
    }

    fun initialize(implementation: CobblemonClientImplementation) {
        LOGGER.info("Initializing Cobblemon client")
        this.implementation = implementation

        PlatformEvents.CLIENT_PLAYER_LOGIN.subscribe { onLogin() }
        PlatformEvents.CLIENT_PLAYER_LOGOUT.subscribe { onLogout() }

        this.registerBlockEntityRenderers()
        registerBlockRenderTypes()
        registerColors()
        registerFlywheelRenderers()
        this.registerEntityRenderers()
        this.registerItemColors()
        Berries.observable.subscribe {
            BerryModelRepository.patchModels()
        }
        this.registerTooltipManagers()
        this.registerMenuScreens()

        LOGGER.info("Registering custom BuiltinItemRenderers")
        CobblemonBuiltinItemRendererRegistry.register(CobblemonItems.POKEMON_MODEL, PokemonItemRenderer())

        PlatformEvents.CLIENT_ITEM_TOOLTIP.subscribe { event ->
            val stack = event.stack
            val lines = event.lines
            TooltipManager.generateTooltips(stack, lines, hasShiftDown())
        }

        PlatformEvents.CLIENT_ENTITY_UNLOAD.subscribe { event -> EntitySoundTracker.clear(event.entity.id) }
        PlatformEvents.CLIENT_TICK_POST.subscribe { event ->
            val player = event.client.player
            if (player != null) {
                // PT145: Inventory.selected field is private in MC 26.1.x — use Inventory.getSelectedSlot() accessor.
                val selectedItem = player.inventory.getItem(player.inventory.getSelectedSlot())
                if (pokedexUsageContext.scanningGuiOpen &&
                    !(selectedItem.`is`(CobblemonItemTags.POKEDEX)) &&
                    !(player.offhandItem.`is`(CobblemonItemTags.POKEDEX) &&
                            player.isUsingItem &&
                            player.usedItemHand == InteractionHand.OFF_HAND)
                ) {
                    // Stop using Pokédex in main hand if player switches to a different slot in hotbar
                    pokedexUsageContext.stopUsing(PokedexUsageContext.OPEN_SCANNER_BUFFER_TICKS + 1)
                }
                if (event.client.isPaused) {
                    return@subscribe
                }

                val nearbyPokemon = player.level().getEntities(
                    player,
                    AABB.ofSize(player.position(), 16.0, 16.0, 16.0)
                ) { it is PokemonEntity }

                nearbyPokemon?.forEach { entity ->
                    if (entity is PokemonEntity && !entity.isSilent && !entity.passengers.contains(player)) {
                        if (player.isLookingAt(entity) && !player.isSpectator && entity.pokemon.shiny) entity.delegate.spawnShinyParticle(player)
                        entity.delegate.spawnAspectParticle()
                    }
                }
            }
            ClientPlayerIcon.onTick()
            PartyOverlayDataControl.tick(event.client.isPaused)
        }
    }

    private fun registerTooltipManagers() {
        TooltipManager.registerTooltipGenerator(CobblemonTooltipGenerator)
        TooltipManager.registerTooltipGenerator(RecipeSeasoningAbsorptionTooltipGenerator)
        TooltipManager.registerTooltipGenerator(FishingBaitTooltipGenerator)
        TooltipManager.registerTooltipGenerator(SeasoningTooltipGenerator)
        TooltipManager.registerTooltipGenerator(FishingRodTooltipGenerator)
        TooltipManager.registerTooltipGenerator(AprijuiceTooltipGenerator)
        TooltipManager.registerTooltipGenerator(TechnicalMachineTooltipGenerator)
        TooltipManager.registerTooltipGenerator(PokePuffTooltipGenerator)
    }

    fun registerFlywheelRenderers() {
//        InstancedRenderRegistry
//            .configure(CobblemonBlockEntities.BERRY)
//            .alwaysSkipRender()
//            .factory(::BerryEntityInstance)
//            .apply()
    }

    fun registerColors() {
        val tintSource = object : BlockTintSource {
            override fun color(state: net.minecraft.world.level.block.state.BlockState): Int = 0xFFFFFF
            override fun colorInWorld(
                state: net.minecraft.world.level.block.state.BlockState,
                view: net.minecraft.client.renderer.block.BlockAndTintGetter,
                pos: net.minecraft.core.BlockPos
            ): Int {
                val blockEntity = view.getBlockEntity(pos)
                if (blockEntity is TintBlockEntity) return blockEntity.getTint()
                return 0xFFFFFF
            }
        }
        this.implementation.registerBlockColors(tintSource,
            CobblemonBlocks.POKE_SNACK,
            CobblemonBlocks.POKE_CAKE,
            CobblemonBlocks.TM_MACHINE
        )
    }

    private fun registerBlockRenderTypes() {

        // PT145: RenderTypes.cutoutMipped()/cutout() removed in MC 26.1.x — use Sheets.cutoutBlockSheet() as fallback.
        this.implementation.registerBlockRenderType(
            net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
            CobblemonBlocks.APRICORN_LEAVES,
            CobblemonBlocks.SACCHARINE_LEAVES,
            CobblemonBlocks.POKE_CAKE
        )

        this.implementation.registerBlockRenderType(
            net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
            CobblemonBlocks.GILDED_CHEST,
            CobblemonBlocks.FOSSIL_ANALYZER,
            CobblemonBlocks.APRICORN_DOOR,
            CobblemonBlocks.APRICORN_TRAPDOOR,
            CobblemonBlocks.APRICORN_SIGN,
            CobblemonBlocks.APRICORN_WALL_SIGN,
            CobblemonBlocks.APRICORN_HANGING_SIGN,
            CobblemonBlocks.APRICORN_WALL_HANGING_SIGN,
            CobblemonBlocks.BLACK_APRICORN_SAPLING,
            CobblemonBlocks.BLUE_APRICORN_SAPLING,
            CobblemonBlocks.GREEN_APRICORN_SAPLING,
            CobblemonBlocks.PINK_APRICORN_SAPLING,
            CobblemonBlocks.RED_APRICORN_SAPLING,
            CobblemonBlocks.WHITE_APRICORN_SAPLING,
            CobblemonBlocks.YELLOW_APRICORN_SAPLING,
            CobblemonBlocks.POTTED_BLACK_APRICORN_SAPLING,
            CobblemonBlocks.POTTED_BLUE_APRICORN_SAPLING,
            CobblemonBlocks.POTTED_GREEN_APRICORN_SAPLING,
            CobblemonBlocks.POTTED_PINK_APRICORN_SAPLING,
            CobblemonBlocks.POTTED_RED_APRICORN_SAPLING,
            CobblemonBlocks.POTTED_WHITE_APRICORN_SAPLING,
            CobblemonBlocks.POTTED_YELLOW_APRICORN_SAPLING,
            CobblemonBlocks.BLACK_APRICORN,
            CobblemonBlocks.BLUE_APRICORN,
            CobblemonBlocks.GREEN_APRICORN,
            CobblemonBlocks.PINK_APRICORN,
            CobblemonBlocks.RED_APRICORN,
            CobblemonBlocks.WHITE_APRICORN,
            CobblemonBlocks.YELLOW_APRICORN,
            CobblemonBlocks.HEALING_MACHINE,
            CobblemonBlocks.MEDICINAL_LEEK,
            CobblemonBlocks.HEALING_MACHINE,
            CobblemonBlocks.MONITOR,
            CobblemonBlocks.RED_MINT,
            CobblemonBlocks.BLUE_MINT,
            CobblemonBlocks.CYAN_MINT,
            CobblemonBlocks.PINK_MINT,
            CobblemonBlocks.GREEN_MINT,
            CobblemonBlocks.WHITE_MINT,
            CobblemonBlocks.PASTURE,
            CobblemonBlocks.ENERGY_ROOT,
            CobblemonBlocks.BIG_ROOT,
            CobblemonBlocks.REVIVAL_HERB,
            CobblemonBlocks.VIVICHOKE_SEEDS,
            CobblemonBlocks.HEARTY_GRAINS,
            CobblemonBlocks.PEP_UP_FLOWER,
            CobblemonBlocks.POTTED_PEP_UP_FLOWER,
            CobblemonBlocks.REVIVAL_HERB,
            *CobblemonBlocks.berries().values.toTypedArray(),
            CobblemonBlocks.GALARICA_NUT_BUSH,
            CobblemonBlocks.RESTORATION_TANK,
            CobblemonBlocks.SMALL_BUDDING_TUMBLESTONE,
            CobblemonBlocks.MEDIUM_BUDDING_TUMBLESTONE,
            CobblemonBlocks.LARGE_BUDDING_TUMBLESTONE,
            CobblemonBlocks.TUMBLESTONE_CLUSTER,
            CobblemonBlocks.SMALL_BUDDING_BLACK_TUMBLESTONE,
            CobblemonBlocks.MEDIUM_BUDDING_BLACK_TUMBLESTONE,
            CobblemonBlocks.LARGE_BUDDING_BLACK_TUMBLESTONE,
            CobblemonBlocks.BLACK_TUMBLESTONE_CLUSTER,
            CobblemonBlocks.TYPE_GEM_CLUSTER_NORMAL,
            CobblemonBlocks.TYPE_GEM_CLUSTER_FIRE,
            CobblemonBlocks.TYPE_GEM_CLUSTER_WATER,
            CobblemonBlocks.TYPE_GEM_CLUSTER_ELECTRIC,
            CobblemonBlocks.TYPE_GEM_CLUSTER_GRASS,
            CobblemonBlocks.TYPE_GEM_CLUSTER_ICE,
            CobblemonBlocks.TYPE_GEM_CLUSTER_FIGHTING,
            CobblemonBlocks.TYPE_GEM_CLUSTER_POISON,
            CobblemonBlocks.TYPE_GEM_CLUSTER_GROUND,
            CobblemonBlocks.TYPE_GEM_CLUSTER_FLYING,
            CobblemonBlocks.TYPE_GEM_CLUSTER_PSYCHIC,
            CobblemonBlocks.TYPE_GEM_CLUSTER_BUG,
            CobblemonBlocks.TYPE_GEM_CLUSTER_ROCK,
            CobblemonBlocks.TYPE_GEM_CLUSTER_GHOST,
            CobblemonBlocks.TYPE_GEM_CLUSTER_DRAGON,
            CobblemonBlocks.TYPE_GEM_CLUSTER_DARK,
            CobblemonBlocks.TYPE_GEM_CLUSTER_STEEL,
            CobblemonBlocks.TYPE_GEM_CLUSTER_FAIRY,
            CobblemonBlocks.SMALL_BUDDING_SKY_TUMBLESTONE,
            CobblemonBlocks.MEDIUM_BUDDING_SKY_TUMBLESTONE,
            CobblemonBlocks.LARGE_BUDDING_SKY_TUMBLESTONE,
            CobblemonBlocks.SKY_TUMBLESTONE_CLUSTER,
            CobblemonBlocks.GIMMIGHOUL_CHEST,
            CobblemonBlocks.DISPLAY_CASE,
            CobblemonBlocks.SACCHARINE_DOOR,
            CobblemonBlocks.SACCHARINE_TRAPDOOR,
            CobblemonBlocks.SACCHARINE_SIGN,
            CobblemonBlocks.SACCHARINE_WALL_SIGN,
            CobblemonBlocks.SACCHARINE_HANGING_SIGN,
            CobblemonBlocks.SACCHARINE_WALL_HANGING_SIGN,
            CobblemonBlocks.SACCHARINE_SAPLING,
            CobblemonBlocks.POTTED_SACCHARINE_SAPLING,
            CobblemonBlocks.POKE_SNACK,
            CobblemonBlocks.LECTERN,
            CobblemonBlocks.CAMPFIRE,
            CobblemonBlocks.SOUL_CAMPFIRE,
            CobblemonBlocks.BLACK_CAMPFIRE_POT,
            CobblemonBlocks.BLUE_CAMPFIRE_POT,
            CobblemonBlocks.GREEN_CAMPFIRE_POT,
            CobblemonBlocks.PINK_CAMPFIRE_POT,
            CobblemonBlocks.RED_CAMPFIRE_POT,
            CobblemonBlocks.WHITE_CAMPFIRE_POT,
            CobblemonBlocks.YELLOW_CAMPFIRE_POT,
            CobblemonBlocks.WHITE_PLAQUE,
            CobblemonBlocks.LIGHT_GRAY_PLAQUE,
            CobblemonBlocks.GRAY_PLAQUE,
            CobblemonBlocks.BLACK_PLAQUE,
            CobblemonBlocks.BROWN_PLAQUE,
            CobblemonBlocks.RED_PLAQUE,
            CobblemonBlocks.ORANGE_PLAQUE,
            CobblemonBlocks.YELLOW_PLAQUE,
            CobblemonBlocks.LIME_PLAQUE,
            CobblemonBlocks.GREEN_PLAQUE,
            CobblemonBlocks.CYAN_PLAQUE,
            CobblemonBlocks.LIGHT_BLUE_PLAQUE,
            CobblemonBlocks.BLUE_PLAQUE,
            CobblemonBlocks.PURPLE_PLAQUE,
            CobblemonBlocks.MAGENTA_PLAQUE,
            CobblemonBlocks.PINK_PLAQUE,
            CobblemonBlocks.BLUNDER_POLICY,
            CobblemonBlocks.WEAKNESS_POLICY,
            CobblemonBlocks.POTION,
            CobblemonBlocks.SUPER_POTION,
            CobblemonBlocks.HYPER_POTION,
            CobblemonBlocks.MAX_POTION,
            CobblemonBlocks.FULL_RESTORE,
            CobblemonBlocks.ANTIDOTE,
            CobblemonBlocks.AWAKENING,
            CobblemonBlocks.BURN_HEAL,
            CobblemonBlocks.ICE_HEAL,
            CobblemonBlocks.PARALYZE_HEAL,
            CobblemonBlocks.FULL_HEAL,
            CobblemonBlocks.ETHER,
            CobblemonBlocks.MAX_ETHER,
            CobblemonBlocks.ELIXIR,
            CobblemonBlocks.MAX_ELIXIR,
            CobblemonBlocks.DIRE_HIT,
            CobblemonBlocks.GUARD_SPEC,
            CobblemonBlocks.X_ACCURACY,
            CobblemonBlocks.X_ATTACK,
            CobblemonBlocks.X_DEFENSE,
            CobblemonBlocks.X_SP_ATK,
            CobblemonBlocks.X_SP_DEF,
            CobblemonBlocks.X_SPEED,
            CobblemonBlocks.TM_MACHINE
        )

        this.createBoatModelLayers()
    }

    fun beforeChatRender(context: GuiGraphicsExtractor, partialDeltaTicks: Float) {
        // PT132: Minecraft.timer → deltaTracker (DeltaTracker) in MC 26.1; overlay.render → extractRenderState
        val deltaTracker = Minecraft.getInstance().deltaTracker
//        ClientTaskTracker.update(partialDeltaTicks / 20f)
        if (battle == null) {
            overlay.extractRenderState(context, deltaTracker)
        } else {
            battleOverlay.extractRenderState(context, deltaTracker)
        }
        rideControlsOverlay.extractRenderState(context, deltaTracker)
    }

    // PT145: PlayerRenderer.addLayer and PlayerModel typing changed in MC 26.1.x — entire shoulder-layer wiring deferred until submit pipeline migration.
    fun onAddLayer(skinMap: Map<net.minecraft.world.entity.player.PlayerModelType, Any>?) {
        // No-op: PokemonOnShoulderRenderer integration is deferred (see PT145 patch series).
        @Suppress("UNUSED_PARAMETER")
        val _unused = skinMap
    }

    private fun registerMenuScreens() {
        MenuScreens.register(CobblemonMenuType.COOKING_POT, ::CookingPotScreen)
        MenuScreens.register(CobblemonMenuType.TM_MACHINE, ::TMMachineScreen)
    }

    private fun registerBlockEntityRenderers() {
        this.implementation.registerBlockEntityRenderer(
            CobblemonBlockEntities.HEALING_MACHINE,
            ::HealingMachineRenderer
        )
        this.implementation.registerBlockEntityRenderer(CobblemonBlockEntities.BERRY, ::BerryBlockRenderer)
        // PT145: StandingSignRenderer/HangingSignRenderer take (Context, Models) in MC 26.1.x — sign renderer registration deferred.
        // this.implementation.registerBlockEntityRenderer(CobblemonBlockEntities.SIGN, ::StandingSignRenderer)
        // this.implementation.registerBlockEntityRenderer(CobblemonBlockEntities.HANGING_SIGN, ::HangingSignRenderer)
        this.implementation.registerBlockEntityRenderer(
            CobblemonBlockEntities.FOSSIL_ANALYZER,
            ::FossilAnalyzerRenderer
        )
        this.implementation.registerBlockEntityRenderer(
            CobblemonBlockEntities.RESTORATION_TANK,
            ::RestorationTankRenderer
        )
        this.implementation.registerBlockEntityRenderer(CobblemonBlockEntities.HABITAT_BLOCK, ::HabitatBlockRenderer)
        this.implementation.registerBlockEntityRenderer(CobblemonBlockEntities.GILDED_CHEST, ::GildedChestBlockRenderer)
        this.implementation.registerBlockEntityRenderer(CobblemonBlockEntities.DISPLAY_CASE, ::DisplayCaseRenderer)
        this.implementation.registerBlockEntityRenderer(CobblemonBlockEntities.LECTERN, ::LecternBlockEntityRenderer)
        this.implementation.registerBlockEntityRenderer(CobblemonBlockEntities.CAMPFIRE, ::CampfireBlockEntityRenderer)
        this.implementation.registerBlockEntityRenderer(CobblemonBlockEntities.POKE_SNACK, ::PokeSnackBlockEntityRenderer)
        this.implementation.registerBlockEntityRenderer(CobblemonBlockEntities.DISC_SHELF, ::DiscShelfBlockEntityRenderer)
    }

    private fun registerEntityRenderers() {
        LOGGER.info("Registering Pokémon renderer")
        this.implementation.registerEntityRenderer(CobblemonEntities.POKEMON, ::PokemonRenderer)
        LOGGER.info("Registering PokéBall renderer")
        this.implementation.registerEntityRenderer(CobblemonEntities.EMPTY_POKEBALL, ::PokeBallRenderer)
        LOGGER.info("Registering Boat renderer")
        this.implementation.registerEntityRenderer(CobblemonEntities.BOAT) { ctx ->
            CobblemonBoatRenderer(
                ctx,
                true
            )
        }
        LOGGER.info("Registering Boat with Chest renderer")
        this.implementation.registerEntityRenderer(CobblemonEntities.CHEST_BOAT) { ctx -> CobblemonBoatRenderer(ctx, true) }
        LOGGER.info("Registering Generic Bedrock renderer")
        this.implementation.registerEntityRenderer(CobblemonEntities.GENERIC_BEDROCK_ENTITY, ::GenericBedrockRenderer)
        LOGGER.info("Registering Generic Bedrock Entity renderer")
        this.implementation.registerEntityRenderer(CobblemonEntities.GENERIC_BEDROCK_ENTITY, ::GenericBedrockRenderer)
        LOGGER.info("Registering PokeRod Bobber renderer")
        this.implementation.registerEntityRenderer(CobblemonEntities.POKE_BOBBER) { ctx -> PokeBobberEntityRenderer(ctx) }
        LOGGER.info("Registering NPC renderer")
        this.implementation.registerEntityRenderer(CobblemonEntities.NPC, ::NPCRenderer)
    }

    private fun registerItemColors() {
        implementation.registerItemColors(AprijuiceItemColorProvider, *CobblemonItems.aprijuices.toTypedArray())
        implementation.registerItemColors(PokeSnackItemColorProvider, CobblemonItems.POKE_SNACK, CobblemonItems.POKE_CAKE)
        implementation.registerItemColors(PokeBaitItemColorProvider, CobblemonItems.POKE_BAIT)
        implementation.registerItemColors(PonigiriItemColorProvider, CobblemonItems.PONIGIRI)
        implementation.registerItemColors(SinisterTeaItemColorProvider, CobblemonItems.SINISTER_TEA)
    }

    fun reloadCodedAssets(resourceManager: ResourceManager) {
        LOGGER.info("Loading assets...")
        // Particles come first because animations need them.
        BedrockParticleOptionsRepository.loadEffects(resourceManager)
        // Animations come next because models need them.
        BedrockAnimationRepository.loadAnimations(
            resourceManager = resourceManager,
            directories = VaryingModelRepository.animationDirectories
        )
        VaryingModelRepository.reload(resourceManager)

        BerryModelRepository.reload(resourceManager)
        MiscModelRepository.reload(resourceManager)
        LOGGER.info("Loaded assets")
    }

    fun endBattle() {
        battle = null
        battleOverlay.lastKnownBattle = null
        BattleMusicController.endMusic()
    }

    private fun createBoatModelLayers() {
        CobblemonBoatType.entries.forEach { type ->
            this.implementation.registerLayer(
                CobblemonBoatRenderer.createBoatModelLayer(type, false),
                BoatModel::createBoatModel
            )
            this.implementation.registerLayer(
                CobblemonBoatRenderer.createBoatModelLayer(type, true),
                BoatModel::createChestBoatModel
            )
        }
    }
}
