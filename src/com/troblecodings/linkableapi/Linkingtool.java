package com.troblecodings.linkableapi;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import com.google.common.base.Predicate;
import com.troblecodings.tcredstone.TCRedstoneMain;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
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
        super(new Settings().maxDamage(64));
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
        final BlockEntity entity = levelIn.getBlockEntity(pos);
        final ItemStack stack = ctx.getStack();
        final NbtCompound itemTag = getOrCreateNbt(stack);
        if (entity instanceof ILinkableTile && this.predicateSet.apply(entity)) {
            final ILinkableTile controller = (ILinkableTile) entity;
            if (!player.isSneaking()) {
                if (!itemTag.contains(LINKINGTOOL_TAG)) {
                    message(player, "lt.notset", pos.toString());
                    return ActionResult.PASS;
                }
                final Optional<BlockPos> lpos = NbtHelper.toBlockPos(itemTag, LINKINGTOOL_TAG);
                if (controller.link(lpos, itemTag)) {
                    message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                    removeToolTag(stack);
                    message(player, "lt.reset");
                    stack.damage(1, player, EquipmentSlot.MAINHAND);
                    return ActionResult.SUCCESS;
                }
                message(player, "lt.notlinked");
                message(player, "lt.notlinked.msg");
                return ActionResult.FAIL;
            } else {
                if (controller.canBeLinked() && predicate.test(levelIn, pos)) {
                    if (itemTag.contains(LINKINGTOOL_TAG)) {
                        message(player, "lt.setpos.msg");
                        return ActionResult.FAIL;
                    }
                    final NbtElement newToolTag = NbtHelper.fromBlockPos(pos);
                    tagFromFunction.test(levelIn, pos, newToolTag);
                    itemTag.put(LINKINGTOOL_TAG, newToolTag);
                    stack.set(TCRedstoneMain.COMPOUND_DATA, itemTag);
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
            if (itemTag.contains(LINKINGTOOL_TAG)) {
                message(player, "lt.setpos.msg");
                return ActionResult.FAIL;
            }
            final NbtElement newToolTag = NbtHelper.fromBlockPos(pos);
            tagFromFunction.test(levelIn, pos, newToolTag);
            itemTag.put(LINKINGTOOL_TAG, newToolTag);
            stack.set(TCRedstoneMain.COMPOUND_DATA, itemTag);
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
        stack.remove(TCRedstoneMain.COMPOUND_DATA);
    }

    protected static NbtCompound getOrCreateNbt(final ItemStack stack) {
        NbtCompound nbt = stack.get(TCRedstoneMain.COMPOUND_DATA);
        if (nbt == null) {
            nbt = new NbtCompound();
            stack.set(TCRedstoneMain.COMPOUND_DATA, nbt);
        }
        return nbt;
    }

    @Override
    public void appendTooltip(final ItemStack stack, final TooltipContext context,
            final TooltipDisplayComponent component, final Consumer<Text> tooltip,
            final TooltipType type) {
        final NbtCompound tag = getOrCreateNbt(stack);
        if (tag.contains(LINKINGTOOL_TAG)) {
            final Optional<BlockPos> pos = NbtHelper.toBlockPos(tag, LINKINGTOOL_TAG);
            if (pos.get() == null)
                return;
            tooltip(tooltip, "lt.linkedpos", pos.get().getX(), pos.get().getY(), pos.get().getZ());
            return;
        }
        tooltip(tooltip, "lt.notlinked");
        tooltip(tooltip, "lt.notlinked.msg");
    }

    public void tooltip(final Consumer<Text> list, final String text, final Object... obj) {
        list.accept(getComponent(text, obj));
    }
}
