package com.troblecodings.linkableapi;

import java.util.List;
import java.util.function.BiPredicate;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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

public class Linkingtool extends Item {

    final BiPredicate<World, BlockPos> predicate;

    public Linkingtool(final CreativeTabs tab, final BiPredicate<World, BlockPos> predicate) {
        setCreativeTab(tab);
        this.predicate = predicate;
    }

    @Override
    public EnumActionResult onItemUse(final EntityPlayer player, final World worldIn,
            final BlockPos pos, final EnumHand hand, final EnumFacing facing, final float hitX,
            final float hitY, final float hitZ) {
        if (worldIn.isRemote)
            return EnumActionResult.PASS;
        final TileEntity entity = worldIn.getTileEntity(pos);
        final ItemStack stack = player.getHeldItem(hand);
        if (entity instanceof ILinkableTile) {
            final ILinkableTile controller = ((ILinkableTile) worldIn.getTileEntity(pos));
            if (!player.isSneaking()) {
                final NBTTagCompound comp = stack.getTagCompound();
                if (comp == null) {
                    player.sendMessage(new TextComponentTranslation("lt.notset", pos.toString()));
                    return EnumActionResult.PASS;
                }
                final BlockPos lpos = NBTUtil.getPosFromTag(comp);
                if (controller.link(lpos)) {
                    player.sendMessage(new TextComponentTranslation("lt.linkedpos", pos.getX(),
                            pos.getY(), pos.getZ()));
                    stack.setTagCompound(null);
                    player.sendMessage(new TextComponentTranslation("lt.reset"));
                    return EnumActionResult.FAIL;
                }
                player.sendMessage(new TextComponentTranslation("lt.notlinked"));
                player.sendMessage(new TextComponentTranslation("lt.notlinked.msg"));
                return EnumActionResult.FAIL;
            } else {
                if (controller.hasLink() && controller.unlink()) {
                    player.sendMessage(new TextComponentTranslation("lt.unlink"));
                }
            }
            return EnumActionResult.SUCCESS;
        } else if (predicate.test(worldIn, pos)) {
            if (stack.getTagCompound() != null) {
                player.sendMessage(new TextComponentTranslation("lt.setpos.msg"));
                return EnumActionResult.FAIL;
            }
            final NBTTagCompound comp = NBTUtil.createPosTag(pos);
            stack.setTagCompound(comp);
            player.sendMessage(
                    new TextComponentTranslation("lt.setpos", pos.getX(), pos.getY(), pos.getZ()));
            player.sendMessage(new TextComponentTranslation("lt.setpos.msg"));
            return EnumActionResult.SUCCESS;
        } else if (player.isSneaking() && stack.getTagCompound() != null) {
            stack.setTagCompound(null);
            player.sendMessage(new TextComponentTranslation("lt.reset"));
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.FAIL;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(final ItemStack stack, final World worldIn,
            final List<String> tooltip, final ITooltipFlag flagIn) {
        final NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null) {
            final BlockPos pos = NBTUtil.getPosFromTag(nbt);
            if (pos != null) {
                tooltip.add(I18n.format("lt.linkedpos", pos.getX(), pos.getY(), pos.getZ()));
                return;
            }
        }

        tooltip.add(I18n.format("lt.notlinked"));
        tooltip.add(I18n.format("lt.notlinked.msg"));
    }
}
