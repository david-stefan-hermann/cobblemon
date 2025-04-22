package com.cobblemon.mod.common.mixin;

import com.cobblemon.mod.common.CobblemonBlocks;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.block.entity.ChiseledBookshelfBlockEntity;
import com.cobblemon.mod.common.item.TechnicalMachineItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

@Mixin(ChiseledBookShelfBlock.class)
public abstract class ChiseledBookShelfMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void cobblemon$replaceWithTMBookshelf(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                  Player player, InteractionHand hand, BlockHitResult hit,
                                                  CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (!level.isClientSide && isInsertableItem_(stack)) {
            Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
            OptionalInt slotOpt = getHitSlot_(hit, facing);
            if (slotOpt.isEmpty()) return;

            int slot = slotOpt.getAsInt();
            ItemStack inserted = stack.copyWithCount(1);
            stack.shrink(1);

            BlockState newState = CobblemonBlocks.CHISELED_BOOKSHELF.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
            level.setBlockAndUpdate(pos, newState);

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChiseledBookshelfBlockEntity entity) {
                entity.getItems().set(slot, inserted);
                entity.setLastInteractedSlot(slot);
                entity.markUpdated();
                cir.setReturnValue(ItemInteractionResult.SUCCESS);
            }
        }
    }

    private OptionalInt getHitSlot_(BlockHitResult hit, Direction facing) {
        var relative = hit.getLocation().subtract(
                hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ()
        );

        double x = relative.x();
        double y = relative.y();
        double z = relative.z();

        double horiz = switch (facing) {
            case NORTH -> 1.0 - x;
            case SOUTH -> x;
            case WEST -> z;
            case EAST -> 1.0 - z;
            default -> -1;
        };

        if (horiz < 0.0625 || horiz > 0.9375 || y < 0.0 || y > 1.0) return OptionalInt.empty();

        int col = (int)((horiz - 0.0625) * 8); // each slot is ~0.125 blocks wide
        col = Math.min(Math.max(col, 0), 6);

        int row = y >= 0.5625 ? 0 : (y < 0.5625 && y >= 0.0 ? 1 : -1);
        if (row == -1) return OptionalInt.empty();

        return OptionalInt.of(row * 7 + col);
    }

    private boolean isInsertableItem_(ItemStack stack) {
        return stack.getItem() instanceof TechnicalMachineItem
                || stack.is(CobblemonItems.UPGRADE)
                || stack.is(CobblemonItems.DUBIOUS_DISC);
    }
}
