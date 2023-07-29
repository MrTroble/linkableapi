package com.troblecodings.linkableapi;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
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

public class MultiLinkingTool extends Item {

    private static final String LINKED_BLOCKS = "linkedBlocks";

    private final BiPredicate<Level, BlockPos> predicate;
    private final Predicate<BlockEntity> predicateSet;
    private final CreativeModeTab tab;
    private final TaggableFunction tagFromFunction;

    public MultiLinkingTool(final CreativeModeTab tab,
            final BiPredicate<Level, BlockPos> predicate) {
        this(tab, predicate, _u -> true);
    }

    public MultiLinkingTool(final CreativeModeTab tab, final BiPredicate<Level, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet, final TaggableFunction function) {
        super(new Properties().durability(64).setNoRepair());
        this.predicate = predicate;
        this.predicateSet = predicateSet;
        this.tagFromFunction = function;
        this.tab = tab;
        if (tab != null) {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onTab);
        }
    }

    public MultiLinkingTool(final CreativeModeTab tab, final BiPredicate<Level, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet) {
        this(tab, predicate, predicateSet, (_u1, _u2, _u3) -> {
        });
    }

    private void onTab(final BuildCreativeModeTabContentsEvent ev) {
        if (ev.getTab().equals(tab))
            ev.accept(() -> this);
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
                final ListTag list = (ListTag) comp.get(LINKED_BLOCKS);
                if (list == null) {
                    message(player, "lt.notlinked");
                    return InteractionResult.FAIL;
                }
                list.stream().map(tag -> NbtUtils.readBlockPos((CompoundTag) tag))
                        .forEach(linkPos -> {
                            if (controller.link(linkPos))
                                message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                        });
                stack.setTag(null);
                message(player, "lt.reset");
                return InteractionResult.FAIL;
            } else {
                if (controller.hasLink() && controller.unlink()) {
                    message(player, "lt.unlink");
                }
            }
            return InteractionResult.SUCCESS;
        } else if (predicate.test(levelIn, pos)) {
            CompoundTag tag = stack.getTag();
            if (tag == null)
                tag = new CompoundTag();
            ListTag list = (ListTag) tag.get(LINKED_BLOCKS);
            if (list == null)
                list = new ListTag();
            list.add(NbtUtils.writeBlockPos(pos));
            tag.put(LINKED_BLOCKS, list);
            tagFromFunction.test(levelIn, pos, tag);
            stack.setTag(tag);
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
        final CompoundTag itemTag = stack.getTag();
        if (itemTag != null) {
            final ListTag list = (ListTag) itemTag.get(LINKED_BLOCKS);
            if (list != null) {
                tooltip(tooltip, "lt.linkedpos",
                        list.stream().map(tag -> NbtUtils.readBlockPos((CompoundTag) tag))
                                .collect(Collectors.toList()));
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
        player.sendSystemMessage(getComponent(text, obj));
    }

    public MutableComponent getComponent(final String text, final Object... obj) {
        return MutableComponent.create(new TranslatableContents(text, text, obj));
    }

}
