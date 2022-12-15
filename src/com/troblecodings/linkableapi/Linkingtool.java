package com.troblecodings.linkableapi;

import java.util.List;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class Linkingtool extends Item {

    private final BiPredicate<World, BlockPos> predicate;
    private final Predicate<TileEntity> predicateSet;

    public Linkingtool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate) {
        this(tab, predicate, _u -> true);
    }

    public Linkingtool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<TileEntity> predicateSet) {
        super(new Properties().group(tab));
        this.predicate = predicate;
        this.predicateSet = predicateSet;
    }

    @Override
    public EnumActionResult onItemUseFirst(final ItemStack stack, final ItemUseContext ctx) {
        final World levelIn = ctx.getWorld();
        final EntityPlayer player = ctx.getPlayer();
        final BlockPos pos = ctx.getPos();
        if (!levelIn.isRemote)
            return EnumActionResult.PASS;
        final TileEntity entity = levelIn.getTileEntity(pos);
        if (entity instanceof ILinkableTile && this.predicateSet.apply(entity)) {
            final ILinkableTile controller = (ILinkableTile) entity;
            if (!player.isSneaking()) {
                final NBTTagCompound comp = stack.getTag();
                if (comp == null) {
                    message(player, "lt.notset", pos.toString());
                    return EnumActionResult.PASS;
                }
                final BlockPos lpos = NBTUtil.readBlockPos(comp);
                if (controller.link(lpos)) {
                    message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                    stack.setTag(null);
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
        } else if (predicate.test(levelIn, pos)) {
            if (stack.getTag() != null) {
                message(player, "lt.setpos.msg");
                return EnumActionResult.FAIL;
            }
            final NBTTagCompound comp = NBTUtil.writeBlockPos(pos);
            stack.setTag(comp);
            message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
            message(player, "lt.setpos.msg");
            return EnumActionResult.SUCCESS;
        } else if (player.isSneaking() && stack.getTag() != null) {
            stack.setTag(null);
            message(player, "lt.reset");
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.FAIL;
    }

    @Override
    public void addInformation(final ItemStack stack, @Nullable final World levelIn,
            final List<ITextComponent> tooltip, final ITooltipFlag flagIn) {
        final NBTTagCompound nbt = stack.getTag();
        if (nbt != null) {
            final BlockPos pos = NBTUtil.readBlockPos(nbt);
            if (pos != null) {
                tooltip(tooltip, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
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
