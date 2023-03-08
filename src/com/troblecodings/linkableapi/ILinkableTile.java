package com.troblecodings.linkableapi;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public interface ILinkableTile {

    boolean hasLink();

    @Deprecated
    boolean link(final BlockPos pos);

    default boolean link(final BlockPos pos, final CompoundTag tag) {
        return link(pos);
    }

    boolean unlink();

}
