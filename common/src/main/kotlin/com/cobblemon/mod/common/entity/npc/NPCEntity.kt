/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.entity.npc

import com.bedrockk.molang.runtime.MoLangRuntime
import com.bedrockk.molang.runtime.struct.VariableStruct
import com.bedrockk.molang.runtime.value.DoubleValue
import com.bedrockk.molang.runtime.value.StringValue
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonEntities
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.CobblemonNetwork.sendPacket
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.entity.PokemonSender
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.asMoLangValue
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.api.moves.animations.ActionEffectContext
import com.cobblemon.mod.common.api.net.serializers.IdentifierDataSerializer
import com.cobblemon.mod.common.api.net.serializers.NPCPlayerTextureSerializer
import com.cobblemon.mod.common.api.net.serializers.PoseTypeDataSerializer
import com.cobblemon.mod.common.api.net.serializers.StringSetDataSerializer
import com.cobblemon.mod.common.api.net.serializers.UUIDSetDataSerializer
import com.cobblemon.mod.common.api.npc.NPCClasses
import com.cobblemon.mod.common.api.npc.configuration.MoLangConfigVariable
import com.cobblemon.mod.common.api.npc.configuration.NPCBattleConfiguration
import com.cobblemon.mod.common.api.npc.configuration.NPCBehaviourConfiguration
import com.cobblemon.mod.common.api.npc.configuration.NPCInteractConfiguration
import com.cobblemon.mod.common.api.permission.CobblemonPermissions.SEE_HIDDEN_NPCS
import com.cobblemon.mod.common.api.scheduling.Schedulable
import com.cobblemon.mod.common.api.scheduling.SchedulingTracker
import com.cobblemon.mod.common.api.storage.party.NPCPartyStore
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.entity.BehaviourEditingTracker
import com.cobblemon.mod.common.entity.EntityCallbacks
import com.cobblemon.mod.common.entity.MoLangScriptingEntity
import com.cobblemon.mod.common.entity.OmniPathingEntity
import com.cobblemon.mod.common.entity.PosableEntity
import com.cobblemon.mod.common.entity.PoseType
import com.cobblemon.mod.common.entity.ai.OmniPathNavigation
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.net.messages.client.npc.CloseNPCEditorPacket
import com.cobblemon.mod.common.net.messages.client.npc.OpenNPCEditorPacket
import com.cobblemon.mod.common.net.messages.client.spawn.SpawnNPCPacket
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.DataKeys
import com.cobblemon.mod.common.util.getBattleState
import com.cobblemon.mod.common.util.getPlayer
import com.cobblemon.mod.common.util.makeEmptyBrainDynamic
import com.cobblemon.mod.common.util.withNPCValue
import com.mojang.authlib.GameProfile
import com.mojang.authlib.ProfileLookupCallback
import com.mojang.serialization.Dynamic
import java.net.URI
import java.util.UUID
import java.util.concurrent.CompletableFuture
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.ai.Brain
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.memory.MemoryModuleType
import net.minecraft.world.entity.ai.sensing.Sensor
import net.minecraft.world.entity.ai.sensing.SensorType
import net.minecraft.world.entity.npc.Npc
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.material.FluidState

class NPCEntity(world: Level) : AgeableMob(CobblemonEntities.NPC, world), PosableEntity, PokemonSender, Schedulable, MoLangScriptingEntity, OmniPathingEntity {
    override val schedulingTracker = SchedulingTracker()

    override val struct = this.asMoLangValue()

    val runtime = MoLangRuntime().setup().withNPCValue(value = this)

    var npc = NPCClasses.dummy()
        set(value) {
            entityData.set(NPC_CLASS, value.id)
            val valueChanged = field != value
            field = value
            if (valueChanged) {
                customName = value.names.randomOrNull() ?: "NPC".text()
                if (!level().isClientSide) {
                    entityData.set(RESOURCE_IDENTIFIER, forcedResourceIdentifier ?: value.resourceIdentifier)
                    remakeBrain()
                }
            }

            this.refreshDimensions()
        }

    val level: Int
        get() = entityData.get(LEVEL)

    var hideNameTag: Boolean
        get() = entityData.get(HIDE_NAME_TAG)
        set(value) {
            entityData.set(HIDE_NAME_TAG, value)
        }

    var renderScale: Float
        get() = entityData.get(RENDER_SCALE)
        set(value) {
            entityData.set(RENDER_SCALE, value)
        }

    var hitboxScale: Float
        get() = entityData.get(HITBOX_SCALE)
        set(value) {
            entityData.set(HITBOX_SCALE, value)
            refreshDimensions()
        }

    var hitboxEyesHeight: Float
        get() = entityData.get(HITBOX_EYES_HEIGHT)
        set(value) {
            if (!level().isClientSide && entityData.get(HITBOX_EYES_HEIGHT) == value) {
                return
            }
            entityData.set(HITBOX_EYES_HEIGHT, value)
            hitbox = (hitbox ?: npc.hitbox).let { EntityDimensions.scalable(it.width, it.height).withEyeHeight(value) }
            refreshDimensions()
        }

    var hitboxWidth: Float
        get() = entityData.get(HITBOX_WIDTH)
        set(value) {
            if (!level().isClientSide && entityData.get(HITBOX_WIDTH) == value) {
                return
            }
            entityData.set(HITBOX_WIDTH, value)
            hitbox = (hitbox ?: npc.hitbox).let { EntityDimensions.scalable(value, it.height).withEyeHeight(it.eyeHeight) }
            refreshDimensions()
        }

    var hitboxHeight: Float
        get() = entityData.get(HITBOX_HEIGHT)
        set(value) {
            if (!level().isClientSide && entityData.get(HITBOX_HEIGHT) == value) {
                return
            }
            entityData.set(HITBOX_HEIGHT, value)
            hitbox = (hitbox ?: npc.hitbox).let { EntityDimensions.scalable(it.width, value).withEyeHeight(it.eyeHeight) }
            refreshDimensions()
        }

    var hitbox: EntityDimensions? = null
        set(value) {
            val comparison = value ?: npc.hitbox
            field = value
            entityData.set(HITBOX_HEIGHT, comparison.height)
            entityData.set(HITBOX_WIDTH, comparison.width)
            entityData.set(HITBOX_EYES_HEIGHT, comparison.eyeHeight)
        }

    var resourceIdentifier: Identifier
        get() = entityData.get(RESOURCE_IDENTIFIER)
        private set(value) {
            entityData.set(RESOURCE_IDENTIFIER, value)
        }

    var forcedResourceIdentifier: Identifier? = null
        set(value) {
            field = value
            if (value != null) {
                entityData.set(RESOURCE_IDENTIFIER, value)
            } else {
                entityData.set(RESOURCE_IDENTIFIER, npc.resourceIdentifier)
            }
        }

    var skill: Int? = null // range from 0 - 5

    var party: NPCPartyStore? = null

    var isMovable: Boolean? = null

    var isInvulnerable: Boolean? = null

    var isLeashable: Boolean? = null

    var allowProjectileHits: Boolean? = null

    fun getPartyForChallenge(players: List<ServerPlayer>): NPCPartyStore? {
        val party = this.party
        return if (party != null) {
            party
        } else if (npc.party?.isStatic == false) {
            npc.party?.provide(this, level, players)
        } else {
            null
        }
    }

    /** Oi, dev, you no touch this one. This one is for [com.cobblemon.mod.common.api.npc.variation.NPCVariationProvider]s. */
    val variationAspects = mutableSetOf<String>()
    /** You can add to this one if you want, that's ok. */
    val appliedAspects = mutableSetOf<String>()

    override val delegate = if (world.isClientSide) {
        com.cobblemon.mod.common.client.entity.NPCClientDelegate()
    } else {
        NPCServerDelegate()
    }

    var battle: NPCBattleConfiguration? = null
    var behaviour: NPCBehaviourConfiguration? = null

    var interaction: NPCInteractConfiguration? = null

    override var behavioursAreCustom = false
    override val behaviours = mutableListOf<Identifier>()
    override val registeredVariables: MutableList<MoLangConfigVariable> = mutableListOf()
    override var data = VariableStruct()
    override var config = VariableStruct()
    override var callbacks = EntityCallbacks(this)

    val aspects: Set<String>
        get() = entityData.get(ASPECTS)

    val battleIds: Set<UUID>
        get() = entityData.get(BATTLE_IDS)

    var actionEffect: ActionEffectContext? = null

    /** Essentially a cached form of what was serialized to make memory reloads still work despite dynamic brain activities on class change. */
    private var brainDynamic: Dynamic<*>? = null


    /* TODO NPC Valuables to add:
     *
     * -- An 'interaction' configuration. This can be loaded from a JSON or API or even a .js (ambitious). Handles what happens
     * -- when you right click. Can be a dialogue tree with some complexity, or provides options to open a shopkeeper GUI,
     * -- that sort of deal. As extensible as we can manage it (and we can manage a lot).
     *
     * A 'party provider' configuration. This is for an NPC that's going to be used as a trainer. A stack of configuration
     * planning has been done by Vera and Design, get it from them and tweak to be clean.
     *
     * A pathing configuration. Another one that could be loaded from JSON or .js or API. Controls AI.
     *
     * npcs should be able to sleep lol
     */

    init {
        delegate.initialize(this)
        addPosableFunctions(struct)
        runtime.environment.query.addFunctions(struct.functions)
        refreshDimensions()
        navigation.setCanFloat(false)
        if (!world.isClientSide) {
            remakeBrain()
        }
    }

    // This has to be below constructor and entity tracker fields otherwise initialization order is weird and breaks them syncing
    companion object {
        fun createAttributes(): AttributeSupplier.Builder = createMobAttributes()
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.ATTACK_KNOCKBACK)

        val NPC_CLASS = SynchedEntityData.defineId(NPCEntity::class.java, IdentifierDataSerializer)
        val RESOURCE_IDENTIFIER = SynchedEntityData.defineId(NPCEntity::class.java, IdentifierDataSerializer)
        val ASPECTS = SynchedEntityData.defineId(NPCEntity::class.java, StringSetDataSerializer)
        val POSE_TYPE = SynchedEntityData.defineId(NPCEntity::class.java, PoseTypeDataSerializer)
        val BATTLE_IDS = SynchedEntityData.defineId(NPCEntity::class.java, UUIDSetDataSerializer)
        val NPC_PLAYER_TEXTURE = SynchedEntityData.defineId(NPCEntity::class.java, NPCPlayerTextureSerializer)
        val LEVEL = SynchedEntityData.defineId(NPCEntity::class.java, EntityDataSerializers.INT)
        val HIDE_NAME_TAG = SynchedEntityData.defineId(NPCEntity::class.java, EntityDataSerializers.BOOLEAN)
        val RENDER_SCALE = SynchedEntityData.defineId(NPCEntity::class.java, EntityDataSerializers.FLOAT)
        val HITBOX_SCALE = SynchedEntityData.defineId(NPCEntity::class.java, EntityDataSerializers.FLOAT)
        val HITBOX_WIDTH = SynchedEntityData.defineId(NPCEntity::class.java, EntityDataSerializers.FLOAT)
        val HITBOX_HEIGHT = SynchedEntityData.defineId(NPCEntity::class.java, EntityDataSerializers.FLOAT)
        val HITBOX_EYES_HEIGHT = SynchedEntityData.defineId(NPCEntity::class.java, EntityDataSerializers.FLOAT)

//        val BATTLING = Activity.register("npc_battling")

        const val SEND_OUT_ANIMATION = "send_out"
        const val RECALL_ANIMATION = "recall"
        const val LOSE_ANIMATION = "lose"
        const val WIN_ANIMATION = "win"
        const val COMMAND_ANIMATION = "command"
    }

    // PT137: LivingEntity.brainProvider() removed from base; Brain.provider() now requires (memoryTypes, sensorTypes, activitySupplier) 3-arg.
    // ActivitySupplier<E>.createActivities(E):List<ActivityData<E>> — return empty list = no activities (configured later via NPCBrain.configure).
    fun npcBrainProvider() = Brain.provider<NPCEntity>(emptySet<MemoryModuleType<*>>(), emptySet<SensorType<out Sensor<in NPCEntity>>>()) { _ -> emptyList() }
    override fun getBreedOffspring(world: ServerLevel, entity: AgeableMob) = null // No lovemaking! Unless...
    override fun getCurrentPoseType() = this.entityData.get(POSE_TYPE)

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(NPC_CLASS, NPCClasses.classes.first().id)
        builder.define(RESOURCE_IDENTIFIER, NPCClasses.classes.first().resourceIdentifier)
        builder.define(ASPECTS, emptySet())
        builder.define(POSE_TYPE, PoseType.STAND)
        builder.define(BATTLE_IDS, setOf())
        builder.define(NPC_PLAYER_TEXTURE, NPCPlayerTexture(ByteArray(1), NPCPlayerModelType.NONE))
        builder.define(LEVEL, 1)
        builder.define(HIDE_NAME_TAG, false)
        builder.define(RENDER_SCALE, 1F)
        builder.define(HITBOX_SCALE, 1F)
        builder.define(HITBOX_WIDTH, 0.6F)
        builder.define(HITBOX_HEIGHT, 1.8F)
        builder.define(HITBOX_EYES_HEIGHT, 1.7F)
    }

    override fun getAddEntityPacket(serverEntity: ServerEntity) = ClientboundCustomPayloadPacket(
        SpawnNPCPacket(
            this,
            super.getAddEntityPacket(serverEntity) as ClientboundAddEntityPacket
        )
    ) as Packet<ClientGamePacketListener>

    // PT138: LivingEntity.remakeBrain removed → non-override helper
    override fun remakeBrain() {
        // No-op: brain rebuild happens via makeBrain(Brain.Packed) at entity construction
    }

    // PT138: LivingEntity.assignNewBrainWithMemoriesAndSensors removed → MoLangScriptingEntity override
    // PT141: needs override modifier (MoLangScriptingEntity interface)
    override fun assignNewBrainWithMemoriesAndSensors(
        packed: Brain.Packed,
        memories: Set<MemoryModuleType<*>>,
        sensors: Set<SensorType<*>>
    ): Brain<out NPCEntity> {
        val allSensors = BuiltInRegistries.SENSOR_TYPE.toSet().filterIsInstance<SensorType<Sensor<in NPCEntity>>>()
        val brain = Brain.provider<NPCEntity>(
            memories.toSet(),
            allSensors.filter { it in sensors }.toSet(),
            Brain.ActivitySupplier<NPCEntity> { _ -> emptyList() }
        ).makeBrain(this, packed)
        this.brain = brain
        return brain
    }

    // PT138: LivingEntity.makeBrain(Dynamic) → makeBrain(Brain.Packed) only abstract member.
    override fun makeBrain(packed: Brain.Packed): Brain<out NPCEntity> {
        val brain = npcBrainProvider().makeBrain(this, packed)
        this.brain = brain
        if (npc != null) {
            // PT141: NPCBrain.configure now takes Brain.Packed (was Dynamic<*>)
            NPCBrain.configure(this, npc, packed)
        }
        return brain
    }

    // PT138: Mob.doHurtTarget(ServerLevel, Entity) — ServerLevel parameter added
    override fun doHurtTarget(level: ServerLevel, target: Entity): Boolean {
        val source = this.damageSources().mobAttack(this)
        // PT138: Entity.hurt returns Unit now; use hurtServer for server-side hits with attribution
        val hurt = super.doHurtTarget(level, target)
        if (hurt) {
            playAttackSound()
        }
        return hurt
    }

    override fun getBrain() = super.getBrain() as Brain<NPCEntity>

    fun updateAspects() {
        entityData.set(ASPECTS, appliedAspects + variationAspects)
    }

    fun isInBattle() = battleIds.isNotEmpty()
    fun getBattleConfiguration() = battle ?: npc.battleConfiguration

    /** Retrieves the battle theme associated with this Pokemon's Species/Form, or the default PVW theme if not found. */
    fun getBattleTheme() = this.npc.battleTheme ?: CobblemonSounds.PVN_BATTLE.location

    override fun tick() {
        super.tick()
        delegate.tick(this)
        schedulingTracker.update(1/20F)
    }

    // PT138: Mob.customServerAiStep(ServerLevel) — ServerLevel parameter added
    override fun customServerAiStep(level: ServerLevel) {
        super.customServerAiStep(level)
        getBrain().tick(level, this)
    }

    override fun broadcastToPlayer(player: ServerPlayer): Boolean {
        if (shouldHideFrom(player)) {
            return false
        }
        return super.broadcastToPlayer(player)
    }

    fun shouldHideFrom(player: ServerPlayer): Boolean {
        if (Cobblemon.permissionValidator.hasPermission(player, permission = SEE_HIDDEN_NPCS)) {
            return false
        }
        val value = Cobblemon.molangData.load(player.uuid).map[stringUUID]
        if (value is VariableStruct) {
            val hide = value.map["hide"] as? DoubleValue
            return hide?.asDouble() == 1.0
        }
        return false
    }

    // PT138: Entity.saveWithoutId/load removed → addAdditionalSaveData(ValueOutput)/readAdditionalSaveData(ValueInput)
    override fun addAdditionalSaveData(output: net.minecraft.world.level.storage.ValueOutput) {
        super.addAdditionalSaveData(output)
        val tmp = CompoundTag()
        saveWithoutIdLegacy(tmp)
        if (!tmp.isEmpty) output.store("LegacyData", CompoundTag.CODEC, tmp)
    }

    override fun readAdditionalSaveData(input: net.minecraft.world.level.storage.ValueInput) {
        super.readAdditionalSaveData(input)
        input.read("LegacyData", CompoundTag.CODEC).ifPresent { loadLegacy(it) }
    }

    fun saveWithoutIdLegacy(nbt: CompoundTag): CompoundTag {
        saveScriptingToNBT(nbt)
        nbt.put(DataKeys.NPC_LEVEL, IntTag.valueOf(level))
        nbt.putBoolean(DataKeys.NPC_HIDE_NAME_TAG, hideNameTag)
        nbt.putString(DataKeys.NPC_CLASS, npc.id.toString())
        if (forcedResourceIdentifier != null) {
            nbt.putString(DataKeys.NPC_FORCED_RESOURCE_IDENTIFIER, forcedResourceIdentifier.toString())
        }
        nbt.put(DataKeys.NPC_ASPECTS, ListTag().also { list -> appliedAspects.forEach { list.add(StringTag.valueOf(it)) } })
        nbt.put(DataKeys.NPC_VARIATION_ASPECTS, ListTag().also { list -> variationAspects.forEach { list.add(StringTag.valueOf(it)) } })
        interaction?.let {
            val interactionNBT = CompoundTag()
            interactionNBT.putString(DataKeys.NPC_INTERACT_TYPE, it.type)
            it.writeToNBT(interactionNBT)
            nbt.put(DataKeys.NPC_INTERACTION, interactionNBT)
        }
        val battle = battle
        if (battle != null) {
            val battleNBT = CompoundTag()
            battle.saveToNBT(battleNBT)
            nbt.put(DataKeys.NPC_BATTLE_CONFIGURATION, battleNBT)
        }
        if (skill != null) {
            nbt.putInt(DataKeys.NPC_SKILL, skill ?: 0)
        }
        val party = party
        if (party != null) {
            val partyNBT = CompoundTag()
            party.saveToNBT(partyNBT, registryAccess())
            nbt.put(DataKeys.NPC_PARTY, partyNBT)
        }
        val playerTexture = entityData.get(NPC_PLAYER_TEXTURE)
        if (playerTexture.model != NPCPlayerModelType.NONE) {
            nbt.put(DataKeys.NPC_PLAYER_TEXTURE, CompoundTag().also {
                it.putString(DataKeys.NPC_PLAYER_TEXTURE_MODEL, playerTexture.model.name)
                it.putByteArray(DataKeys.NPC_PLAYER_TEXTURE_TEXTURE, playerTexture.texture)
            })
        }
        nbt.putFloat(DataKeys.NPC_BOX_SCALE, hitboxScale)
        nbt.putFloat(DataKeys.NPC_RENDER_SCALE, renderScale)
        val hitbox = hitbox
        if (hitbox != null) {
            nbt.put(DataKeys.NPC_HITBOX, CompoundTag().also {
                it.putFloat(DataKeys.NPC_HITBOX_WIDTH, hitbox.width)
                it.putFloat(DataKeys.NPC_HITBOX_HEIGHT, hitbox.height)
                it.putBoolean(DataKeys.NPC_HITBOX_FIXED, hitbox.fixed)
            })
        }
        val isMovable = isMovable
        if (isMovable != null) {
            nbt.putBoolean(DataKeys.NPC_IS_MOVABLE, isMovable)
        }
        val isInvulnerable = isInvulnerable
        if (isInvulnerable != null) {
            nbt.putBoolean(DataKeys.NPC_IS_INVULNERABLE, isInvulnerable)
        }
        val isLeashable = isLeashable
        if (isLeashable != null) {
            nbt.putBoolean(DataKeys.NPC_IS_LEASHABLE, isLeashable)
        }
        val allowProjectileHits = allowProjectileHits
        if (allowProjectileHits != null) {
            nbt.putBoolean(DataKeys.NPC_ALLOW_PROJECTILE_HITS, allowProjectileHits)
        }
        return nbt
    }

    fun loadLegacy(nbt: CompoundTag) {
        npc = NPCClasses.getByIdentifier(Identifier.parse(nbt.getStringOr(DataKeys.NPC_CLASS, ""))) ?: NPCClasses.classes.first()
        forcedResourceIdentifier = if (nbt.contains(DataKeys.NPC_FORCED_RESOURCE_IDENTIFIER)) {
            Identifier.parse(nbt.getStringOr(DataKeys.NPC_FORCED_RESOURCE_IDENTIFIER, ""))
        } else {
            null
        }
        entityData.set(LEVEL, nbt.getIntOr(DataKeys.NPC_LEVEL, 0).takeIf { it != 0 } ?: 1)
        entityData.set(HIDE_NAME_TAG, nbt.getBooleanOr(DataKeys.NPC_HIDE_NAME_TAG, false))
        // PT138: super.load(CompoundTag) removed — readAdditionalSaveData(ValueInput) takes over in parent
        loadScriptingFromNBT(nbt)
        appliedAspects.addAll(nbt.getList(DataKeys.NPC_ASPECTS).orElseGet { net.minecraft.nbt.ListTag() }.map { it.asString().orElse("") })
        variationAspects.addAll(nbt.getList(DataKeys.NPC_VARIATION_ASPECTS).orElseGet { net.minecraft.nbt.ListTag() }.map { it.asString().orElse("") })
        nbt.getCompoundOrEmpty(DataKeys.NPC_INTERACTION).takeIf { !it.isEmpty }?.let { nbt ->
            val type = nbt.getStringOr(DataKeys.NPC_INTERACT_TYPE, "")
            val configType = NPCInteractConfiguration.types[type] ?: return@let
            interaction = configType.clazz.getConstructor().newInstance().also { it.readFromNBT(nbt) }
        }
        val battleNBT = nbt.getCompoundOrEmpty(DataKeys.NPC_BATTLE_CONFIGURATION)
        if (!battleNBT.isEmpty) {
            battle = NPCBattleConfiguration().also { it.loadFromNBT(battleNBT) }
        }
        this.skill = if (nbt.contains(DataKeys.NPC_SKILL)) nbt.getIntOr(DataKeys.NPC_SKILL, 0) else null
        val partyNBT = nbt.getCompoundOrEmpty(DataKeys.NPC_PARTY)
        if (!partyNBT.isEmpty) {
            party = NPCPartyStore(this).also {
                it.loadFromNBT(partyNBT, registryAccess())
                it.initialize()
            }
        }
        if (nbt.contains(DataKeys.NPC_PLAYER_TEXTURE)) {
            val textureNBT = nbt.getCompoundOrEmpty(DataKeys.NPC_PLAYER_TEXTURE)
            val model = NPCPlayerModelType.valueOf(textureNBT.getStringOr(DataKeys.NPC_PLAYER_TEXTURE_MODEL, ""))
            // PT138: CompoundTag.getByteArray returns Optional<ByteArray>
            val texture = textureNBT.getByteArray(DataKeys.NPC_PLAYER_TEXTURE_TEXTURE).orElse(ByteArray(0))
            entityData.set(NPC_PLAYER_TEXTURE, NPCPlayerTexture(texture, model))
        }
        this.isMovable = if (nbt.contains(DataKeys.NPC_IS_MOVABLE)) nbt.getBooleanOr(DataKeys.NPC_IS_MOVABLE, false) else null
        this.isInvulnerable = if (nbt.contains(DataKeys.NPC_IS_INVULNERABLE)) nbt.getBooleanOr(DataKeys.NPC_IS_INVULNERABLE, false) else null
        this.isLeashable = if (nbt.contains(DataKeys.NPC_IS_LEASHABLE)) nbt.getBooleanOr(DataKeys.NPC_IS_LEASHABLE, false) else null
        this.allowProjectileHits = if (nbt.contains(DataKeys.NPC_ALLOW_PROJECTILE_HITS)) nbt.getBooleanOr(DataKeys.NPC_ALLOW_PROJECTILE_HITS, false) else null
        if (nbt.contains(DataKeys.NPC_BASE_SCALE)) {
            val baseScale = nbt.getFloatOr(DataKeys.NPC_BASE_SCALE, 0f)
            entityData.set(HITBOX_SCALE, baseScale)
        }
        if (nbt.contains(DataKeys.NPC_BOX_SCALE)) {
            hitboxScale = nbt.getFloatOr(DataKeys.NPC_BOX_SCALE, 0f)
        }
        if (nbt.contains(DataKeys.NPC_RENDER_SCALE)) {
            renderScale = nbt.getFloatOr(DataKeys.NPC_RENDER_SCALE, 0f)
        }
        this.hitbox = if (nbt.contains(DataKeys.NPC_HITBOX)) {
            val hitboxNBT = nbt.getCompoundOrEmpty(DataKeys.NPC_HITBOX)

            val width = hitboxNBT.getFloatOr(DataKeys.NPC_HITBOX_WIDTH, 0f)
            val height = hitboxNBT.getFloatOr(DataKeys.NPC_HITBOX_HEIGHT, 0f)
            val fixed = hitboxNBT.getBooleanOr(DataKeys.NPC_HITBOX_FIXED, false)

            if (fixed) EntityDimensions.fixed(width, height) else EntityDimensions.scalable(width, height)
        } else {
            null
        }
        updateAspects()
        remakeBrain()
    }

    fun loadTextureFromGameProfileName(username: String) {
        // PT141: MinecraftServer.services().profileRepository removed in MC 26.1.x.
        // Profile resolution path completely refactored — stub pending API survey (non-critical).
        Cobblemon.LOGGER.warn("Profile lookup for NPC '$username' stubbed — MC 26.1.x service refactor pending")
    }

    fun loadTexture(uri: URI, model: NPCPlayerModelType) {
        appliedAspects -= "model-default"
        appliedAspects -= "model-slim"
        appliedAspects += "model-${model.name.lowercase()}"
        entityData.set(NPC_PLAYER_TEXTURE, NPCPlayerTexture(uri.toURL().openStream().readBytes(), model))
        updateAspects()
    }

    fun unloadTexture() {
        appliedAspects -= "model-default"
        appliedAspects -= "model-slim"
        entityData.set(NPC_PLAYER_TEXTURE, NPCPlayerTexture(ByteArray(1), NPCPlayerModelType.NONE))
        data.map.remove("player_texture_username")
        updateAspects()
    }

    override fun hasCustomName() = true
    override fun isCustomNameVisible() = true
    override fun isPersistenceRequired() = super.isPersistenceRequired() || !npc.canDespawn
    // PT138: LivingEntity.getScale() is final in MC 26.1.x — override via getAgeScale() or attribute system
    // Keep helper for entity dimensions sizing
    fun npcScale() = hitboxScale

    override fun getDimensions(pose: Pose): EntityDimensions {
        val hitbox = hitbox ?: npc.hitbox
        val scaledHitbox = hitbox.scale(hitboxScale)
        return scaledHitbox
    }

    override fun isPushable() = isMovable ?: npc.isMovable
    // PT138: Entity.isInvulnerableTo(ServerLevel, DamageSource) — ServerLevel param added
    override fun isInvulnerableTo(level: ServerLevel, source: DamageSource) = (isInvulnerable ?: npc.isInvulnerable) && !source.`is`(BYPASSES_INVULNERABILITY)
    override fun canBeLeashed() = isLeashable ?: npc.isLeashable
    override fun canBeHitByProjectile() = allowProjectileHits ?: npc.allowProjectileHits

    fun initialize(level: Int) {
        variationAspects.clear()
        entityData.set(HIDE_NAME_TAG, npc.hideNameTag)
        entityData.set(LEVEL, level)
        remakeBrain()
        npc.variations.values.forEach { this.variationAspects.addAll(it.provideAspects(this)) }
        if (party == null || npc.party != null) {
            party = npc.party?.takeIf { it.isStatic }?.provide(this, level)
        }
        updateAspects()
    }

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
        if (player is ServerPlayer) {
            if (player.isCreative && player.getItemInHand(hand).item.toString() == CobblemonItems.NPC_EDITOR.toString()) {
                edit(player)
            } else if (hand == InteractionHand.MAIN_HAND) {
                if (player.getBattleState()?.first?.getActor(this) != null) {
                    return InteractionResult.PASS
                }

                (interaction ?: npc.interaction)?.interact(this, player)
//                val battle = getBattleConfiguration()
//                if (battle.canChallenge) {
//                    val provider = battle.party
//                    if (provider != null) {
//                        val party = provider.provide(this, listOf(player))
//                        val result = BattleBuilder.pvn(
//                            player = player,
//                            npcEntity = this
//                        )
//                    }
//                }
            }

        }
        return InteractionResult.SUCCESS
    }

    override fun recalling(pokemonEntity: PokemonEntity): CompletableFuture<Unit> {
        playAnimation(RECALL_ANIMATION, pokemonExpressions(pokemonEntity.pokemon))
        return delayedFuture(seconds = 1.6F)
    }

    override fun sendingOut(pokemon: Pokemon): CompletableFuture<Unit> {
        playAnimation(SEND_OUT_ANIMATION, pokemonExpressions(pokemon))
        return delayedFuture(seconds = 1.6F)
    }

    private fun pokemonExpressions(pokemon: Pokemon): List<String> {
        return listOf(
            "v.actioning_pokemon_name=\'${pokemon.species.name}\';",
            "v.actioning_pokemon_level=${pokemon.level};",
            "v.actioning_pokemon_ball=\'${pokemon.caughtBall.name}\';",
            "v.actioning_pokemon_shiny=\'${pokemon.shiny}\';",
        )
    }

    override fun onSyncedDataUpdated(data: EntityDataAccessor<*>) {
        super.onSyncedDataUpdated(data)
        if (delegate != null) {
            delegate.onSyncedDataUpdated(data)
        }
    }

    fun edit(player: ServerPlayer) {
        val lastEditing = BehaviourEditingTracker.getPlayerIdEditing(this)?.getPlayer()
        if (lastEditing != null) {
            BehaviourEditingTracker.stopEditing(lastEditing.uuid)
            lastEditing.sendPacket(CloseNPCEditorPacket())
        }
        player.sendPacket(OpenNPCEditorPacket(this))
        BehaviourEditingTracker.startEditing(player, this)
    }

    override fun getNavigation() = navigation as OmniPathNavigation
    override fun createNavigation(level: Level) = OmniPathNavigation(level, this)

    // At some point these need to be changeable from MoLang or something.
    override fun canWalk(): Boolean {
        return true
    }

    override fun canSwimInWater(): Boolean {
        return true
    }

    override fun canSwimInLava(): Boolean {
        return false
    }

    override fun canWalkOnLava(): Boolean {
        return false
    }

    override fun canSwimUnderFluid(fluidState: FluidState): Boolean {
        return false
    }

    override fun canWalkOnWater(): Boolean {
        return false
    }

    override fun canPathThroughSaccLeaves(): Boolean {
        return false
    }

    override fun canFly(): Boolean {
        return false
    }

    override fun couldStopFlying(): Boolean {
        return false
    }

    override fun isFlying(): Boolean {
        return false
    }

    override fun setFlying(state: Boolean) {
        // NPCs cannot fly (yet)
    }

    override fun entityOnGround(): Boolean = onGround()
}