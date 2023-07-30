package com.troblecodings.linkableapi;

import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MultiLinkingTool extends Item {

    private static final String LINKED_BLOCKS = "linkedBlocks";

    private final BiPredicate<World, BlockPos> predicate;
    private final Predicate<TileEntity> predicateSet;
    private final TaggableFunction tagFromFunction;


    public MultiLinkingTool(final CreativeTabs tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<TileEntity> predicateSet, final TaggableFunction function) {
        setCreativeTab(tab);
        this.predicate = predicate;
        this.predicateSet = predicateSet;
        this.tagFromFunction = function;
    }

    @SuppressWarnings({
            "rawtypes", "unchecked"
    })
    @Override
    public EnumActionResult onItemUse(final EntityPlayer player, final World levelIn,
            final BlockPos pos, final EnumHand hand, final EnumFacing facing, final float hitX,
            final float hitY, final float hitZ) {
        if (player == null)
            return EnumActionResult.FAIL;
        if (levelIn.isRemote)
            return EnumActionResult.PASS;
        final TileEntity entity = levelIn.getTileEntity(pos);
        final ItemStack stack = player.getHeldItem(hand);
        if (entity instanceof ILinkableTile && this.predicateSet.apply(entity)) {
            final ILinkableTile controller = (ILinkableTile) entity;
            if (!player.isSneaking()) {
                final NBTTagCompound comp = stack.getTagCompound();
                if (comp == null) {
                    message(player, "lt.notset", pos.toString());
                    return EnumActionResult.PASS;
                }
                final NBTTagList list = (NBTTagList) comp.getTag(LINKED_BLOCKS);
                if (list == null) {
                    message(player, "lt.notlinked");
                    return EnumActionResult.FAIL;
                }
                ((Collection) list).stream().map(tag -> NBTUtil.getPosFromTag((NBTTagCompound) tag))
                        .forEach(linkPos -> {
                            if (controller.link(linkPos))
                                message(player, "lt.linkedpos", pos.getX(),
                                        pos.getY(), pos.getZ());
                        });
                stack.setTagCompound(null);
                message(player, "lt.reset");
                return EnumActionResult.FAIL;
            } else {
                if (controller.hasLink() && controller.unlink()) {
                    message(player, "lt.unlink");
                }
            }
            return EnumActionResult.SUCCESS;
        } else if (predicate.test(levelIn, pos)) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null)
                tag = new NBTTagCompound();
            NBTTagList list = (NBTTagList) tag.getTag(LINKED_BLOCKS);
            if (list == null)
                list = new NBTTagList();
            list.appendTag(NBTUtil.createPosTag(pos));
            tag.setTag(LINKED_BLOCKS, list);
            tagFromFunction.test(levelIn, pos, tag);
            stack.setTagCompound(tag);
            message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
            message(player, "lt.setpos.msg");
            return EnumActionResult.SUCCESS;
        } else if (player.isSneaking() && stack.getTagCompound() != null) {
            stack.setTagCompound(null);
            message(player, "lt.reset");
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.FAIL;
    }

    @SuppressWarnings({
            "unchecked", "rawtypes"
    })
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(final ItemStack stack, @Nullable final World levelIn,
            final List<String> tooltip, final ITooltipFlag flagIn) {
        final NBTTagCompound itemTag = stack.getTagCompound();
        if (itemTag != null) {
            final NBTTagList list = (NBTTagList) itemTag.getTag(LINKED_BLOCKS);
            if (list != null) {
                tooltip(tooltip, "lt.linkedpos",
                        ((Collection) list).stream().map(tag -> NBTUtil.getPosFromTag((NBTTagCompound) tag))
                                .collect(Collectors.toList()));
                return;
            }
        }
        tooltip(tooltip, "lt.notlinked");
        tooltip(tooltip, "lt.notlinked.msg");
    }

    @SuppressWarnings({
            "rawtypes", "unchecked"
    })
    public void tooltip(final List list, final String text, final Object... obj) {
        list.add(getComponent(text, obj));
    }

    public void message(final EntityPlayer player, final String text, final Object... obj) {
        player.sendMessage(getComponent(text, obj));
    }

    public TextComponentTranslation getComponent(final String text, final Object... obj) {
        return new TextComponentTranslation(text, obj);
    }

}
