package com.cobblemon.mod.common.item

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.moves.BenchedMove
import com.cobblemon.mod.common.api.text.gray
import com.cobblemon.mod.common.api.text.green
import com.cobblemon.mod.common.api.tms.TechnicalMachines
import com.cobblemon.mod.common.block.entity.TMBlockEntity
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.item.components.TMMoveComponent
import com.cobblemon.mod.common.util.lang
import com.cobblemon.mod.common.util.toBlockPos
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.properties.BlockStateProperties

class TechnicalMachineItem(properties: Properties) : CobblemonItem(properties) {
    override fun isFoil(stack: ItemStack?) = false

    override fun appendHoverText(
            stack: ItemStack,
            context: TooltipContext,
            tooltip: MutableList<Component>,
            tooltipFlag: TooltipFlag
    ) {
        val move = TMMoveComponent.getTMMove(stack)
        val text = move?.displayName ?: lang("tms.unknown_move")
        tooltip.add(text.gray())
        super.appendHoverText(stack, context, tooltip, tooltipFlag)
    }

    override fun interactLivingEntity(stack: ItemStack, user: Player, target: LivingEntity, hand: InteractionHand): InteractionResult {
        if (user.level().isClientSide) return InteractionResult.FAIL
        if (target !is PokemonEntity) return InteractionResult.FAIL

        val tm = TMMoveComponent.getTMMove(stack) ?: return InteractionResult.FAIL
        val pokemon = target.pokemon

        val tmLearnableMoves = pokemon.species.moves.tmLearnableMoves()

        if (!tmLearnableMoves.contains(tm)) {
            user.displayClientMessage(lang("tms.cannot_learn", pokemon.getDisplayName(), tm.displayName), true)
            return InteractionResult.FAIL
        }
        if (pokemon.allAccessibleMoves.contains(tm)) {
            user.displayClientMessage(lang("tms.already_known", pokemon.getDisplayName(), tm.displayName), true)
            return InteractionResult.FAIL
        }

        if (!user.isCreative) {
            stack.shrink(1)
        }
        if (pokemon.moveSet.hasSpace()) {
            pokemon.moveSet.add(tm.create())
        } else {
            pokemon.benchedMoves.add(BenchedMove(tm, 0))
        }

        user.displayClientMessage(lang("tms.teach_move", pokemon.getDisplayName(), tm.displayName).green(), true)
        user.level().playSound(null, user.blockPosition(), CobblemonSounds.TM_USE, SoundSource.PLAYERS, 1.0f, 1.0f)
        target.cry()
        return InteractionResult.CONSUME
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        if (level.isClientSide) {
            context.player?.swing(context.hand)
            return InteractionResult.FAIL
        }

        val blockEntity = level.getBlockEntity(context.clickedPos)

        if (blockEntity is TMBlockEntity && (blockEntity.tmmInventory.filterTM == null || blockEntity.tmmInventory.filterTM != TMMoveComponent.getTMMove(context.itemInHand))) {
            context.player?.level()?.playSound(null, context.player?.blockPosition(), CobblemonSounds.TMM_ON, SoundSource.BLOCKS, 1.0f, 1.0f)
            context.player?.swing(context.hand)
            val filterTM = blockEntity.tmmInventory.filterTM
            if (filterTM != null) {
                if (!context.player?.isCreative!!) {
                    context.player!!.addItem(TMMoveComponent.createStack(filterTM))
                }
            }

            blockEntity.tmmInventory.filterTM = TMMoveComponent.getTMMove(context.itemInHand)

            if (!context.player?.isCreative!!) {
                context.itemInHand.shrink(1)
            }

            blockEntity.setChanged()
            blockEntity.level?.sendBlockUpdated(blockEntity.blockPos, blockEntity.blockState, blockEntity.blockState, Block.UPDATE_ALL)
        }
        return super.useOn(context)
    }
}
