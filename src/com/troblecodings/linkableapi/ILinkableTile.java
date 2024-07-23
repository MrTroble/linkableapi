package com.troblecodings.linkableapi;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

public interface ILinkableTile {

    boolean hasLink();

    @Deprecated
    boolean link(final BlockPos pos);

    default boolean link(final BlockPos pos, final NBTTagCompound tag) {
        return link(pos);
    }

    boolean unlink();

    default boolean canBeLinked() {
        return false;
    }

}
