package com.troblecodings.linkableapi;

import java.util.List;
import java.util.function.BiPredicate;

import com.google.common.base.Predicate;

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
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class Linkingtool extends Item {

    private final BiPredicate<World, BlockPos> predicate;
    private final Predicate<TileEntity> predicateSet;
    private final TaggableFunction tagFromFunction;

    public Linkingtool(final CreativeTabs tab, final BiPredicate<World, BlockPos> predicate) {
        this(tab, predicate, _u -> true);
    }

    public Linkingtool(final CreativeTabs tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<TileEntity> predicateSet) {
        this(tab, predicate, predicateSet, (_u1, _u2, _u3) -> {
        });
    }

    public Linkingtool(final CreativeTabs tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<TileEntity> predicateSet, final TaggableFunction function) {
        this.predicate = predicate;
        this.predicateSet = predicateSet;
        this.tagFromFunction = function;
        setCreativeTab(tab);
        setNoRepair();
        setMaxDamage(64);
        setMaxStackSize(1);
    }

    @Override
    public EnumActionResult onItemUse(final EntityPlayer player, final World worldIn, final BlockPos pos,
            final EnumHand hand, final EnumFacing facing, final float hitX, final float hitY, final float hitZ) {
        if (player == null)
            return EnumActionResult.FAIL;
        if (worldIn.isRemote)
            return EnumActionResult.PASS;
        final TileEntity entity = worldIn.getTileEntity(pos);
        final ItemStack stack = player.getHeldItem(hand);
        if (entity instanceof ILinkableTile && this.predicateSet.apply(entity)) {
            final ILinkableTile controller = (ILinkableTile) entity;
            if (!player.isSneaking()) {
                final NBTTagCompound comp = stack.getTagCompound();
                if (comp == null) {
                    message(player, "lt.notset", pos.toString());
                    return EnumActionResult.PASS;
                }
                final BlockPos linkedPos = NBTUtil.getPosFromTag(comp);
                if (controller.link(linkedPos, comp)) {
                    message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                    stack.setTagCompound(null);
                    message(player, "lt.reset");
                    return EnumActionResult.FAIL;
                }
                message(player, "lt.notlinked");
                message(player, "lt.notlinked.msg");
                return EnumActionResult.FAIL;
            } else {
                if (controller.hasLink() && controller.unlink()) {
                    message(player, "lt.unlink");
                }
            }
            return EnumActionResult.SUCCESS;
        } else if (predicate.test(worldIn, pos)) {
            final NBTTagCompound tag = stack.getTagCompound();
            if (tag != null) {
                final boolean containsPos = tag.hasKey("X") && tag.hasKey("Y") && tag.hasKey("Z");
                if (containsPos) {
                    message(player, "lt.setpos.msg");
                    return EnumActionResult.FAIL;
                }
            }
            final NBTTagCompound comp = NBTUtil.createPosTag(pos);
            tagFromFunction.test(worldIn, pos, comp);
            stack.setTagCompound(comp);
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

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(final ItemStack stack, final World worldIn, final List<String> tooltip,
            final ITooltipFlag flagIn) {
        final NBTTagCompound tag = stack.getTagCompound();
        if (tag != null) {
            final boolean containsPos = tag.hasKey("X") && tag.hasKey("Y") && tag.hasKey("Z");
            if (containsPos) {
                final BlockPos pos = NBTUtil.getPosFromTag(tag);
                tooltip.add(I18n.format("lt.linkedpos", pos.getX(), pos.getY(), pos.getZ()));
                return;
            }
        }
        tooltip.add(I18n.format("lt.notlinked"));
        tooltip.add(I18n.format("lt.notlinked.msg"));
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

    public ITextComponent getComponent(final String text, final Object... obj) {
        return new TextComponentTranslation(text, obj);
    }
}
