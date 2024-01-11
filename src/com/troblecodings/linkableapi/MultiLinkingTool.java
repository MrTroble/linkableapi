package com.troblecodings.linkableapi;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MultiLinkingTool extends Item {

    private static final String LINKED_BLOCKS = "linkedBlocks";
    private static final String MULTILINKINGTOOL_TAG = "multiLinkingToolTag";

    private final BiPredicate<Level, BlockPos> predicate;
    private final Predicate<BlockEntity> predicateSet;
    private final TaggableFunction tagFromFunction;

    public MultiLinkingTool(final CreativeModeTab tab,
            final BiPredicate<Level, BlockPos> predicate) {
        this(tab, predicate, _u -> true);
    }

    public MultiLinkingTool(final CreativeModeTab tab, final BiPredicate<Level, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet) {
        this(tab, predicate, predicateSet, (_u1, _u2, _u3) -> {
        });
    }

    public MultiLinkingTool(final CreativeModeTab tab, final BiPredicate<Level, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet, final TaggableFunction function) {
        super(new Properties().tab(tab).durability(64).setNoRepair());
        this.predicate = predicate;
        this.predicateSet = predicateSet;
        this.tagFromFunction = function;
    }

    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext ctx) {
        final Level levelIn = ctx.getLevel();
        final Player player = ctx.getPlayer();
        if (player == null)
            return InteractionResult.FAIL;
        final BlockPos pos = ctx.getClickedPos();
        if (levelIn.isClientSide)
            return InteractionResult.PASS;
        final BlockEntity entity = levelIn.getBlockEntity(pos);
        final CompoundTag itemTag = stack.getOrCreateTag();
        final CompoundTag toolTag = itemTag.getCompound(MULTILINKINGTOOL_TAG);
        if (entity instanceof ILinkableTile && this.predicateSet.apply(entity)) {
            final ILinkableTile controller = (ILinkableTile) entity;
            if (!player.isShiftKeyDown()) {
                if (toolTag == null) {
                    message(player, "lt.notset", pos.toString());
                    return InteractionResult.PASS;
                }
                final ListTag list = (ListTag) toolTag.get(LINKED_BLOCKS);
                if (list == null) {
                    message(player, "lt.notlinked");
                    return InteractionResult.FAIL;
                }
                list.stream().map(tag -> NbtUtils.readBlockPos((CompoundTag) tag))
                        .forEach(linkPos -> {
                            if (controller.link(linkPos, toolTag))
                                message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                        });
                removeToolTag(stack);
                message(player, "lt.reset");
                return InteractionResult.FAIL;
            } else {
                if (controller.canBeLinked() && predicate.test(levelIn, pos)) {
                    ListTag list = (ListTag) toolTag.get(LINKED_BLOCKS);
                    if (list == null)
                        list = new ListTag();
                    list.add(NbtUtils.writeBlockPos(pos));
                    toolTag.put(LINKED_BLOCKS, list);
                    tagFromFunction.test(levelIn, pos, toolTag);
                    itemTag.put(MULTILINKINGTOOL_TAG, toolTag);
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
            ListTag list = (ListTag) toolTag.get(LINKED_BLOCKS);
            if (list == null)
                list = new ListTag();
            list.add(NbtUtils.writeBlockPos(pos));
            toolTag.put(LINKED_BLOCKS, list);
            tagFromFunction.test(levelIn, pos, toolTag);
            itemTag.put(MULTILINKINGTOOL_TAG, toolTag);
            message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
            message(player, "lt.setpos.msg");
            return InteractionResult.SUCCESS;
        } else if (player.isShiftKeyDown() && stack.getTag() != null) {
            removeToolTag(stack);
            message(player, "lt.reset");
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    private void removeToolTag(final ItemStack stack) {
        stack.getOrCreateTag().remove(MULTILINKINGTOOL_TAG);
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level levelIn,
            final List<Component> tooltip, final TooltipFlag flagIn) {
        final CompoundTag itemTag = stack.getOrCreateTag();
        final CompoundTag toolTag = itemTag.getCompound(MULTILINKINGTOOL_TAG);
        if (toolTag != null) {
            final ListTag list = (ListTag) toolTag.get(LINKED_BLOCKS);
            if (list != null) {
                tooltip(tooltip, "lt.linkedpos",
                        list.stream().map(tag -> NbtUtils.readBlockPos((CompoundTag) tag))
                                .collect(Collectors.toList()));
                return;
            }
        }
        tooltip(tooltip, "lt.notlinked");
        tooltip(tooltip, "lt.notlinked.msg");
    }

    public void tooltip(final List<Component> list, final String text, final Object... obj) {
        list.add(getComponent(text, obj));
    }

    public void message(final Player player, final String text, final Object... obj) {
        player.sendMessage(getComponent(text, obj), player.getUUID());
    }

    public MutableComponent getComponent(final String text, final Object... obj) {
        return new TranslatableComponent(text, obj);
    }

}
