package com.troblecodings.linkableapi;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@FunctionalInterface
public interface TaggableFunction {

    void test(World level, BlockPos pos, NbtCompound tag);
    
}
