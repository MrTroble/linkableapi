package com.troblecodings.linkableapi;

import java.util.List;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

public class Linkingtool extends Item {

    private final BiPredicate<World, BlockPos> predicate;
    private final Predicate<TileEntity> predicateSet;
    private final TaggableFunction tagFromFunction;

    public Linkingtool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate) {
        this(tab, predicate, _u -> true);
    }

    public Linkingtool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<TileEntity> predicateSet) {
        super(new Properties().tab(tab), predicate, predicateSet, (_u1, _u2, _u3) -> {
        });
    }

    public Linkingtool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<TileEntity> predicateSet, final TaggableFunction function) {
        super(new Properties().tab(tab).durability(64).setNoRepair());
        this.predicate = predicate;
        this.predicateSet = predicateSet;
        this.tagFromFunction = function;
    }

    @Override
    public ActionResultType onItemUseFirst(final ItemStack stack, final ItemUseContext ctx) {
        final World levelIn = ctx.getLevel();
        final PlayerEntity player = ctx.getPlayer();
        if (player == null)
            return InteractionResult.FAIL;
        final BlockPos pos = ctx.getClickedPos();
        if (levelIn.isClientSide)
            return ActionResultType.PASS;
        final TileEntity entity = levelIn.getBlockEntity(pos);
        if (entity instanceof ILinkableTile && this.predicateSet.apply(entity)) {
            final ILinkableTile controller = (ILinkableTile) entity;
            if (!player.isShiftKeyDown()) {
                final CompoundNBT comp = stack.getTag();
                if (comp == null) {
                    message(player, "lt.notset", pos.toString());
                    return ActionResultType.PASS;
                }
                final BlockPos linkedPos = NbtUtils.readBlockPos(comp);
                if (controller.link(linkedPos, comp)) {
                    message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                    stack.setTag(null);
                    message(player, "lt.reset");
                    return ActionResultType.FAIL;
                }
                message(player, "lt.notlinked");
                message(player, "lt.notlinked.msg");
                return ActionResultType.FAIL;
            } else {
                if (controller.hasLink() && controller.unlink()) {
                    message(player, "lt.unlink");
                }
            }
            return ActionResultType.SUCCESS;
        } else if (predicate.test(levelIn, pos)) {
            final CompoundTag tag = stack.getTag();
            if (tag != null) {
                final boolean containsPos = tag.contains("X") && tag.contains("Y")
                        && tag.contains("Z");
                if (containsPos) {
                    message(player, "lt.setpos.msg");
                    return ActionResultType.FAIL;
                }
            }
            final CompoundNBT comp = NBTUtil.writeBlockPos(pos);
            tagFromFunction.test(levelIn, pos, comp);
            stack.setTag(comp);
            message(player, "lt.setpos", pos.getX(), pos.getY(), pos.getZ());
            message(player, "lt.setpos.msg");
            return ActionResultType.SUCCESS;
        } else if (player.isShiftKeyDown() && stack.getTag() != null) {
            stack.setTag(null);
            message(player, "lt.reset");
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.FAIL;
    }

    @Override
    public void appendHoverText(final ItemStack stack, @Nullable final World levelIn,
            final List<ITextComponent> tooltip, final ITooltipFlag flagIn) {
        final CompoundNBT tag = stack.getTag();
        if (tag != null) {
            final boolean containsPos = tag.contains("X") && tag.contains("Y") && tag.contains("Z");
            if (containsPos) {
                final BlockPos pos = NBTUtil.readBlockPos(tag);
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

    public void message(final PlayerEntity player, final String text, final Object... obj) {
        player.sendMessage(getComponent(text, obj), player.getUUID());
    }

    public TextComponent getComponent(final String text, final Object... obj) {
        return new TranslationTextComponent(text, obj);
    }
}
