package com.troblecodings.linkableapi;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import com.google.common.base.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public class Linkingtool extends Item implements Message {

    protected static final String LINKINGTOOL_TAG = "linkingToolTag";
    public DataComponentType<CompoundTag> compoundData;

    protected final BiPredicate<Level, BlockPos> predicate;
    protected final Predicate<BlockEntity> predicateSet;
    protected final CreativeModeTab tab;
    protected final TaggableFunction tagFromFunction;

    public Linkingtool(final Properties properties, final CreativeModeTab tab,
            final BiPredicate<Level, BlockPos> predicate, final DataComponentType<CompoundTag> data) {
        this(properties, tab, predicate, _u -> true, data);
    }

    public Linkingtool(final Properties properties, final CreativeModeTab tab,
            final BiPredicate<Level, BlockPos> predicate, final Predicate<BlockEntity> predicateSet,
            final DataComponentType<CompoundTag> data) {
        this(properties, tab, predicate, predicateSet, (_u1, _u2, _u3) -> {
        }, data);
    }

    public Linkingtool(final Properties properties, final CreativeModeTab tab,
            final BiPredicate<Level, BlockPos> predicate, final Predicate<BlockEntity> predicateSet,
            final TaggableFunction function, final DataComponentType<CompoundTag> data) {
        super(properties.durability(64));
        this.predicate = predicate;
        this.predicateSet = predicateSet;
        this.tab = tab;
        if (tab != null) {
            final IEventBus bus = ModLoadingContext.get().getActiveContainer().getEventBus();
            if (bus != null) {
                bus.addListener(this::onTab);
            }
        }
        this.tagFromFunction = function;
        this.compoundData = data;
    }

    private void onTab(final BuildCreativeModeTabContentsEvent ev) {
        if (ev.getTab().equals(tab)) {
            ev.accept(() -> this);
        }
    }

    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext ctx) {
        final Player player = ctx.getPlayer();
        if (player == null)
            return InteractionResult.FAIL;
        final Level levelIn = ctx.getLevel();
        if (levelIn.isClientSide())
            return InteractionResult.PASS;
        final BlockPos pos = ctx.getClickedPos();
        final BlockEntity entity = levelIn.getBlockEntity(pos);
        final CompoundTag itemTag = getOrCreateForStack(stack);
        if (entity instanceof ILinkableTile && this.predicateSet.apply(entity)) {
            final ILinkableTile controller = (ILinkableTile) entity;
            if (!player.isShiftKeyDown()) {
                if (!itemTag.contains(LINKINGTOOL_TAG)) {
                    message(player, "lt.notset", pos.toString());
                    return InteractionResult.PASS;
                }
                final Optional<BlockPos> lpos = readBlockPos(itemTag, LINKINGTOOL_TAG);
                if (controller.link(lpos, itemTag)) {
                    message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                    removeToolTag(stack);
                    message(player, "lt.reset");
                    stack.hurtAndBreak(1, (ServerLevel) levelIn, player, item -> {
                    });
                    return InteractionResult.SUCCESS;
                }
                message(player, "lt.notlinked");
                message(player, "lt.notlinked.msg");
                return InteractionResult.FAIL;
            }
            if (controller.canBeLinked() && predicate.test(levelIn, pos)) {
                if (itemTag.contains(LINKINGTOOL_TAG)) {
                    message(player, "lt.setpos.msg");
                    return InteractionResult.FAIL;
                }
                final Tag newToolTag = writeBlockPos(pos);
                tagFromFunction.test(levelIn, pos, newToolTag);
                itemTag.put(LINKINGTOOL_TAG, newToolTag);
                stack.set(compoundData, itemTag);
                message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
                message(player, "lt.setpos.msg");
                return InteractionResult.SUCCESS;
            }
            if (controller.hasLink() && controller.unlink()) {
                message(player, "lt.unlink");
            }
            return InteractionResult.SUCCESS;
        }
        if (predicate.test(levelIn, pos)) {
            if (itemTag.contains(LINKINGTOOL_TAG)) {
                message(player, "lt.setpos.msg");
                return InteractionResult.FAIL;
            }
            final Tag newToolTag = writeBlockPos(pos);
            tagFromFunction.test(levelIn, pos, newToolTag);
            itemTag.put(LINKINGTOOL_TAG, newToolTag);
            stack.set(compoundData, itemTag);
            message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
            message(player, "lt.setpos.msg");
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            removeToolTag(stack);
            message(player, "lt.reset");
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public void removeToolTag(final ItemStack stack) {
        stack.remove(compoundData);
    }

    protected CompoundTag getOrCreateForStack(final ItemStack stack) {
        CompoundTag tag = stack.get(compoundData);
        if (tag == null) {
            tag = new CompoundTag();
            stack.set(compoundData, tag);
        }
        return tag;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext ctx,
            final TooltipDisplay display, final Consumer<Component> tooltip,
            final TooltipFlag flagIn) {
        final CompoundTag tag = getOrCreateForStack(stack);
        if (tag.contains(LINKINGTOOL_TAG)) {
            final Optional<BlockPos> pos = readBlockPos(tag, LINKINGTOOL_TAG);
            if (pos.get() == null)
                return;
            tooltip(tooltip, "lt.linkedpos", pos.get().getX(), pos.get().getY(), pos.get().getZ());
            return;
        }
        tooltip(tooltip, "lt.notlinked");
        tooltip(tooltip, "lt.notlinked.msg");
    }

    public void tooltip(final Consumer<Component> sink, final String text, final Object... obj) {
        sink.accept(getComponent(text, obj));
    }

    public static Tag writeBlockPos(final BlockPos pos) {
        return new IntArrayTag(new int[] { pos.getX(), pos.getY(), pos.getZ() });
    }

    public static Optional<BlockPos> readBlockPos(final CompoundTag tag, final String key) {
        final Tag posTag = tag.get(key);
        if (posTag instanceof IntArrayTag intArray) {
            final int[] aint = intArray.getAsIntArray();
            if (aint.length == 3) {
                return Optional.of(new BlockPos(aint[0], aint[1], aint[2]));
            }
        }
        return Optional.empty();
    }

}
