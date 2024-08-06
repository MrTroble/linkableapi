package com.troblecodings.linkableapi;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import com.google.common.base.Predicate;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MultiLinkingTool extends Linkingtool {

    private static final String LINKED_BLOCKS = "linkedBlocks";
    private static final String MULTILINKINGTOOL_TAG = "multiLinkingToolTag";

    public MultiLinkingTool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate) {
        super(tab, predicate);
    }

    public MultiLinkingTool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet, final TaggableFunction function) {
        super(tab, predicate, predicateSet, function);
    }

    public MultiLinkingTool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet) {
        super(tab, predicate, predicateSet);
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
        final ItemStack stack = ctx.getStack();
        final BlockEntity entity = levelIn.getBlockEntity(pos);
        final NbtCompound itemTag = stack.getOrCreateNbt();
        final NbtCompound toolTag = itemTag.getCompound(MULTILINKINGTOOL_TAG);
        if (entity instanceof ILinkableTile && this.predicateSet.apply(entity)) {
            final ILinkableTile controller = (ILinkableTile) entity;
            if (!player.isSneaking()) {
                if (toolTag == null) {
                    message(player, "lt.notset", pos.toString());
                    return ActionResult.PASS;
                }
                final NbtList list = (NbtList) toolTag.get(LINKED_BLOCKS);
                if (list == null) {
                    message(player, "lt.notlinked");
                    return ActionResult.FAIL;
                }
                list.stream().map(tag -> NbtHelper.toBlockPos((NbtCompound) tag))
                        .forEach(linkPos -> {
                            if (controller.link(linkPos, toolTag)) {
                                message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                            }
                        });
                removeToolTag(stack);
                message(player, "lt.reset");
                stack.damage(1, player, (user) -> user.sendToolBreakStatus(ctx.getHand()));
                return ActionResult.FAIL;
            } else {
                if (controller.canBeLinked() && predicate.test(levelIn, pos)) {
                    NbtList list = (NbtList) toolTag.get(LINKED_BLOCKS);
                    if (list == null) {
                        list = new NbtList();
                    }
                    final NbtCompound posTag = NbtHelper.fromBlockPos(pos);
                    if (list.contains(posTag)) {
                        message(player, "lt.setpos.msg");
                        return ActionResult.FAIL;
                    }
                    list.add(NbtHelper.fromBlockPos(pos));
                    toolTag.put(LINKED_BLOCKS, list);
                    tagFromFunction.test(levelIn, pos, toolTag);
                    itemTag.put(MULTILINKINGTOOL_TAG, toolTag);
                    message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
                    message(player, "lt.setpos.msg");
                    return ActionResult.SUCCESS;
                }
                if (controller.hasLink() && controller.unlink()) {
                    message(player, "lt.unlink");
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.SUCCESS;
        } else if (predicate.test(levelIn, pos)) {
            NbtList list = (NbtList) toolTag.get(LINKED_BLOCKS);
            if (list == null) {
                list = new NbtList();
            }
            list.add(NbtHelper.fromBlockPos(pos));
            toolTag.put(LINKED_BLOCKS, list);
            tagFromFunction.test(levelIn, pos, toolTag);
            itemTag.put(MULTILINKINGTOOL_TAG, toolTag);
            message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
            message(player, "lt.setpos.msg");
            return ActionResult.SUCCESS;
        } else if (player.isSneaking()) {
            removeToolTag(stack);
            message(player, "lt.reset");
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    @Override
    public void removeToolTag(final ItemStack stack) {
        stack.getOrCreateNbt().remove(MULTILINKINGTOOL_TAG);
    }

    @Override
    public void appendTooltip(final ItemStack stack, final World world, final List<Text> tooltip,
            final TooltipContext context) {
        final NbtCompound itemTag = stack.getOrCreateNbt();
        final NbtCompound toolTag = itemTag.getCompound(MULTILINKINGTOOL_TAG);
        if (toolTag != null) {
            final NbtList list = (NbtList) toolTag.get(LINKED_BLOCKS);
            if (list != null) {
                tooltip(tooltip, "lt.linkedpos",
                        list.stream().map(tag -> NbtHelper.toBlockPos((NbtCompound) tag))
                                .collect(Collectors.toList()));
                return;
            }
        }
        tooltip(tooltip, "lt.notlinked");
        tooltip(tooltip, "lt.notlinked.msg");
    }
}
