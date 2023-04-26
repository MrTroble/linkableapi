package com.troblecodings.linkableapi;

import net.minecraft.util.math.BlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public interface ILinkableTile {

    boolean hasLink();

    @Deprecated
    default boolean link(final BlockPos pos) {
        return false;
    }

    default boolean link(final BlockPos pos, final CompoundTag tag) {
        return link(pos);
    }

    boolean unlink();

}
