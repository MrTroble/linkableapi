package com.troblecodings.linkableapi;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public interface ILinkableTile {

    boolean hasLink();

    @Deprecated
    default boolean link(final BlockPos pos) {
        return false;
    }

    default boolean link(final BlockPos pos, final NbtCompound tag) {
        return link(pos);
    }

    boolean unlink();

    default boolean canBeLinked() {
        return false;
    }

}
