package dev.nonamecrackers2.simpleclouds.common.world;

import net.minecraft.world.level.Level;

public interface CloudManagerAccessor<T extends Level>
{
	CloudManager<T> getCloudManager();
}
