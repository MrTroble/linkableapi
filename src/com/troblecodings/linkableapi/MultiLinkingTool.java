package com.troblecodings.linkableapi;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

import com.google.common.base.Predicate;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MultiLinkingTool extends Linkingtool {

    private static final String LINKED_BLOCKS = "linkedBlocks";

    public MultiLinkingTool(final ItemGroups tab, final BiPredicate<World, BlockPos> predicate,
            final ComponentType<NbtCompound> data) {
        super(tab, predicate, data);
    }

    public MultiLinkingTool(final ItemGroups tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet, final TaggableFunction function,
            final ComponentType<NbtCompound> data) {
        super(tab, predicate, predicateSet, function, data);
    }

    public MultiLinkingTool(final ItemGroups tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet, final ComponentType<NbtCompound> data) {
        super(tab, predicate, predicateSet, data);
    }

    @Override
    public ActionResult useOnBlock(final ItemUsageContext ctx) {
        final PlayerEntity player = ctx.getPlayer();
        if (player == null)
            return ActionResult.FAIL;
        final World levelIn = ctx.getWorld();
        if (levelIn.isClient())
            return ActionResult.PASS;
        final BlockPos pos = ctx.getBlockPos();
        final BlockEntity entity = levelIn.getBlockEntity(pos);
        final ItemStack stack = ctx.getStack();
        final NbtCompound itemTag = getOrCreateNbt(stack);
        if (entity instanceof ILinkableTile && this.predicateSet.apply(entity)) {
            final ILinkableTile controller = (ILinkableTile) entity;
            if (!player.isSneaking()) {
                if (!itemTag.contains(LINKED_BLOCKS)) {
                    message(player, "lt.notset", pos.toString());
                    return ActionResult.PASS;
                }
                final NbtList list = (NbtList) itemTag.get(LINKED_BLOCKS);
                if (list == null) {
                    message(player, "lt.notlinked");
                    return ActionResult.FAIL;
                }
                list.stream().map(tag -> toBlockPos((NbtIntArray) tag, LINKED_BLOCKS))
                        .forEach(linkPos -> {
                            if (controller.link(linkPos, itemTag)) {
                                message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                            }
                        });
                removeToolTag(stack);
                message(player, "lt.reset");
                stack.damage(1, player, EquipmentSlot.MAINHAND);
            } else {
                if (controller.canBeLinked() && predicate.test(levelIn, pos)) {
                    NbtList list = (NbtList) itemTag.get(LINKED_BLOCKS);
                    if (list == null) {
                        list = new NbtList();
                    }
                    final NbtElement posTag = NbtHelper.fromBlockPos(pos);
                    if (list.contains(posTag)) {
                        message(player, "lt.setpos.msg");
                        return ActionResult.FAIL;
                    }
                    list.add(NbtHelper.fromBlockPos(pos));
                    tagFromFunction.test(levelIn, pos, list);
                    itemTag.put(LINKED_BLOCKS, list);
                    stack.set(compoundData, itemTag);
                    message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
                    message(player, "lt.setpos.msg");
                    return ActionResult.SUCCESS;
                }
                if (controller.hasLink() && controller.unlink()) {
                    message(player, "lt.unlink");
                }
            }
            return ActionResult.SUCCESS;
        }
        if (predicate.test(levelIn, pos)) {
            NbtList list = (NbtList) itemTag.get(LINKED_BLOCKS);
            if (list == null) {
                list = new NbtList();
            }
            final NbtElement posTag = NbtHelper.fromBlockPos(pos);
            if (list.contains(posTag)) {
                message(player, "lt.setpos.msg");
                return ActionResult.FAIL;
            }
            list.add(NbtHelper.fromBlockPos(pos));
            tagFromFunction.test(levelIn, pos, list);
            itemTag.put(LINKED_BLOCKS, list);
            stack.set(compoundData, itemTag);
            message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
            message(player, "lt.setpos.msg");
            return ActionResult.SUCCESS;
        }
        if (player.isSneaking()) {
            removeToolTag(stack);
            message(player, "lt.reset");
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    @Override
    public void removeToolTag(final ItemStack stack) {
        stack.remove(compoundData);
    }

    public static Optional<BlockPos> toBlockPos(final NbtIntArray tag, final String string) {
        int[] aint = tag.getIntArray();
        return aint.length == 3 ? Optional.of(new BlockPos(aint[0], aint[1], aint[2]))
                : Optional.empty();
    }

    @Override
    public void appendTooltip(final ItemStack stack, final TooltipContext context,
            final List<Text> tooltip, final TooltipType type) {
        final NbtCompound itemTag = getOrCreateNbt(stack);
        if (itemTag.contains(LINKED_BLOCKS)) {
            NbtList list = (NbtList) itemTag.get(LINKED_BLOCKS);
            if (list != null) {
                list.stream().map(tag -> toBlockPos((NbtIntArray) tag, LINKED_BLOCKS).get())
                        .forEach(pos -> {
                            tooltip.add(Text.translatable("lt.linkedpos",
                                    Text.literal(String.valueOf(pos.getX())),
                                    Text.literal(String.valueOf(pos.getY())),
                                    Text.literal(String.valueOf(pos.getZ()))));
                        });
                return;
            }
        }
        tooltip(tooltip, "lt.notlinked");
        tooltip(tooltip, "lt.notlinked.msg");
    }
}
