package com.troblecodings.linkableapi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import net.minecraft.client.resources.I18n;
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

    private static final String MULTILINKINGTOOL_TAG = "multiLinkingToolTag";
    private static final String LINKED_BLOCKS = "linkedBlocks";

    private final BiPredicate<World, BlockPos> predicate;
    private final Predicate<TileEntity> predicateSet;
    private final TaggableFunction tagFromFunction;

    public MultiLinkingTool(final CreativeTabs tab, final BiPredicate<World, BlockPos> predicate) {
        this(tab, predicate, _u -> true);
    }

    public MultiLinkingTool(final CreativeTabs tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<TileEntity> predicateSet, final TaggableFunction function) {
        setCreativeTab(tab);
        setNoRepair();
        setMaxDamage(64);
        setMaxStackSize(1);
        this.predicate = predicate;
        this.predicateSet = predicateSet;
        this.tagFromFunction = function;

    }

    public MultiLinkingTool(final CreativeTabs tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<TileEntity> predicateSet) {
        this(tab, predicate, predicateSet, (_u1, _u2, _u3) -> {
        });
    }

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
        final NBTTagCompound itemTag = getOrCreateForStack(stack);
        final NBTTagCompound toolTag = itemTag.getCompoundTag(MULTILINKINGTOOL_TAG);
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
                list.forEach(tag -> {
                    if (controller.link(NBTUtil.getPosFromTag((NBTTagCompound) tag), toolTag))
                        message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                });
                removeToolTag(stack);
                message(player, "lt.reset");
                return EnumActionResult.FAIL;
            } else {
                if (controller.canBeLinked() && predicate.test(levelIn, pos)) {
                    NBTTagList list = (NBTTagList) toolTag.getTag(LINKED_BLOCKS);
                    if (list == null)
                        list = new NBTTagList();
                    list.appendTag(NBTUtil.createPosTag(pos));
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
            if (list == null)
                list = new NBTTagList();
            list.appendTag(NBTUtil.createPosTag(pos));
            toolTag.setTag(LINKED_BLOCKS, list);
            tagFromFunction.test(levelIn, pos, toolTag);
            itemTag.setTag(MULTILINKINGTOOL_TAG, toolTag);
            message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
            message(player, "lt.setpos.msg");
            return EnumActionResult.SUCCESS;
        } else if (player.isSneaking() && stack.getTagCompound() != null) {
            removeToolTag(stack);
            message(player, "lt.reset");
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.FAIL;
    }

    private void removeToolTag(final ItemStack stack) {
        getOrCreateForStack(stack).removeTag(MULTILINKINGTOOL_TAG);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(final ItemStack stack, @Nullable final World levelIn,
            final List<String> tooltip, final ITooltipFlag flagIn) {
        final NBTTagCompound itemTag = getOrCreateForStack(stack);
        final NBTTagCompound toolTag = itemTag.getCompoundTag(MULTILINKINGTOOL_TAG);
        if (toolTag != null) {
            final NBTTagList list = (NBTTagList) toolTag.getTag(LINKED_BLOCKS);
            if (list != null) {
                final List<BlockPos> linkedPos = new ArrayList<>();
                list.forEach(tag -> linkedPos.add(NBTUtil.getPosFromTag((NBTTagCompound) tag)));
                tooltip.add(linkedPos.toString());
                return;
            }
        }
        tooltip.add(I18n.format("lt.notlinked"));
        tooltip.add(I18n.format("lt.notlinked.msg"));
    }

    private static NBTTagCompound getOrCreateForStack(final ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        return tag;
    }

    public void message(final EntityPlayer player, final String text, final Object... obj) {
        player.sendMessage(getComponent(text, obj));
    }

    public TextComponentTranslation getComponent(final String text, final Object... obj) {
        return new TextComponentTranslation(text, obj);
    }
}