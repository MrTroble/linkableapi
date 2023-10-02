package com.troblecodings.linkableapi;

import java.util.List;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class Linkingtool extends Item {

    private final BiPredicate<Level, BlockPos> predicate;
    private final Predicate<BlockEntity> predicateSet;
    private final TaggableFunction tagFromFunction;

    public Linkingtool(final CreativeModeTab tab, final BiPredicate<Level, BlockPos> predicate) {
        this(tab, predicate, _u -> true);
    }

    public Linkingtool(final CreativeModeTab tab, final BiPredicate<Level, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet) {
        this(tab, predicate, predicateSet, (_u1, _u2, _u3) -> {
        });
    }

    public Linkingtool(final CreativeModeTab tab, final BiPredicate<Level, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet, final TaggableFunction function) {
        super(new Properties().tab(tab).durability(64).setNoRepair());
        this.predicate = predicate;
        this.predicateSet = predicateSet;
        this.tagFromFunction = function;
    }

    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext ctx) {
        final Level levelIn = ctx.getLevel();
        final Player player = ctx.getPlayer();
        if (player == null)
            return InteractionResult.FAIL;
        final BlockPos pos = ctx.getClickedPos();
        if (levelIn.isClientSide)
            return InteractionResult.PASS;
        final BlockEntity entity = levelIn.getBlockEntity(pos);
        if (entity instanceof ILinkableTile && this.predicateSet.apply(entity)) {
            final ILinkableTile controller = (ILinkableTile) entity;
            if (!player.isShiftKeyDown()) {
                final CompoundTag comp = stack.getTag();
                if (comp == null) {
                    message(player, "lt.notset", pos.toString());
                    return InteractionResult.PASS;
                }
                final BlockPos linkedPos = NbtUtils.readBlockPos(comp);
                if (controller.link(linkedPos, comp)) {
                    message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                    stack.setTag(null);
                    message(player, "lt.reset");
                    return InteractionResult.FAIL;
                }
                message(player, "lt.notlinked");
                message(player, "lt.notlinked.msg");
                return InteractionResult.FAIL;
            } else {
                if (controller.hasLink() && controller.unlink()) {
                    message(player, "lt.unlink");
                    return InteractionResult.SUCCESS;
                }
                if (controller.canBeLinked() && predicate.test(levelIn, pos)) {
                    final CompoundTag tag = stack.getTag();
                    if (tag != null) {
                        final boolean containsPos = tag.contains("X") && tag.contains("Y")
                                && tag.contains("Z");
                        if (containsPos) {
                            message(player, "lt.setpos.msg");
                            return InteractionResult.FAIL;
                        }
                    }
                    final CompoundTag comp = NbtUtils.writeBlockPos(pos);
                    tagFromFunction.test(levelIn, pos, comp);
                    stack.setTag(comp);
                    message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
                    message(player, "lt.setpos.msg");
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.SUCCESS;
        } else if (predicate.test(levelIn, pos)) {
            final CompoundTag tag = stack.getTag();
            if (tag != null) {
                final boolean containsPos = tag.contains("X") && tag.contains("Y")
                        && tag.contains("Z");
                if (containsPos) {
                    message(player, "lt.setpos.msg");
                    return InteractionResult.FAIL;
                }
            }
            final CompoundTag comp = NbtUtils.writeBlockPos(pos);
            tagFromFunction.test(levelIn, pos, comp);
            stack.setTag(comp);
            message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
            message(player, "lt.setpos.msg");
            return InteractionResult.SUCCESS;
        } else if (player.isShiftKeyDown() && stack.getTag() != null) {
            stack.setTag(null);
            message(player, "lt.reset");
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final Level levelIn,
            final List<Component> tooltip, final TooltipFlag flagIn) {
        final CompoundTag tag = stack.getTag();
        if (tag != null) {
            final boolean containsPos = tag.contains("X") && tag.contains("Y") && tag.contains("Z");
            if (containsPos) {
                final BlockPos pos = NbtUtils.readBlockPos(tag);
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

    public void message(final Player player, final String text, final Object... obj) {
        player.sendMessage(getComponent(text, obj), player.getUUID());
    }

    public MutableComponent getComponent(final String text, final Object... obj) {
        return new TranslatableComponent(text, obj);
    }
}
