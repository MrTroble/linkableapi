package com.troblecodings.linkableapi;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

public class MultiLinkingTool extends Item {

    private static final String LINKED_BLOCKS = "linkedBlocks";
    private static final String MULTILINKINGTOOL_TAG = "multiLinkingToolTag";

    private final BiPredicate<World, BlockPos> predicate;
    private final Predicate<TileEntity> predicateSet;
    private final TaggableFunction tagFromFunction;

    public MultiLinkingTool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate) {
        this(tab, predicate, _u -> true);
    }

    public MultiLinkingTool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<TileEntity> predicateSet) {
        this(tab, predicate, predicateSet, (_u1, _u2, _u3) -> {
        });
    }

    public MultiLinkingTool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<TileEntity> predicateSet, final TaggableFunction function) {
        super(new Properties().tab(tab).durability(64).setNoRepair());
        this.predicate = predicate;
        this.predicateSet = predicateSet;
        this.tagFromFunction = function;
    }

    @Override
    public ActionResultType onItemUseFirst(final ItemStack stack, final ItemUseContext ctx) {
        final World levelIn = ctx.getLevel();
        final PlayerEntity player = ctx.getPlayer();
        if (player == null)
            return ActionResultType.FAIL;
        final BlockPos pos = ctx.getClickedPos();
        if (levelIn.isClientSide)
            return ActionResultType.PASS;
        final TileEntity entity = levelIn.getBlockEntity(pos);
        final CompoundNBT itemTag = stack.getOrCreateTag();
        final CompoundNBT toolTag = itemTag.getCompound(MULTILINKINGTOOL_TAG);
        if (entity instanceof ILinkableTile && this.predicateSet.apply(entity)) {
            final ILinkableTile controller = (ILinkableTile) entity;
            if (!player.isShiftKeyDown()) {
                if (toolTag == null) {
                    message(player, "lt.notset", pos.toString());
                    return ActionResultType.PASS;
                }
                final ListNBT list = (ListNBT) toolTag.get(LINKED_BLOCKS);
                if (list == null) {
                    message(player, "lt.notlinked");
                    return ActionResultType.FAIL;
                }
                list.stream().map(tag -> NBTUtil.readBlockPos((CompoundNBT) tag))
                        .forEach(linkPos -> {
                            if (controller.link(linkPos, toolTag))
                                message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                        });
                removeToolTag(stack);
                message(player, "lt.reset");
                return ActionResultType.FAIL;
            } else {
                if (controller.canBeLinked() && predicate.test(levelIn, pos)) {
                    ListNBT list = (ListNBT) toolTag.get(LINKED_BLOCKS);
                    if (list == null)
                        list = new ListNBT();
                    list.add(NBTUtil.writeBlockPos(pos));
                    toolTag.put(LINKED_BLOCKS, list);
                    tagFromFunction.test(levelIn, pos, toolTag);
                    itemTag.put(MULTILINKINGTOOL_TAG, toolTag);
                    message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
                    message(player, "lt.setpos.msg");
                    return ActionResultType.SUCCESS;
                }
                if (controller.hasLink() && controller.unlink()) {
                    message(player, "lt.unlink");
                    return ActionResultType.SUCCESS;
                }
            }
            return ActionResultType.SUCCESS;
        } else if (predicate.test(levelIn, pos)) {
            ListNBT list = (ListNBT) toolTag.get(LINKED_BLOCKS);
            if (list == null)
                list = new ListNBT();
            list.add(NBTUtil.writeBlockPos(pos));
            toolTag.put(LINKED_BLOCKS, list);
            tagFromFunction.test(levelIn, pos, toolTag);
            itemTag.put(MULTILINKINGTOOL_TAG, toolTag);
            message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
            message(player, "lt.setpos.msg");
            return ActionResultType.SUCCESS;
        } else if (player.isShiftKeyDown() && stack.getTag() != null) {
            removeToolTag(stack);
            message(player, "lt.reset");
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.FAIL;
    }

    private void removeToolTag(final ItemStack stack) {
        stack.getOrCreateTag().remove(MULTILINKINGTOOL_TAG);
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final World levelIn,
            final List<ITextComponent> tooltip, final ITooltipFlag flagIn) {
        final CompoundNBT itemTag = stack.getOrCreateTag();
        final CompoundNBT toolTag = itemTag.getCompound(MULTILINKINGTOOL_TAG);
        if (toolTag != null) {
            final ListNBT list = (ListNBT) toolTag.get(LINKED_BLOCKS);
            if (list != null) {
                tooltip(tooltip, "lt.linkedpos",
                        list.stream().map(tag -> NBTUtil.readBlockPos((CompoundNBT) tag))
                                .collect(Collectors.toList()));
                return;
            }
        }
        tooltip(tooltip, "lt.notlinked");
        tooltip(tooltip, "lt.notlinked.msg");
    }

    public void tooltip(final List<ITextComponent> list, final String text, final Object... obj) {
        list.add(getComponent(text, obj));
    }

    public void message(final PlayerEntity player, final String text, final Object... obj) {
        player.sendMessage(getComponent(text, obj), player.getUUID());
    }

    public TextComponent getComponent(final String text, final Object... obj) {
        return new TranslationTextComponent(text, obj);
    }

}
