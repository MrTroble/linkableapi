package com.troblecodings.linkableapi;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

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
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.BaseText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MultiLinkingTool extends Item {

    private static final String LINKED_BLOCKS = "linkedBlocks";

    private final BiPredicate<World, BlockPos> predicate;
    private final Predicate<BlockEntity> predicateSet;
    private final TaggableFunction tagFromFunction;

    public MultiLinkingTool(final ItemGroup tab,
            final BiPredicate<World, BlockPos> predicate) {
        this(tab, predicate, _u -> true);
    }

    public MultiLinkingTool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet, final TaggableFunction function) {
        super(new FabricItemSettings().maxDamage(64).group(tab));
        this.predicate = predicate;
        this.predicateSet = predicateSet;
        this.tagFromFunction = function;
    }

    public MultiLinkingTool(final ItemGroup tab, final BiPredicate<World, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet) {
        this(tab, predicate, predicateSet, (_u1, _u2, _u3) -> {
        });
    }

	@Override
    public ActionResult useOnBlock(final ItemUsageContext ctx) {
        final World levelIn = ctx.getWorld();
        final PlayerEntity player = ctx.getPlayer();
        if (player == null)
            return ActionResult.FAIL;
        final BlockPos pos = ctx.getBlockPos();
        final ItemStack stack = ctx.getStack();
        if (levelIn.isClient())
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
                final ListTag list = (ListTag) comp.get(LINKED_BLOCKS);
                if (list == null) {
                    message(player, "lt.notlinked");
                    return ActionResult.FAIL;
                }
                list.stream().map(tag -> NbtHelper.toBlockPos((CompoundTag) tag))
                        .forEach(linkPos -> {
                            if (controller.link(linkPos))
                                message(player, "lt.linkedpos", pos.getX(), pos.getY(), pos.getZ());
                        });
                stack.setTag(null);
                message(player, "lt.reset");
                return ActionResult.FAIL;
            } else {
                if (controller.hasLink() && controller.unlink()) {
                    message(player, "lt.unlink");
                }
            }
            return ActionResult.SUCCESS;
        } else if (predicate.test(levelIn, pos)) {
            CompoundTag tag = stack.getTag();
            if (tag == null)
                tag = new CompoundTag();
            ListTag list = (ListTag) tag.get(LINKED_BLOCKS);
            if (list == null)
                list = new ListTag();
            list.add(NbtHelper.fromBlockPos(pos));
            tag.put(LINKED_BLOCKS, list);
            tagFromFunction.test(levelIn, pos, tag);
            stack.setTag(tag);
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
        final CompoundTag itemTag = stack.getTag();
        if (itemTag != null) {
            final ListTag list = (ListTag) itemTag.get(LINKED_BLOCKS);
            if (list != null) {
                tooltip(tooltip, "lt.linkedpos",
                        list.stream().map(tag -> NbtHelper.toBlockPos((CompoundTag) tag))
                                .collect(Collectors.toList()));
                return;
            }
        }
        tooltip(tooltip, "lt.notlinked");
        tooltip(tooltip, "lt.notlinked.msg");
    }

    public void tooltip(final List<Text> list, final String text, final Object... obj) {
        list.add(getComponent(text, obj));
    }

    public void message(final PlayerEntity player, final String text, final Object... obj) {
        player.sendMessage(getComponent(text, obj));
    }

    public BaseText getComponent(final String text, final Object... obj) {
        return new TranslatableText(text, obj);
    }

}
