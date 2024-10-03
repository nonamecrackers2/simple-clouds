package dev.nonamecrackers2.simpleclouds.common.registry;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.RegionType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

public class SimpleCloudsRegistries
{
	public static final ResourceKey<Registry<RegionType>> REGION_TYPES_KEY = ResourceKey.createRegistryKey(SimpleCloudsMod.id("region_type"));
	public static final Registry<RegionType> REGION_TYPES = new RegistryBuilder<>(REGION_TYPES_KEY).defaultKey(SimpleCloudsMod.id("voronoi_diagram")).create();
	
	public static void registerRegistries(NewRegistryEvent event)
	{
		event.register(REGION_TYPES);
	}
}
