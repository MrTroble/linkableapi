package eu.gir.linkableapi;

import java.util.List;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class Linkingtool extends Item {

    private final BiPredicate<Level, BlockPos> predicate;
    private final Predicate<BlockEntity> predicateSet;

    public Linkingtool(final CreativeModeTab tab, final BiPredicate<Level, BlockPos> predicate) {
        this(tab, predicate, _u -> true);
    }

    public Linkingtool(final CreativeModeTab tab, final BiPredicate<Level, BlockPos> predicate,
            final Predicate<BlockEntity> predicateSet) {
        super(new Properties());
        this.predicate = predicate;
        this.predicateSet = predicateSet;
        FMLJavaModLoadingContext.get().getModEventBus().addListener(
                ev -> ((CreativeModeTabEvent.BuildContents) ev).registerSimple(tab, this));
    }

    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext ctx) {
        final Level levelIn = ctx.getLevel();
        final Player player = ctx.getPlayer();
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
                final BlockPos lpos = NbtUtils.readBlockPos(comp);
                if (controller.link(lpos)) {
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
                }
            }
            return InteractionResult.SUCCESS;
        } else if (predicate.test(levelIn, pos)) {
            if (stack.getTag() != null) {
                message(player, "lt.setpos.msg");
                return InteractionResult.FAIL;
            }
            final CompoundTag comp = NbtUtils.writeBlockPos(pos);
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
        final CompoundTag nbt = stack.getTag();
        if (nbt != null) {
            final BlockPos pos = NbtUtils.readBlockPos(nbt);
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

    public void message(final Player player, final String text, final Object... obj) {
        player.sendSystemMessage(getComponent(text, obj));
    }

    public MutableComponent getComponent(final String text, final Object... obj) {
        return MutableComponent.create(new TranslatableContents(text, obj));
    }
}
