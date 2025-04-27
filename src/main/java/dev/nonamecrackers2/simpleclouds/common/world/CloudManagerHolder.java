package dev.nonamecrackers2.simpleclouds.common.world;

import net.minecraft.world.level.Level;

public interface CloudManagerHolder<T extends Level>
{
	CloudManager<T> getCloudManager();
}
