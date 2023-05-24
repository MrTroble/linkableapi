package com.troblecodings.linkableapi;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@FunctionalInterface
public interface TaggableFunction {

    void test(World level, BlockPos pos, NBTTagCompound tag);

}
