package com.troblecodings.linkableapi;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

import com.google.common.base.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MultiLinkingTool extends Linkingtool implements Message {

    private static final String LINKED_BLOCKS = "linkedBlocks";
    // private static final String MULTILINKINGTOOL_TAG = "multiLinkingToolTag";

    public MultiLinkingTool(final CreativeModeTab tab, final BiPredicate<Level, BlockPos> predicate,
            final DataComponentType<CompoundTag> data) {
        super(tab, predicate, data);
    }

    public MultiLinkingTool(final CreativeModeTab tab, final BiPredicate<Level, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet, final TaggableFunction function,
            final DataComponentType<CompoundTag> data) {
        super(tab, predicate, predicateSet, function, data);
    }

    public MultiLinkingTool(final CreativeModeTab tab, final BiPredicate<Level, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet, final DataComponentType<CompoundTag> data) {
        super(tab, predicate, predicateSet, data);
    }

    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext ctx) {
        final Player player = ctx.getPlayer();
        if (player == null)
            return InteractionResult.FAIL;
        final Level levelIn = ctx.getLevel();
        if (levelIn.isClientSide)
            return InteractionResult.PASS;
        final BlockPos pos = ctx.getClickedPos();
        final BlockEntity entity = levelIn.getBlockEntity(pos);
        final CompoundTag itemTag = getOrCreateForStack(stack);
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

                list.stream().map(tag -> readBlockPos((IntArrayTag) tag, LINKED_BLOCKS))
                        .forEach(linkPos -> {
                            if (controller.link(linkPos, itemTag)) {
                                message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                            }
                        });
                removeToolTag(stack);
                message(player, "lt.reset");
                stack.hurtAndBreak(list.size(), player, getEquipmentSlot(stack));
            } else {
                if (controller.canBeLinked() && predicate.test(levelIn, pos)) {
                    ListTag tagList = (ListTag) itemTag.get(LINKED_BLOCKS);
                    if (tagList == null) {
                        tagList = new ListTag();
                    }
                    final Tag posTag = NbtUtils.writeBlockPos(pos);
                    if (tagList.contains(posTag)) {
                        message(player, "lt.setpos.msg");
                        return InteractionResult.FAIL;
                    }
                    tagList.add(NbtUtils.writeBlockPos(pos));
                    tagFromFunction.test(levelIn, pos, tagList);
                    itemTag.put(LINKED_BLOCKS, tagList);
                    stack.set(compoundData, itemTag);
                    message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
                    message(player, "lt.setpos.msg");
                    return InteractionResult.SUCCESS;
                }
                if (controller.hasLink() && controller.unlink()) {
                    message(player, "lt.unlink");
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (predicate.test(levelIn, pos)) {
            ListTag tagList = (ListTag) itemTag.get(LINKED_BLOCKS);
            if (tagList == null) {
                tagList = new ListTag();
            }
            final Tag posTag = NbtUtils.writeBlockPos(pos);
            if (tagList.contains(posTag)) {
                message(player, "lt.setpos.msg");
                return InteractionResult.FAIL;
            }
            tagList.add(posTag);
            tagFromFunction.test(levelIn, pos, tagList);
            itemTag.put(LINKED_BLOCKS, tagList);
            stack.set(compoundData, itemTag);
            message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
            message(player, "lt.setpos.msg");
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            removeToolTag(stack);
            message(player, "lt.reset");
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void removeToolTag(final ItemStack stack) {
        stack.remove(compoundData);
    }

    public static Optional<BlockPos> readBlockPos(final IntArrayTag tag, final String string) {
        int[] aint = tag.getAsIntArray();
        return aint.length == 3 ? Optional.of(new BlockPos(aint[0], aint[1], aint[2]))
                : Optional.empty();
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext ctx,
            final List<Component> tooltip, final TooltipFlag flagIn) {
        final CompoundTag itemTag = getOrCreateForStack(stack);
        if (itemTag.contains(LINKED_BLOCKS)) {
            final ListTag list = (ListTag) itemTag.get(LINKED_BLOCKS);
            if (list != null) {
                list.stream().map(tag -> readBlockPos((IntArrayTag) tag, LINKED_BLOCKS).get())
                        .forEach(pos -> {
                            tooltip.add(Component.translatable("lt.linkedpos",
                                    Component.literal(String.valueOf(pos.getX())),
                                    Component.literal(String.valueOf(pos.getY())),
                                    Component.literal(String.valueOf(pos.getZ()))));
                        });
            }
            return;
        }
        tooltip(tooltip, "lt.notlinked");
        tooltip(tooltip, "lt.notlinked.msg");

    }
}
