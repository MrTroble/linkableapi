package com.troblecodings.linkableapi;

import net.minecraft.core.BlockPos;

public interface ILinkableTile {

    boolean hasLink();

    boolean link(final BlockPos pos);

    boolean unlink();

}
