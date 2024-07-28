package com.troblecodings.linkableapi;

import java.util.List;
import java.util.function.BiPredicate;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class Linkingtool extends Item {

    protected static final String LINKINGTOOL_TAG = "linkingToolTag";

    protected final BiPredicate<World, BlockPos> predicate;
    protected final Predicate<TileEntity> predicateSet;
    protected final TaggableFunction tagFromFunction;

    public Linkingtool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate) {
        this(tab, predicate, _u -> true);
    }

    public Linkingtool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<TileEntity> predicateSet) {
        this(tab, predicate, predicateSet, (_u1, _u2, _u3) -> {
        });
    }

    public Linkingtool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<TileEntity> predicateSet, final TaggableFunction function) {
        super(new Properties().group(tab).setNoRepair().defaultMaxDamage(64));
        this.predicate = predicate;
        this.predicateSet = predicateSet;
        this.tagFromFunction = function;
    }

    @Override
    public EnumActionResult onItemUseFirst(final ItemStack stack, final ItemUseContext ctx) {
        final EntityPlayer player = ctx.getPlayer();
        if (player == null)
            return EnumActionResult.FAIL;
        final World levelIn = ctx.getWorld();
        if (levelIn.isRemote)
            return EnumActionResult.PASS;
        final BlockPos pos = ctx.getPos();
        final TileEntity entity = levelIn.getTileEntity(pos);
        final NBTTagCompound itemTag = getOrCreateForStack(stack);
        final NBTTagCompound toolTag = itemTag.getCompound(LINKINGTOOL_TAG);
        if (entity instanceof ILinkableTile && this.predicateSet.apply(entity)) {
            final ILinkableTile controller = (ILinkableTile) entity;
            if (!player.isSneaking()) {
                if (toolTag == null) {
                    message(player, "lt.notset", pos.toString());
                    return EnumActionResult.PASS;
                }
                final BlockPos lpos = NBTUtil.readBlockPos(toolTag);
                if (controller.link(lpos, toolTag)) {
                    message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                    removeToolTag(stack);
                    message(player, "lt.reset");
                    stack.damageItem(1, player);
                    return EnumActionResult.FAIL;
                }
                message(player, "lt.notlinked");
                message(player, "lt.notlinked.msg");
                return EnumActionResult.FAIL;
            } else {
                if (controller.canBeLinked() && predicate.test(levelIn, pos)) {
                    final boolean containsPos =
                            toolTag.hasKey("X") && toolTag.hasKey("Y") && toolTag.hasKey("Z");
                    if (containsPos) {
                        message(player, "lt.setpos.msg");
                        return EnumActionResult.FAIL;
                    }
                    final NBTTagCompound newToolTag = NBTUtil.writeBlockPos(pos);
                    tagFromFunction.test(levelIn, pos, newToolTag);
                    itemTag.setTag(LINKINGTOOL_TAG, newToolTag);
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
            final boolean containsPos =
                    toolTag.hasKey("X") && toolTag.hasKey("Y") && toolTag.hasKey("Z");
            if (containsPos) {
                message(player, "lt.setpos.msg");
                return EnumActionResult.FAIL;
            }
            final NBTTagCompound newToolTag = NBTUtil.writeBlockPos(pos);
            tagFromFunction.test(levelIn, pos, newToolTag);
            itemTag.setTag(LINKINGTOOL_TAG, newToolTag);
            message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
            message(player, "lt.setpos.msg");
            return EnumActionResult.SUCCESS;
        } else if (player.isSneaking()) {
            removeToolTag(stack);
            message(player, "lt.reset");
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.FAIL;
    }

    public void removeToolTag(final ItemStack stack) {
        getOrCreateForStack(stack).removeTag(LINKINGTOOL_TAG);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(final ItemStack stack, final World levelIn,
            final List<ITextComponent> tooltip, final ITooltipFlag flagIn) {
        final NBTTagCompound nbt = getOrCreateForStack(stack);
        if (nbt.hasKey(LINKINGTOOL_TAG)) {
            final NBTTagCompound comp = nbt.getCompound(LINKINGTOOL_TAG);
            final boolean containsPos = comp.hasKey("X") && comp.hasKey("Y") && comp.hasKey("Z");
            if (containsPos) {
                final BlockPos pos = NBTUtil.readBlockPos(nbt);
                tooltip(tooltip, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                return;
            }
        }
        tooltip(tooltip, "lt.notlinked");
        tooltip(tooltip, "lt.notlinked.msg");
    }

    public static NBTTagCompound getOrCreateForStack(final ItemStack stack) {
        NBTTagCompound tag = stack.getTag();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTag(tag);
        }
        return tag;
    }

    public void tooltip(final List<ITextComponent> list, final String text, final Object... obj) {
        list.add(getComponent(text, obj));
    }

    public void message(final EntityPlayer player, final String text, final Object... obj) {
        player.sendMessage(getComponent(text, obj));
    }

    public TextComponentTranslation getComponent(final String text, final Object... obj) {
        return new TextComponentTranslation(text, obj);
    }
}
