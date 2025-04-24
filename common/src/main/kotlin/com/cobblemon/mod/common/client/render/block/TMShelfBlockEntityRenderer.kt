package com.cobblemon.mod.common.client.render.block

import com.cobblemon.mod.common.block.entity.TMShelfBlockEntity
import com.cobblemon.mod.common.item.TechnicalMachineItem
import com.cobblemon.mod.common.item.components.TMMoveComponent
import com.cobblemon.mod.common.util.cobblemonResource
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FastColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import org.joml.Matrix4f

class TMShelfBlockEntityRenderer(ctx: BlockEntityRendererProvider.Context) :
        BlockEntityRenderer<TMShelfBlockEntity> {

    private val slotWidthPixels = 6
    private val slotHeightPixels = 2
    private val textureSize = 16f

    private val leftMargin = 1f
    private val rightMargin = 1f
    private val topMargin = 1f
    private val bottomMargin = 1f
    private val interRowSpacing = 0f

    override fun render(
            entity: TMShelfBlockEntity,
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

        val lightGetter = entity.level as? BlockAndTintGetter
        val packedLight = lightGetter?.let { LevelRenderer.getLightColor(it, entity.blockPos) } ?: 0xF000F0

        poseStack.pushPose()
        poseStack.translate(0.5, 0.5, 0.5)
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation))
        poseStack.translate(-0.5, -0.5, -0.5)

        val slotWidth = slotWidthPixels / textureSize
        val slotHeight = slotHeightPixels / textureSize
        val verticalSpacing = (slotHeightPixels + interRowSpacing) / textureSize
        val totalHeight = slotHeight * 7 + interRowSpacing * 6 / textureSize
        val startY = topMargin / textureSize

        for ((index, item) in entity.items.withIndex()) {
            if (!item.isEmpty) {
                val row = index / 2
                val col = index % 2

                val isLeftColumn = col == 0
                val posX = if (isLeftColumn)
                    leftMargin / textureSize
                else
                    (16f - rightMargin - slotWidthPixels) / textureSize

                val posY = startY + (6 - row) * verticalSpacing

                poseStack.pushPose()
                poseStack.translate(posX, posY, -0.001f)

                val tex = getItemTexture(item)
                val colorize = item.item is TechnicalMachineItem
                val color = if (colorize) {
                    TMMoveComponent.getTMMove(item)?.elementalType?.primaryColor ?: 0xAAAAAA
                } else 0xFFFFFF

                renderSlotQuad(poseStack, buffer, tex, color, packedLight, slotWidth, slotHeight, tint = colorize)

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
            height: Float,
            tint: Boolean
    ) {
        val consumer: VertexConsumer = buffer.getBuffer(RenderType.text(texture))
        val matrix: Matrix4f = poseStack.last().pose()

        val (r, g, b, a) = if (tint) {
            listOf(
                    FastColor.ARGB32.red(color) / 255f,
                    FastColor.ARGB32.green(color) / 255f,
                    FastColor.ARGB32.blue(color) / 255f,
                    1.0f
            )
        } else listOf(1f, 1f, 1f, 1f)

        consumer.addVertex(matrix, 0f, 0f, 0f).setColor(r, g, b, a).setUv(0f, 0f)
                .setUv1(0, 0).setUv2(light and 0xFFFF, light shr 16).setNormal(0f, 0f, -1f)
        consumer.addVertex(matrix, 0f, height, 0f).setColor(r, g, b, a).setUv(0f, 1f)
                .setUv1(0, 0).setUv2(light and 0xFFFF, light shr 16).setNormal(0f, 0f, -1f)
        consumer.addVertex(matrix, width, height, 0f).setColor(r, g, b, a).setUv(1f, 1f)
                .setUv1(0, 0).setUv2(light and 0xFFFF, light shr 16).setNormal(0f, 0f, -1f)
        consumer.addVertex(matrix, width, 0f, 0f).setColor(r, g, b, a).setUv(1f, 0f)
                .setUv1(0, 0).setUv2(light and 0xFFFF, light shr 16).setNormal(0f, 0f, -1f)
    }

    private fun getItemTexture(itemStack: ItemStack): ResourceLocation {
        val id = BuiltInRegistries.ITEM.getKey(itemStack.item)
        return cobblemonResource("textures/block/tm_shelf/${id.path}.png")
    }
}