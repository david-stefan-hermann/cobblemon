/*
 * Copyright (C) 2023 Cobblemon Contributors
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.cobblemon.mod.common.client.render.pokemon

import net.minecraft.client.renderer.rendertype.RenderTypes

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.OrientationControllable
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.client.battle.ClientBallDisplay
import com.cobblemon.mod.common.client.entity.NPCClientDelegate
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate.Companion.BEAM_EXTEND_TIME
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate.Companion.BEAM_SHRINK_TIME
import com.cobblemon.mod.common.client.keybind.boundKey
import com.cobblemon.mod.common.client.keybind.keybinds.PartySendBinding
import com.cobblemon.mod.common.client.render.item.HeldItemRenderer
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState
import com.cobblemon.mod.common.client.render.models.blockbench.pokemon.PosablePokemonEntityModel
import com.cobblemon.mod.common.client.render.models.blockbench.repository.MiscModelRepository
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository
import com.cobblemon.mod.common.client.render.pokeball.PokeBallPosableState
import com.cobblemon.mod.common.client.render.renderBeaconBeam
import com.cobblemon.mod.common.client.settings.ServerSettings
import com.cobblemon.mod.common.entity.PlatformType
import com.cobblemon.mod.common.entity.PosableEntity
import com.cobblemon.mod.common.entity.npc.NPCEntity
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity.Companion.SPAWN_DIRECTION
import com.cobblemon.mod.common.pokeball.PokeBall
import com.cobblemon.mod.common.util.effectiveName
import com.cobblemon.mod.common.util.isLookingAt
import com.cobblemon.mod.common.util.lang
import com.cobblemon.mod.common.util.math.DoubleRange
import com.cobblemon.mod.common.util.math.geometry.toRadians
import com.cobblemon.mod.common.util.math.remap
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font.DisplayMode
import com.cobblemon.mod.common.client.render.submitPosableModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import org.joml.*
import kotlin.math.*

/**
 * port/26.2: entity rendering is two-phase. Cobblemon's drawing needs the live entity - its client
 * delegate carries the animation clock, pose state and beam progress, none of which fits in a render
 * state - so the entity is carried on the state itself rather than in a field on the renderer.
 */
class PokemonRenderState : LivingEntityRenderState() {
    var entity: PokemonEntity? = null
    var partialTicks: Float = 0F
}

class PokemonRenderer(
    context: EntityRendererProvider.Context
) : MobRenderer<PokemonEntity, PokemonRenderState, PosablePokemonEntityModel>(context, PosablePokemonEntityModel(), 0.5f) {
    companion object {
        val recallBeamColour = Vector4f(1F, 0.1F, 0.1F, 1F)
        fun ease(x: Double): Double {
            return 1 - (1 - x).pow(3)
        }
        private val LEVEL_LABEL_STYLE = Style.EMPTY.withColor(ChatFormatting.WHITE)
            .withBold(false)
            .withItalic(false)
            .withUnderlined(false)
            .withStrikethrough(false)
            .withObfuscated(false)

        private const val HIDDEN_NAME = "???"

        private const val SPACE = " "
    }

    val ballContext = RenderContext().also {
        it.put(RenderContext.RENDER_STATE, RenderContext.RenderState.WORLD)
    }

    private val heldItemRenderer = HeldItemRenderer()

    override fun createRenderState(): PokemonRenderState = PokemonRenderState()

    override fun getTextureLocation(state: PokemonRenderState): Identifier {
        return state.entity?.let { getTextureLocationForEntity(it) }
            ?: com.cobblemon.mod.common.util.cobblemonResource("textures/entity/missing.png")
    }

    override fun extractRenderState(entity: PokemonEntity, state: PokemonRenderState, partialTick: Float) {
        super.extractRenderState(entity, state, partialTick)
        state.entity = entity
        state.partialTicks = partialTick
    }

    override fun submit(
        state: PokemonRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val entity = state.entity ?: return
        currentRenderState = state
        render(entity, state.bodyRot, state.partialTicks, poseStack, collector, state.lightCoords)
    }

    /** The state currently being submitted, so the model draw can read the walk/age values off it. */
    private var currentRenderState: PokemonRenderState? = null

    /**
     * port/26.2: replaces the LivingEntityRenderer.render call this used to delegate to. Vanilla's own
     * submission would draw the wrapper model's empty root - Cobblemon's geometry hangs off
     * posableModel - so the transforms vanilla applied are reproduced here and the real model is handed
     * to the collector.
     */
    private fun submitPokemonModel(
        entity: PokemonEntity,
        entityYaw: Float,
        partialTicks: Float,
        poseMatrix: PoseStack,
        collector: SubmitNodeCollector,
        packedLight: Int
    ) {
        val state = currentRenderState
        model.setupAnim(
            entity,
            state?.walkAnimationPos ?: 0F,
            state?.walkAnimationSpeed ?: 0F,
            state?.ageInTicks ?: 0F,
            entityYaw,
            state?.xRot ?: 0F
        )

        poseMatrix.pushPose()
        poseMatrix.mulPose(Axis.YP.rotationDegrees(180F - entityYaw))
        poseMatrix.scale(-1F, -1F, 1F)
        applyEntityScale(entity, poseMatrix, partialTicks)
        // Undone inside PosableEntityModel.renderToBufferLegacy, which re-adds 1.5 for living entities.
        poseMatrix.translate(0.0, -1.501, 0.0)

        val texture = getTextureLocationForEntity(entity)
        collector.submitPosableModel(poseMatrix, RenderTypes.entityCutout(texture)) { stack, consumer ->
            model.renderToBufferLegacy(stack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -0x1)
        }
        poseMatrix.popPose()
    }

    fun getTextureLocationForEntity(entity: PokemonEntity): Identifier {
        return VaryingModelRepository.getTexture(entity.pokemon.species.resourceIdentifier, entity.delegate as PokemonClientDelegate)
    }

    // PT145: render() removed in MC 26.1.x — kept as helper for legacy callers; new submit pipeline pending.
    fun render(
        entity: PokemonEntity,
        entityYaw: Float,
        partialTicks: Float,
        poseMatrix: PoseStack,
        buffer: SubmitNodeCollector,
        packedLight: Int
    ) {
        val clientDelegate = entity.delegate as PokemonClientDelegate
        // PT134-DEFER: shadowRadius moved to EntityRenderState in MC 26.1.x — set in extractRenderState
        // shadowRadius = (min((entity.boundingBox.maxX - entity.boundingBox.minX), (entity.boundingBox.maxZ) - (entity.boundingBox.minZ)).toFloat() / 1.5F * (entity.delegate as PokemonClientDelegate).activeSendoutScale)/entity.scale
        model.posableModel = VaryingModelRepository.getPoser(entity.pokemon.species.resourceIdentifier, clientDelegate)
        model.posableModel.context = model.context
        model.setupEntityTypeContext(entity)
        val modelNow = model.posableModel

        val freezeFrame = entity.entityData.get(PokemonEntity.FREEZE_FRAME)
        if (freezeFrame != -1F) {
            clientDelegate.updateAge(0)
            clientDelegate.updatePartialTicks(freezeFrame * 20F)
        } else {
            clientDelegate.updatePartialTicks(partialTicks)
        }

        if (entity.beamMode != 0 && !Minecraft.getInstance().isPaused) {
            renderTransition(
                modelNow,
                entity.beamMode,
                entity,
                partialTicks,
                poseMatrix,
                buffer,
                packedLight,
                clientDelegate
            )
        }
        if (entity.platform != PlatformType.NONE) {
            drawPlatform(
                poseMatrix,
                entity,
                (entity.delegate as PokemonClientDelegate).activeSendoutScale,
                buffer,
                packedLight,
            )
            // keeps the pokemon's root on the raft
            poseMatrix.translate(0.0, 0.25 * (entity.delegate as PokemonClientDelegate).activeSendoutScale, 0.0)
        }

        modelNow.setLayerContext(buffer, clientDelegate, VaryingModelRepository.getLayers(entity.pokemon.species.resourceIdentifier, clientDelegate))


        if (entity.passengers.isNotEmpty()) {
            renderRiding(entity, entityYaw, partialTicks, poseMatrix, buffer, packedLight)
        } else {
            submitPokemonModel(entity, entityYaw, partialTicks, poseMatrix, buffer, packedLight)
        }

        // Call rendering for alpha eye trail
        doAlphaEyeRendering(
            entity = entity,
            partialTicks = partialTicks,
            poseStack = poseMatrix,
            bufferSource = buffer
        )

        modelNow.green = 1F
        modelNow.blue = 1F
        modelNow.resetLayerContext()
        if (this.shouldRenderLabel(entity)) {
            this.renderNameTag(entity, entity.effectiveName(), poseMatrix, buffer, packedLight, partialTicks)
        }
//        Minecraft.getInstance().bufferBuilders.entityVertexConsumers.draw()

        //Render Held Item
        heldItemRenderer.renderOnModel(
            entity.shownItem,
            clientDelegate,
            poseMatrix,
            buffer,
            packedLight,
            false,
            entity
        )
    }

    fun renderRiding(
        entity: PokemonEntity,
        entityYaw: Float,
        partialTicks: Float,
        poseMatrix: PoseStack,
        buffer: SubmitNodeCollector,
        packedLight: Int
    ) {
        val rollable = entity as? OrientationControllable ?: return
        val controller = rollable.orientationController
        poseMatrix.pushPose()

        if (controller.active) {
            // Allow the ride controller to modify its rotations using partialTick
            entity.delegate.applyRenderRotation(partialTicks)

            val matrix = poseMatrix.last().pose()
            val yaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot)
            val center = Vector3f(0f, entity.bbHeight/2, 0f)
            val transformationMatrix = Matrix4f()
            //Move origin to center of Pokemon
            transformationMatrix.translate(center)

            transformationMatrix.rotate(controller.getRenderOrientation(partialTicks))
            //Move origin to base of the entity
            transformationMatrix.translate(center.negate(Vector3f()))

            //Pre-undo yaw rotation
            transformationMatrix.rotate(Axis.YP.rotationDegrees(yaw+180f))
            matrix.mul(transformationMatrix)
        }

        submitPokemonModel(entity, entityYaw, partialTicks, poseMatrix, buffer, packedLight)
        // super.render(entity, entityYaw, partialTicks, poseMatrix, buffer, packedLight)
        poseMatrix.popPose()
    }

    fun renderTransition(
        modelNow: PosableModel,
        beamMode: Int,
        entity: PokemonEntity,
        partialTicks: Float,
        poseMatrix: PoseStack,
        buffer: SubmitNodeCollector,
        packedLight: Int,
        clientDelegate: PokemonClientDelegate
    ) {
        val s = clientDelegate.secondsSinceBeamEffectStarted
        if (beamMode == 3) {
            if (s > BEAM_EXTEND_TIME) {
                val value = (s - BEAM_EXTEND_TIME) /  BEAM_SHRINK_TIME
                val colourValue = 1F - min(0.6F, value)
                modelNow.green = colourValue
                modelNow.blue = colourValue
            }
        }

        val phaseTarget = clientDelegate.phaseTarget ?: return
        poseMatrix.pushPose()
        var beamSourcePosition = if (phaseTarget is NPCEntity) {
                val npcDelegate = phaseTarget.delegate as NPCClientDelegate
                val baseScale = phaseTarget.renderScale.toDouble()

                (npcDelegate.locatorStates["beam"]?.getOrigin()?.scale(baseScale))
                    ?: phaseTarget.position().add(0.0, (phaseTarget.bbHeight / 2.0) * baseScale, 0.0)
                } else if (phaseTarget is PosableEntity) {
                    (phaseTarget.delegate as PosableState).locatorStates["beam"]?.getOrigin() ?: phaseTarget.position()
        } else {
            if (phaseTarget.uuid == Minecraft.getInstance().player?.uuid) {
                val lookVec = phaseTarget.lookAngle.yRot((PI / 2).toFloat()).multiply(1.0, 0.0, 1.0).normalize()
                phaseTarget.getEyePosition(partialTicks).subtract(0.0, 0.4, 0.0).subtract(lookVec.scale(0.3))
            } else {
                val lookVec = phaseTarget.lookAngle.yRot((PI / 2 - (phaseTarget.visualRotationYInDegrees - phaseTarget.xRot).toRadians()).toFloat()).multiply(1.0, 0.0, 1.0).normalize()
                phaseTarget.getEyePosition(partialTicks).subtract(0.0, 0.7, 0.0).subtract(lookVec.scale(0.4))
            }
        }

        if (clientDelegate.sendOutPosition == null && beamMode == 1) {
            clientDelegate.sendOutPosition = beamSourcePosition
        } else if (beamMode == 1) {
            clientDelegate.sendOutPosition = clientDelegate.sendOutPosition!!.add(0.0, 0.04, 0.0)
            beamSourcePosition = clientDelegate.sendOutPosition!!
        }
        val offsetDirection = beamSourcePosition.subtract(entity.position()).normalize().scale(-clientDelegate.ballOffset.toDouble())
        val facingDir: Vec3
        with(beamSourcePosition.subtract(entity.position())) {
            var newOffset = offsetDirection.scale(2.0)
            val distance = beamSourcePosition.distanceTo(entity.position())
            newOffset = newOffset.scale((distance / 10.0) * 5)
            facingDir = newOffset.normalize()
            clientDelegate.sendOutOffset = newOffset
            poseMatrix.translate(x+newOffset.x, y+newOffset.y, z+newOffset.z)
        }
        val dir = beamSourcePosition.subtract(entity.position()).normalize()
        val angle = Mth.atan2(dir.z, dir.x) - PI / 2
        poseMatrix.mulPose(Axis.YP.rotation(-angle.toFloat() + (180 * Math.PI / 180).toFloat()))

        if (beamMode == 1 && !clientDelegate.ballDone){
            if(entity.pokemon.caughtBall.name.toString().contains("beast")){
                // get rotation angle on x-axis for facingDir
                val xAngleFacingDir = Mth.atan2(facingDir.y, sqrt(facingDir.x * facingDir.x + facingDir.z * facingDir.z))
                poseMatrix.mulPose(Axis.XP.rotation(-xAngleFacingDir.toFloat()))
            }
            drawPokeBall(
                ClientBallDisplay(entity.pokemon.caughtBall, setOf()),
                poseMatrix,
                scale = clientDelegate.ballOffset,
                partialTicks = partialTicks,
                buff = buffer,
                packedLight = packedLight,
                ball = CobblemonClient.storage.party.firstOrNull { it?.uuid == entity.pokemon.uuid }?.caughtBall
                    ?: clientDelegate.currentEntity.pokemon.caughtBall,
                distance = ceil(beamSourcePosition.distanceTo(entity.position())/4f).toInt()
            )
        }
        poseMatrix.popPose()
        if (beamMode == 3) {
            renderBeam(poseMatrix, partialTicks, entity, phaseTarget, buffer, offsetDirection)
        }
    }

    /** port/26.2: LivingEntityRenderer.scale is state-based now; this stays entity-based and is applied by hand. */
    fun applyEntityScale(pEntity: PokemonEntity, pPoseStack: PoseStack, pPartialTickTime: Float) {
        val scale = pEntity.pokemon.form.baseScale * pEntity.pokemon.effectiveScale * (pEntity.delegate as PokemonClientDelegate).activeSendoutScale
        pPoseStack.scale(scale, scale, scale)
    }

    /**
     * Renders a beam between the Cobblemon and the target.
     *
     * @param matrixStack The matrix stack to render with.
     * @param partialTicks The partial ticks.
     * @param entity The Cobblemon.
     * @param beamTarget The target.
     * @param colour The colour of the beam.
     * @param buffer The vertex consumer provider.
     */
    fun renderBeam(matrixStack: PoseStack, partialTicks: Float, entity: PokemonEntity, beamTarget: Entity, buffer: SubmitNodeCollector, offset: Vec3) {
        val clientDelegate = entity.delegate as PokemonClientDelegate
        val pokemonPosition = entity.position().add(0.0, entity.bbHeight / 2.0 * clientDelegate.activeSendoutScale.toDouble(), 0.0)
        var beamSourcePosition = if (beamTarget is EmptyPokeBallEntity) {
            (beamTarget.delegate as PokeBallPosableState).locatorStates["beam"]?.getOrigin() ?: beamTarget.position()
        } else {
            if (beamTarget.uuid == Minecraft.getInstance().player?.uuid) {
                val lookVec = beamTarget.lookAngle.yRot((PI / 2).toFloat()).multiply(1.0, 0.0, 1.0).normalize()
                beamTarget.getEyePosition(partialTicks).subtract(0.0, 0.4, 0.0).subtract(lookVec.scale(0.3))
            } else if (beamTarget is NPCEntity) {
                val npcDelegate = beamTarget.delegate as NPCClientDelegate
                val baseScale = beamTarget.renderScale * beamTarget.hitboxScale

                (npcDelegate.locatorStates["beam"]?.getOrigin()?.scale(baseScale.toDouble()))
                    ?: beamTarget.position().add(0.0, (beamTarget.bbHeight / 2.0) * baseScale, 0.0)
            } else {
                val lookVec = beamTarget.lookAngle.yRot((PI / 2 - (beamTarget.visualRotationYInDegrees - beamTarget.xRot).toRadians()).toFloat()).multiply(1.0, 0.0, 1.0).normalize()
                beamTarget.getEyePosition(partialTicks).subtract(0.0, 0.7, 0.0).subtract(lookVec.scale(0.4))
            }
        }
        if (clientDelegate.sendOutPosition != null) {
            beamSourcePosition = clientDelegate.sendOutPosition!!
        }

        if (beamSourcePosition.distanceTo(pokemonPosition) > 20) {
            return
        }
        var newOffset = offset.scale(2.0)
        // the further away the source position is, the smaller the newOffset should be. Max distance is 20 blocks
        val distance = beamSourcePosition.distanceTo(entity.position())
        newOffset = newOffset.scale((distance / 10.0) * 5)
        newOffset = newOffset.multiply(0.0, 1+ease(clientDelegate.ballOffset.toDouble()), 0.0)
        val direction = pokemonPosition.subtract(beamSourcePosition.add(newOffset)).let { Vector3f(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()) }

        matrixStack.pushPose()
        with(beamSourcePosition.subtract(entity.position())) {
            matrixStack.translate(x + newOffset.x, y + newOffset.y, z + newOffset.z)
        }

        val s = clientDelegate.secondsSinceBeamEffectStarted
        val ratio = if (s < BEAM_EXTEND_TIME) {
            s / BEAM_EXTEND_TIME
        } else if (s > BEAM_EXTEND_TIME + BEAM_SHRINK_TIME) {
            1 - min((s - BEAM_EXTEND_TIME - BEAM_SHRINK_TIME) / BEAM_EXTEND_TIME, 1F)
        } else {
            1F
        }

        direction.normalize()

        val yAxis = Vector3f(0F, 1F, 0F)
        val dot = direction.dot(yAxis)
        val cross = yAxis.cross(direction)
        val q = Quaternionf(cross.x, cross.y, cross.z, 1 + dot).normalize()
        matrixStack.mulPose(q)

        renderBeaconBeam(
            matrixStack = matrixStack,
            buffer = buffer,
            partialTicks = partialTicks,
            totalLevelTime = entity.level().gameTime,
            height = pokemonPosition.distanceTo(beamSourcePosition.add(offset)).toFloat() * ratio,
            red = recallBeamColour.x,
            green = recallBeamColour.y,
            blue = recallBeamColour.z,
            alpha = recallBeamColour.w,
            beamRadius = 0.03F,
            glowRadius = 0.07F,
            glowAlpha = 0.4F
        )

        matrixStack.popPose()
    }

    // PT145: getFlipDegrees(T)/shouldShowName(T) replaced by state-based variants in MC 26.1.x.
    fun getFlipDegrees_DEFER_NO_OVERRIDE(entity: PokemonEntity): Float = 0F

    fun shouldShowName_DEFER_NO_OVERRIDE(entity: PokemonEntity): Boolean = false

    private fun shouldRenderLabel(entity: PokemonEntity): Boolean {
        if (!shouldShowName_DEFER_NO_OVERRIDE(entity)) {
            return false
        }
        if (entity.entityData.get(PokemonEntity.HIDE_LABEL)) {
            return false
        }
        val player = Minecraft.getInstance().player ?: return false
        val delegate = entity.delegate as? PokemonClientDelegate ?: return false
        return (!Cobblemon.config.displayEntityLabelsWhenCrouchingOnly || player.isCrouching) &&
                player.isLookingAt(entity) &&
                delegate.phaseTarget == null &&
                !CobblemonClient.pokedexUsageContext.scanningGuiOpen
    }

    // PT145: renderNameTag(T,...) removed in MC 26.1.x — kept as helper used by render() variant.
    fun renderNameTag(
        entity: PokemonEntity,
        text: Component,
        matrices: PoseStack,
        vertexConsumers: SubmitNodeCollector,
        light: Int,
        tickDelta: Float
    ) {
        if (entity.isInvisible) {
            return
        }
        val player = Minecraft.getInstance().player ?: return
        // PT145: entityRenderDispatcher field private — use Minecraft.getInstance().entityRenderDispatcher.
        val d = Minecraft.getInstance().entityRenderDispatcher.distanceToSqr(entity)
        if (d <= 4096.0){
            val scale = min(1.5, max(0.65, d.remap(DoubleRange(-16.0, 96.0), DoubleRange(0.0, 1.0))))
            val sizeScale = Mth.lerp(scale.remap(DoubleRange(0.65, 1.5), DoubleRange(0.0,1.0)), 0.5, 1.0)
            val offsetScale = Mth.lerp(scale.remap(DoubleRange(0.65, 1.5), DoubleRange(0.0,1.0)), 0.0,1.0)
            val entityHeight = entity.boundingBox.ysize + 0.5f
            matrices.pushPose()
            matrices.translate(0.0, entityHeight.toDouble(), 0.0)
            // PT145: EntityRenderDispatcher.cameraOrientation() removed in MC 26.1.x — use mainCamera.rotation().
            matrices.mulPose(Minecraft.getInstance().gameRenderer.mainCamera().rotation())
            matrices.translate(0.0, 0.0 + (offsetScale / 2), -(scale + offsetScale))
            matrices.scale((0.025 * sizeScale).toFloat(), (-0.025 * sizeScale).toFloat(), (1 * sizeScale).toFloat())
            // port/26.2: Font.drawInBatch is gone; text is submitted to the collector, which reads the
            // matrix off the pose stack itself, so the separate matrix4f is no longer needed.
            val opacity = (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F).toInt() shl 24
            val label = this.resolveBaseLabel(entity)
            if (ServerSettings.displayEntityLevelLabel && entity.labelLevel() > 0) {
                if (ServerSettings.displayEntityNameLabel) {
                    label.append(SPACE)
                }
                // This a Style.EMPTY with a lot of effects set to false and color set to white, renderer inherits these from nick otherwise
                val levelLabel = lang("label.lv", entity.labelLevel())
                    .setStyle(LEVEL_LABEL_STYLE)
                label.append(levelLabel)
            }
            var h = (-Minecraft.getInstance().font.width(label) / 2).toFloat()
            val y = 0F
            val packedLight = ((15) or ((15) shl 16))
            vertexConsumers.submitText(matrices, h, y, label.visualOrderText, false, DisplayMode.SEE_THROUGH, packedLight, 0x20FFFFFF, opacity, 0)
            vertexConsumers.submitText(matrices, h, y, label.visualOrderText, false, DisplayMode.NORMAL, packedLight, -1, 0, 0)

            if (CobblemonClient.clientPlayerData.showChallengeLabel && entity.canBattle(player)) {
                val sendOutBinding = PartySendBinding.boundKey().displayName
                val battlePrompt = lang("challenge_label", sendOutBinding)
                h = (-Minecraft.getInstance().font.width(battlePrompt) / 2).toFloat()
                vertexConsumers.submitText(matrices, h, y + 10, battlePrompt.visualOrderText, false, DisplayMode.SEE_THROUGH, packedLight, 0x20FFFFFF, opacity, 0)
                vertexConsumers.submitText(matrices, h, y + 10, battlePrompt.visualOrderText, false, DisplayMode.NORMAL, packedLight, -1, 0, 0)
            }
            matrices.popPose()
        }
    }

    private fun resolveBaseLabel(entity: PokemonEntity): MutableComponent {
        return when {
            !ServerSettings.displayEntityNameLabel -> Component.empty()
            Cobblemon.config.displayNameForUnknownPokemon || CobblemonClient.clientPokedexData.getKnowledgeForSpecies(entity.pokemon.species.resourceIdentifier) != PokedexEntryProgress.UNREGISTERED -> entity.getTitledName()
            else -> Component.literal(HIDDEN_NAME)
        }
    }

    private fun drawPokeBall(
        state: ClientBallDisplay,
        matrixStack: PoseStack,
        scale: Float = 5F,
        partialTicks: Float,
        reversed: Boolean = false,
        buff: SubmitNodeCollector,
        packedLight: Int,
        ball: PokeBall,
        distance: Int
    ) {
        matrixStack.pushPose()
        matrixStack.scale(0.7F, -0.7F, -0.7F)
        val model = VaryingModelRepository.getPoser(ball.name, state)
        val texture = VaryingModelRepository.getTexture(ball.name, state)
        if (scale == 1.0f) {
            model.moveToPose(state, model.poses["open"]!!)
        } else {
            matrixStack.translate(0.0, -0.2, 0.0)
            val rot = 360F * distance
            if(ball.name.toString().contains("beast")){
                matrixStack.mulPose(Axis.ZN.rotationDegrees(Mth.lerp(scale, 0F, rot)))
            } else {
                matrixStack.mulPose(Axis.XN.rotationDegrees(Mth.lerp(scale, 0F, rot)))
            }
            matrixStack.translate(0.0, 0.2, 0.0)
        }
        state.updatePartialTicks(partialTicks)
        model.context = ballContext
        ballContext.put(RenderContext.ASPECTS, state.aspects)
        ballContext.put(RenderContext.POSABLE_STATE, state)
        model.applyAnimations(null, state, 0F, 0F, 0F, 0F, 0F)
//        model.animateModel(null, 0f, 0F, 0F)
        buff.submitPosableModel(matrixStack, RenderTypes.entityCutout(texture)) { stack, consumer ->
            model.render(ballContext, stack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -0x1)
        }
        model.green = 1f
        model.blue = 1f
        model.red = 1f
        model.resetLayerContext()
        matrixStack.popPose()
    }

    private fun drawPlatform(
            matrixStack: PoseStack,
            entity: PokemonEntity,
            scale: Float = 1F,
            buff: SubmitNodeCollector,
            packedLight: Int,
            ) {
        val (modelResource, textureResource) = PlatformType.getModelWithTexture(entity.platform)
        val model = MiscModelRepository.modelOf(modelResource) ?: return
        matrixStack.pushPose()
        matrixStack.mulPose(Axis.ZP.rotationDegrees(180f))
        matrixStack.rotateAround(Axis.YP.rotationDegrees(entity.entityData.get(SPAWN_DIRECTION)), 0.0f, 0f, 0.0f)
        matrixStack.scale(scale, scale, scale)
        buff.submitPosableModel(matrixStack, RenderTypes.entityCutout(textureResource)) { stack, consumer ->
            model.render(stack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -0x1)
        }

        matrixStack.popPose()
    }
}
