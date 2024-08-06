package com.troblecodings.linkableapi;

import java.util.List;
import java.util.function.BiPredicate;

import com.google.common.base.Predicate;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Linkingtool extends Item implements Message {

    protected static final String LINKINGTOOL_TAG = "linkingToolTag";

    protected final BiPredicate<World, BlockPos> predicate;
    protected final Predicate<BlockEntity> predicateSet;
    protected final ItemGroups tab;
    protected final TaggableFunction tagFromFunction;

    public Linkingtool(final ItemGroups tab, final BiPredicate<World, BlockPos> predicate) {
        this(tab, predicate, _u -> true);
    }

    public Linkingtool(final ItemGroups tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet) {
        this(tab, predicate, predicateSet, (_u1, _u2, _u3) -> {
        });
    }

    public Linkingtool(final ItemGroups tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet, final TaggableFunction function) {
        super(new FabricItemSettings().maxDamage(10));
        this.predicate = predicate;
        this.predicateSet = predicateSet;
        this.tab = tab;
        this.tagFromFunction = function;
    }

    @Override
    public ActionResult useOnBlock(final ItemUsageContext ctx) {
        final PlayerEntity player = ctx.getPlayer();
        if (player == null)
            return ActionResult.FAIL;
        final World levelIn = ctx.getWorld();
        if (levelIn.isClient())
            return ActionResult.PASS;
        final BlockPos pos = ctx.getBlockPos();
        final ItemStack stack = ctx.getStack();
        final BlockEntity entity = levelIn.getBlockEntity(pos);
        final NbtCompound itemTag = stack.getOrCreateNbt();
        final NbtCompound toolTag = itemTag.getCompound(LINKINGTOOL_TAG);
        if (entity instanceof ILinkableTile && this.predicateSet.apply(entity)) {
            final ILinkableTile controller = (ILinkableTile) entity;
            if (!player.isSneaking()) {
                if (toolTag == null) {
                    message(player, "lt.notset", pos.toString());
                    return ActionResult.PASS;
                }
                final BlockPos lpos = NbtHelper.toBlockPos(toolTag);
                if (controller.link(lpos, toolTag)) {
                    message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                    removeToolTag(stack);
                    message(player, "lt.reset");
                    stack.damage(1, player, (user) -> user.sendToolBreakStatus(ctx.getHand()));
                    return ActionResult.FAIL;
                }
                message(player, "lt.notlinked");
                message(player, "lt.notlinked.msg");
                return ActionResult.FAIL;
            } else {
                if (controller.canBeLinked() && predicate.test(levelIn, pos)) {
                    final boolean containsPos =
                            toolTag.contains("X") && toolTag.contains("Y") && toolTag.contains("Z");
                    if (containsPos) {
                        message(player, "lt.setpos.msg");
                        return ActionResult.FAIL;
                    }

                    final NbtCompound newToolTag = NbtHelper.fromBlockPos(pos);
                    tagFromFunction.test(levelIn, pos, newToolTag);
                    itemTag.put(LINKINGTOOL_TAG, newToolTag);
                    message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
                    message(player, "lt.setpos.msg");
                    return ActionResult.SUCCESS;
                }
                if (controller.hasLink() && controller.unlink()) {
                    message(player, "lt.unlink");
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.SUCCESS;
        } else if (predicate.test(levelIn, pos)) {
            final boolean containsPos =
                    toolTag.contains("X") && toolTag.contains("Y") && toolTag.contains("Z");
            if (containsPos) {
                message(player, "lt.setpos.msg");
                return ActionResult.FAIL;
            }
            final NbtCompound newToolTag = NbtHelper.fromBlockPos(pos);
            tagFromFunction.test(levelIn, pos, newToolTag);
            itemTag.put(LINKINGTOOL_TAG, newToolTag);
            message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
            message(player, "lt.setpos.msg");
            return ActionResult.SUCCESS;
        } else if (player.isSneaking()) {
            removeToolTag(stack);
            message(player, "lt.reset");
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    public void removeToolTag(final ItemStack stack) {
        stack.getOrCreateNbt().remove(LINKINGTOOL_TAG);
    }

    @Override
    public void appendTooltip(final ItemStack stack, final World world, final List<Text> tooltip,
            final TooltipContext context) {
        final NbtCompound tag = stack.getOrCreateNbt();
        if (tag.contains(LINKINGTOOL_TAG)) {
            final NbtCompound comp = tag.getCompound(LINKINGTOOL_TAG);
            final boolean containsPos =
                    comp.contains("X") && comp.contains("Y") && comp.contains("Z");
            if (containsPos) {
                final BlockPos pos = NbtHelper.toBlockPos(comp);
                tooltip(tooltip, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                return;
            }
        }
        tooltip(tooltip, "lt.notlinked");
        tooltip(tooltip, "lt.notlinked.msg");
    }

    public void tooltip(final List<Text> list, final String text, final Object... obj) {
        list.add(getComponent(text, obj));
    }

}
