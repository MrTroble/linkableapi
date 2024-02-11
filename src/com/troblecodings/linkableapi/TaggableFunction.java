package com.troblecodings.linkableapi;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@FunctionalInterface
public interface TaggableFunction {

    void test(World level, BlockPos pos, CompoundTag tag);
    
}
