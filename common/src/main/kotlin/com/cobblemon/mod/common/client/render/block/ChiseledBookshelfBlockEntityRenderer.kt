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

        for ((index, item) in entity.items.withIndex()) {
            if (!item.isEmpty) {
                val col = index % 7
                val row = index / 7

                val slotWidth = 1f / 7f
                val slotHeight = 0.5f

                val posX = (6 - col) * slotWidth
                val posY = if (row == 0) 0.5f else 0.0f
                val posZ = -0.001f

                val move = TMMoveComponent.getTMMove(item)
                val color = move?.elementalType?.primaryColor ?: 0xAAAAAA

                poseStack.pushPose()
                poseStack.translate(posX, posY, posZ)
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

        val u0 = 0f
        val v0 = 0f
        val u1 = 1f
        val v1 = 1f

        consumer.addVertex(matrix, 0f, 0f, 0f)
            .setColor(r, g, b, a)
            .setUv(u0, v0)
            .setUv1(0, 0)
            .setUv2(light and 0xFFFF, light shr 16)
            .setNormal(0f, 0f, -1f)

        consumer.addVertex(matrix, 0f, height, 0f)
            .setColor(r, g, b, a)
            .setUv(u0, v1)
            .setUv1(0, 0)
            .setUv2(light and 0xFFFF, light shr 16)
            .setNormal(0f, 0f, -1f)

        consumer.addVertex(matrix, width, height, 0f)
            .setColor(r, g, b, a)
            .setUv(u1, v1)
            .setUv1(0, 0)
            .setUv2(light and 0xFFFF, light shr 16)
            .setNormal(0f, 0f, -1f)

        consumer.addVertex(matrix, width, 0f, 0f)
            .setColor(r, g, b, a)
            .setUv(u1, v0)
            .setUv1(0, 0)
            .setUv2(light and 0xFFFF, light shr 16)
            .setNormal(0f, 0f, -1f)
    }
}