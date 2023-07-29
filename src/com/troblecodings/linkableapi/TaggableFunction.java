package com.troblecodings.linkableapi;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface TaggableFunction {

    void test(Level level, BlockPos pos, CompoundTag tag);
    
}
