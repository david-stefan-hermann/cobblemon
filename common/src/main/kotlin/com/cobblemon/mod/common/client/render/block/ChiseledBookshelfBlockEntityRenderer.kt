package com.cobblemon.mod.common.client.render.block

import com.cobblemon.mod.common.block.entity.ChiseledBookshelfBlockEntity
import com.cobblemon.mod.common.item.components.TMMoveComponent
import com.cobblemon.mod.common.util.cobblemonResource
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FastColor
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.core.Direction
import org.joml.Matrix4f

class ChiseledBookshelfBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) :
    BlockEntityRenderer<ChiseledBookshelfBlockEntity> {

    private val texture = cobblemonResource("textures/block/chiseled_bookshelf/technical_machine_case.png")
    private val vanillaFaceTexture = ResourceLocation.parse("minecraft:textures/block/chiseled_bookshelf_empty.png")

    // Layout parameters in pixels (relative to 16x16 block face)
    private val slotWidthPixels = 2
    private val slotHeightPixels = 6
    private val textureSize = 16f

    private val leftMargin = 1
    private val topMargin = 1
    private val interRowSpacing = 2
    private val slotSpacing = 0  // Horizontal spacing between slots if needed


    override fun render(
        entity: ChiseledBookshelfBlockEntity,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {

        val facing = entity.blockState.getValue(HorizontalDirectionalBlock.FACING)
        val rotation = when (facing) {
            Direction.NORTH -> 0f
            Direction.SOUTH -> 180f
            Direction.WEST -> 90f
            Direction.EAST -> -90f
            else -> 0f
        }

        poseStack.pushPose()
        poseStack.translate(0.5, 0.5, 0.5)
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation))
        poseStack.translate(-0.5, -0.5, -0.5)

        // we render the dumb missing face
        renderFrontFaceQuad(poseStack, buffer, vanillaFaceTexture, light)

        val slotWidth = slotWidthPixels / textureSize
        val slotHeight = slotHeightPixels / textureSize

        for ((index, item) in entity.items.withIndex()) {
            if (!item.isEmpty) {
                val col = index % 7
                val row = index / 7

                val posX = (leftMargin + (6 - col) * slotWidthPixels) / textureSize
                val posY = when (row) {
                    0 -> (topMargin + slotHeightPixels + interRowSpacing) / textureSize
                    1 -> topMargin / textureSize
                    else -> continue
                }

                val move = TMMoveComponent.getTMMove(item)
                val color = move?.elementalType?.primaryColor ?: 0xAAAAAA

                poseStack.pushPose()
                poseStack.translate(posX, posY, -0.001f)
                renderSlotQuad(poseStack, buffer, texture, color, light, slotWidth, slotHeight)
                poseStack.popPose()
            }
        }

        poseStack.popPose()
    }

    private fun renderSlotQuad(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        texture: ResourceLocation,
        color: Int,
        light: Int,
        width: Float,
        height: Float
    ) {
        val consumer: VertexConsumer = buffer.getBuffer(RenderType.entitySolid(texture))
        val matrix: Matrix4f = poseStack.last().pose()

        val r = FastColor.ARGB32.red(color) / 255f
        val g = FastColor.ARGB32.green(color) / 255f
        val b = FastColor.ARGB32.blue(color) / 255f
        val a = 1.0f

        consumer.addVertex(matrix, 0f, 0f, 0f)
            .setColor(r, g, b, a)
            .setUv(0f, 0f)
            .setUv1(0, 0)
            .setUv2(light and 0xFFFF, light shr 16)
            .setNormal(0f, 0f, -1f)

        consumer.addVertex(matrix, 0f, height, 0f)
            .setColor(r, g, b, a)
            .setUv(0f, 1f)
            .setUv1(0, 0)
            .setUv2(light and 0xFFFF, light shr 16)
            .setNormal(0f, 0f, -1f)

        consumer.addVertex(matrix, width, height, 0f)
            .setColor(r, g, b, a)
            .setUv(1f, 1f)
            .setUv1(0, 0)
            .setUv2(light and 0xFFFF, light shr 16)
            .setNormal(0f, 0f, -1f)

        consumer.addVertex(matrix, width, 0f, 0f)
            .setColor(r, g, b, a)
            .setUv(1f, 0f)
            .setUv1(0, 0)
            .setUv2(light and 0xFFFF, light shr 16)
            .setNormal(0f, 0f, -1f)
    }

    private fun renderFrontFaceQuad(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        texture: ResourceLocation,
        light: Int
    ) {
        val consumer = buffer.getBuffer(RenderType.entitySolid(texture))
        val matrix = poseStack.last().pose()

        val u0 = 0f
        val v0 = 0f
        val u1 = 1f
        val v1 = 1f

        val r = 1.0f
        val g = 1.0f
        val b = 1.0f
        val a = 1.0f

        // Front face is on Z = 0
        consumer.addVertex(matrix, 0f, 0f, 0f)
            .setColor(r, g, b, a)
            .setUv(u0, v0)
            .setUv1(0, 0)
            .setUv2(light and 0xFFFF, light shr 16)
            .setNormal(0f, 0f, -1f)

        consumer.addVertex(matrix, 0f, 1f, 0f)
            .setColor(r, g, b, a)
            .setUv(u0, v1)
            .setUv1(0, 0)
            .setUv2(light and 0xFFFF, light shr 16)
            .setNormal(0f, 0f, -1f)

        consumer.addVertex(matrix, 1f, 1f, 0f)
            .setColor(r, g, b, a)
            .setUv(u1, v1)
            .setUv1(0, 0)
            .setUv2(light and 0xFFFF, light shr 16)
            .setNormal(0f, 0f, -1f)

        consumer.addVertex(matrix, 1f, 0f, 0f)
            .setColor(r, g, b, a)
            .setUv(u1, v0)
            .setUv1(0, 0)
            .setUv2(light and 0xFFFF, light shr 16)
            .setNormal(0f, 0f, -1f)
    }
}