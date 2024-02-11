package com.troblecodings.linkableapi;

import java.util.List;
import java.util.function.BiPredicate;

import com.google.common.base.Predicate;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Linkingtool extends Item implements Message {

    private final BiPredicate<World, BlockPos> predicate;
    private final Predicate<BlockEntity> predicateSet;

    public Linkingtool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate) {
        this(tab, predicate, _u -> true);
    }

    public Linkingtool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet) {
        super(new FabricItemSettings().group(tab));
        this.predicate = predicate;
        this.predicateSet = predicateSet;
    }
    
    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        final World levelIn = ctx.getWorld();
        final PlayerEntity player = ctx.getPlayer();
        final BlockPos pos = ctx.getBlockPos();
        final ItemStack stack = ctx.getStack();
        if (levelIn.isClient)
            return ActionResult.PASS;
        final BlockEntity entity = levelIn.getBlockEntity(pos);
        if (entity instanceof ILinkableTile && this.predicateSet.apply(entity)) {
            final ILinkableTile controller = (ILinkableTile) entity;
            if (!player.isSneaking()) {
                final CompoundTag comp = stack.getTag();
                if (comp == null) {
                    message(player, "lt.notset", pos.toString());
                    return ActionResult.PASS;
                }
                final BlockPos lpos = NbtHelper.toBlockPos(comp);
                if (controller.link(lpos)) {
                    message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                    stack.setTag(null);
                    message(player, "lt.reset");
                    return ActionResult.FAIL;
                }
                message(player, "lt.notlinked");
                message(player, "lt.notlinked.msg");
                return ActionResult.FAIL;
            } else {
                if (controller.hasLink() && controller.unlink()) {
                    message(player, "lt.unlink");
                }
            }
            return ActionResult.SUCCESS;
        } else if (predicate.test(levelIn, pos)) {
            if (stack.getTag() != null) {
                message(player, "lt.setpos.msg");
                return ActionResult.FAIL;
            }
            final CompoundTag comp = NbtHelper.fromBlockPos(pos);
            stack.setTag(comp);
            message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
            message(player, "lt.setpos.msg");
            return ActionResult.SUCCESS;
        } else if (player.isSneaking() && stack.getTag() != null) {
            stack.setTag(null);
            message(player, "lt.reset");
            return ActionResult.SUCCESS;
        }
    	return ActionResult.FAIL;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
    	final CompoundTag nbt = stack.getTag();
        if (nbt != null) {
            final BlockPos pos = NbtHelper.toBlockPos(nbt);
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

}
