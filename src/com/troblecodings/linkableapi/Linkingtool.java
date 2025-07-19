package com.troblecodings.linkableapi;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

import com.google.common.base.Predicate;
import com.troblecodings.tcredstone.GIRCRedstoneMain;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class Linkingtool extends Item implements Message {

    protected static final String LINKINGTOOL_TAG = "linkingToolTag";

    protected final BiPredicate<Level, BlockPos> predicate;
    protected final Predicate<BlockEntity> predicateSet;
    protected final CreativeModeTab tab;
    protected final TaggableFunction tagFromFunction;

    public Linkingtool(final CreativeModeTab tab, final BiPredicate<Level, BlockPos> predicate) {
        this(tab, predicate, _u -> true);
    }

    public Linkingtool(final CreativeModeTab tab, final BiPredicate<Level, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet) {
        this(tab, predicate, predicateSet, (_u1, _u2, _u3) -> {
        });
    }

    @SuppressWarnings("removal")
    public Linkingtool(final CreativeModeTab tab, final BiPredicate<Level, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet, final TaggableFunction function) {
        super(new Properties().durability(64));
        this.predicate = predicate;
        this.predicateSet = predicateSet;
        this.tab = tab;
        if (tab != null) {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onTab);
        }
        this.tagFromFunction = function;
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
        if (levelIn.isClientSide)
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
                final Optional<BlockPos> lpos = NbtUtils.readBlockPos(itemTag, LINKINGTOOL_TAG);
                if (controller.link(lpos, itemTag)) {
                    message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                    removeToolTag(stack);
                    message(player, "lt.reset");
                    stack.hurtAndBreak(1, player, getEquipmentSlot(stack));
                    return InteractionResult.SUCCESS;
                }
                message(player, "lt.notlinked");
                message(player, "lt.notlinked.msg");
                return InteractionResult.FAIL;
            } else {
                if (controller.canBeLinked() && predicate.test(levelIn, pos)) {
                    if (itemTag.contains(LINKINGTOOL_TAG)) {
                        message(player, "lt.setpos.msg");
                        return InteractionResult.FAIL;
                    }
                    final Tag newToolTag = NbtUtils.writeBlockPos(pos);
                    tagFromFunction.test(levelIn, pos, newToolTag);
                    itemTag.put(LINKINGTOOL_TAG, newToolTag);
                    stack.set(GIRCRedstoneMain.COMPOUND_DATA, itemTag);
                    message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
                    message(player, "lt.setpos.msg");
                    return InteractionResult.SUCCESS;
                }
                if (controller.hasLink() && controller.unlink()) {
                    message(player, "lt.unlink");
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.SUCCESS;
        } else if (predicate.test(levelIn, pos)) {
            if (itemTag.contains(LINKINGTOOL_TAG)) {
                message(player, "lt.setpos.msg");
                return InteractionResult.FAIL;
            }
            final Tag newToolTag = NbtUtils.writeBlockPos(pos);
            tagFromFunction.test(levelIn, pos, newToolTag);
            itemTag.put(LINKINGTOOL_TAG, newToolTag);
            stack.set(GIRCRedstoneMain.COMPOUND_DATA, itemTag);
            message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
            message(player, "lt.setpos.msg");
            return InteractionResult.SUCCESS;
        } else if (player.isShiftKeyDown()) {
            removeToolTag(stack);
            message(player, "lt.reset");
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public void removeToolTag(final ItemStack stack) {
        stack.remove(GIRCRedstoneMain.COMPOUND_DATA);
    }

    protected static CompoundTag getOrCreateForStack(final ItemStack stack) {
        CompoundTag tag = stack.get(GIRCRedstoneMain.COMPOUND_DATA);
        if (tag == null) {
            tag = new CompoundTag();
            stack.set(GIRCRedstoneMain.COMPOUND_DATA, tag);
        }
        return tag;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext ctx,
            final List<Component> tooltip, final TooltipFlag flagIn) {
        final CompoundTag tag = getOrCreateForStack(stack);
        if (tag.contains(LINKINGTOOL_TAG)) {
            final Optional<BlockPos> pos = NbtUtils.readBlockPos(tag, LINKINGTOOL_TAG);
            if (pos.get() == null)
                return;
            tooltip(tooltip, "lt.linkedpos", pos.get().getX(), pos.get().getY(), pos.get().getZ());
            return;
        }
        tooltip(tooltip, "lt.notlinked");
        tooltip(tooltip, "lt.notlinked.msg");
    }

    public void tooltip(final List<Component> list, final String text, final Object... obj) {
        list.add(getComponent(text, obj));
    }

}
