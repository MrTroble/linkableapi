package com.troblecodings.linkableapi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.INBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class MultiLinkingTool extends Linkingtool {

    private static final String MULTILINKINGTOOL_TAG = "multiLinkingToolTag";
    private static final String LINKED_BLOCKS = "linkedBlocks";

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
        super(tab, predicate, predicateSet, function);
    }

    @Override
    public EnumActionResult onItemUseFirst(final ItemStack stack, final ItemUseContext ctx) {
        final World levelIn = ctx.getWorld();
        final EntityPlayer player = ctx.getPlayer();
        if (player == null)
            return EnumActionResult.FAIL;
        if (levelIn.isRemote)
            return EnumActionResult.PASS;
        final BlockPos pos = ctx.getPos();
        final TileEntity entity = levelIn.getTileEntity(pos);
        final NBTTagCompound itemTag = getOrCreateForStack(stack);
        final NBTTagCompound toolTag = itemTag.getCompound(MULTILINKINGTOOL_TAG);
        if (entity instanceof ILinkableTile && this.predicateSet.apply(entity)) {
            final ILinkableTile controller = (ILinkableTile) entity;
            if (!player.isSneaking()) {
                if (toolTag == null) {
                    message(player, "lt.notset", pos.toString());
                    return EnumActionResult.PASS;
                }
                final NBTTagList list = (NBTTagList) toolTag.getTag(LINKED_BLOCKS);
                if (list == null) {
                    message(player, "lt.notlinked");
                    return EnumActionResult.FAIL;
                }
                list.stream().map(tag -> NBTUtil.readBlockPos((NBTTagCompound) tag))
                        .forEach(linkPos -> {
                            if (controller.link(linkPos, toolTag)) {
                                message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                            }
                        });
                removeToolTag(stack);
                message(player, "lt.reset");
                stack.damageItem(list.size(), player);
                return EnumActionResult.FAIL;
            } else {
                if (controller.canBeLinked() && predicate.test(levelIn, pos)) {
                    NBTTagList list = (NBTTagList) toolTag.getTag(LINKED_BLOCKS);
                    if (list == null) {
                        list = new NBTTagList();
                    }
                    final List<INBTBase> tagList = new ArrayList<>();
                    list.forEach(tagList::add);
                    final NBTTagCompound tag = NBTUtil.writeBlockPos(pos);
                    if (tagList.contains(tag)) {
                        message(player, "lt.setpos.msg");
                        return EnumActionResult.FAIL;
                    }
                    list.add(tag);
                    toolTag.setTag(LINKED_BLOCKS, list);
                    tagFromFunction.test(levelIn, pos, toolTag);
                    itemTag.setTag(MULTILINKINGTOOL_TAG, toolTag);
                    message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
                    message(player, "lt.setpos.msg");
                    return EnumActionResult.SUCCESS;
                }
                if (controller.hasLink() && controller.unlink()) {
                    message(player, "lt.unlink");
                    return EnumActionResult.SUCCESS;
                }
            }
            return EnumActionResult.SUCCESS;
        } else if (predicate.test(levelIn, pos)) {
            NBTTagList list = (NBTTagList) toolTag.getTag(LINKED_BLOCKS);
            if (list == null) {
                list = new NBTTagList();
            }
            final List<INBTBase> tagList = new ArrayList<>();
            list.forEach(tagList::add);
            final NBTTagCompound tag = NBTUtil.writeBlockPos(pos);
            if (tagList.contains(tag)) {
                message(player, "lt.setpos.msg");
                return EnumActionResult.FAIL;
            }
            list.add(tag);
            toolTag.setTag(LINKED_BLOCKS, list);
            tagFromFunction.test(levelIn, pos, toolTag);
            itemTag.setTag(MULTILINKINGTOOL_TAG, toolTag);
            message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
            message(player, "lt.setpos.msg");
            return EnumActionResult.SUCCESS;
        } else if (player.isSneaking() && stack.getTag() != null) {
            removeToolTag(stack);
            message(player, "lt.reset");
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.FAIL;
    }

    @Override
    public void removeToolTag(final ItemStack stack) {
        getOrCreateForStack(stack).removeTag(MULTILINKINGTOOL_TAG);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(final ItemStack stack, @Nullable final World levelIn,
            final List<ITextComponent> tooltip, final ITooltipFlag flagIn) {
        final NBTTagCompound itemTag = getOrCreateForStack(stack);
        final NBTTagCompound toolTag = itemTag.getCompound(MULTILINKINGTOOL_TAG);
        if (toolTag != null) {
            final NBTTagList list = (NBTTagList) toolTag.getTag(LINKED_BLOCKS);
            if (list != null) {
                tooltip(tooltip, "lt.linkedpos",
                        list.stream().map(tag -> NBTUtil.readBlockPos((NBTTagCompound) tag))
                                .collect(Collectors.toList()));
                return;
            }
        }
        tooltip(tooltip, "lt.notlinked");
        tooltip(tooltip, "lt.notlinked.msg");
    }
}
