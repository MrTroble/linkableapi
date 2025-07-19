package com.troblecodings.linkableapi;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public interface ILinkableTile {

    boolean hasLink();

    @Deprecated
    default boolean link(final Optional<BlockPos> lpos) {
        return false;
    }

    default boolean link(final Optional<BlockPos> pos, final CompoundTag tag) {
        return link(pos);
    }

    boolean unlink();

    default boolean canBeLinked() {
        return false;
    }

}
