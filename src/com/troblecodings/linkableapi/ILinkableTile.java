package com.troblecodings.linkableapi;

import net.minecraft.util.math.BlockPos;

public interface ILinkableTile {

    boolean hasLink();

    boolean link(final BlockPos pos);

    boolean unlink();

}
