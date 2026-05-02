package com.troblecodings.linkableapi;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.base.Predicate;
import com.troblecodings.tcredstone.TCRedstoneMain;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MultiLinkingTool extends Linkingtool {

    private static final String LINKED_BLOCKS = "linkedBlocks";

    public MultiLinkingTool(final Properties properties, final CreativeModeTab tab,
            final BiPredicate<Level, BlockPos> predicate) {
        super(properties, tab, predicate);
    }

    public MultiLinkingTool(final Properties properties, final CreativeModeTab tab,
            final BiPredicate<Level, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet, final TaggableFunction function) {
        super(properties, tab, predicate, predicateSet, function);
    }

    public MultiLinkingTool(final Properties properties, final CreativeModeTab tab,
            final BiPredicate<Level, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet) {
        super(properties, tab, predicate, predicateSet);
    }

    @Override
    public InteractionResult useOn(final UseOnContext ctx) {
        final Player player = ctx.getPlayer();
        if (player == null)
            return InteractionResult.FAIL;
        final Level levelIn = ctx.getLevel();
        if (levelIn.isClientSide())
            return InteractionResult.PASS;
        final BlockPos pos = ctx.getClickedPos();
        final BlockEntity entity = levelIn.getBlockEntity(pos);
        final ItemStack stack = ctx.getItemInHand();
        final CompoundTag itemTag = getOrCreateNbt(stack);
        if (entity instanceof ILinkableTile && this.predicateSet.apply(entity)) {
            final ILinkableTile controller = (ILinkableTile) entity;
            if (!player.isShiftKeyDown()) {
                if (!itemTag.contains(LINKED_BLOCKS)) {
                    message(player, "lt.notset", pos.toString());
                    return InteractionResult.PASS;
                }
                final ListTag list = (ListTag) itemTag.get(LINKED_BLOCKS);
                if (list == null) {
                    message(player, "lt.notlinked");
                    return InteractionResult.FAIL;
                }
                list.stream().map(tag -> toBlockPos((IntArrayTag) tag, LINKED_BLOCKS))
                        .forEach(linkPos -> {
                            if (controller.link(linkPos, itemTag)) {
                                message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                            }
                        });
                removeToolTag(stack);
                message(player, "lt.reset");
                stack.hurtAndBreak(list.size(), (ServerLevel) levelIn, (ServerPlayer) player,
                        item -> {
                        });
                return InteractionResult.SUCCESS;
            } else {
                if (controller.canBeLinked() && predicate.test(levelIn, pos)) {
                    ListTag tagList = (ListTag) itemTag.get(LINKED_BLOCKS);
                    if (tagList == null) {
                        tagList = new ListTag();
                    }
                    final Tag posTag = writeBlockPos(pos);
                    if (tagList.contains(posTag)) {
                        message(player, "lt.setpos.msg");
                        return InteractionResult.FAIL;
                    }
                    tagList.add(writeBlockPos(pos));
                    tagFromFunction.test(levelIn, pos, tagList);
                    itemTag.put(LINKED_BLOCKS, tagList);
                    stack.set(TCRedstoneMain.COMPOUND_DATA, itemTag);
                    message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
                    message(player, "lt.setpos.msg");
                    return InteractionResult.SUCCESS;
                }
                if (controller.hasLink() && controller.unlink()) {
                    message(player, "lt.unlink");
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.SUCCESS;
        } else if (predicate.test(levelIn, pos)) {
            ListTag tagList = (ListTag) itemTag.get(LINKED_BLOCKS);
            if (tagList == null) {
                tagList = new ListTag();
            }
            final Tag posTag = writeBlockPos(pos);
            if (tagList.contains(posTag)) {
                message(player, "lt.setpos.msg");
                return InteractionResult.FAIL;
            }
            tagList.add(posTag);
            tagFromFunction.test(levelIn, pos, tagList);
            itemTag.put(LINKED_BLOCKS, tagList);
            stack.set(TCRedstoneMain.COMPOUND_DATA, itemTag);
            message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
            message(player, "lt.setpos.msg");
            return InteractionResult.SUCCESS;
        } else if (player.isShiftKeyDown()) {
            removeToolTag(stack);
            message(player, "lt.reset");
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void removeToolTag(final ItemStack stack) {
        stack.remove(TCRedstoneMain.COMPOUND_DATA);
    }

    public static Optional<BlockPos> toBlockPos(final IntArrayTag tag, final String string) {
        int[] aint = tag.getAsIntArray();
        return aint.length == 3 ? Optional.of(new BlockPos(aint[0], aint[1], aint[2]))
                : Optional.empty();
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext ctx,
            final TooltipDisplay display, final Consumer<Component> tooltip,
            final TooltipFlag flagIn) {
        final CompoundTag itemTag = getOrCreateNbt(stack);
        if (itemTag.contains(LINKED_BLOCKS)) {
            final ListTag list = (ListTag) itemTag.get(LINKED_BLOCKS);
            if (list != null) {
                tooltip(tooltip, "lt.linkedpos",
                        list.stream().map(tag -> toBlockPos((IntArrayTag) tag, LINKED_BLOCKS))
                                .collect(Collectors.toList()));
                return;
            }
        }
        tooltip(tooltip, "lt.notlinked");
        tooltip(tooltip, "lt.notlinked.msg");
    }
}
