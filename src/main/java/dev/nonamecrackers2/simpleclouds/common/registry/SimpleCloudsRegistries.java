package dev.nonamecrackers2.simpleclouds.common.registry;

import java.util.Objects;
import java.util.function.Supplier;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.RegionType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;

public class SimpleCloudsRegistries
{
	public static final ResourceLocation REGION_TYPES = SimpleCloudsMod.id("region_type");
	private static Supplier<IForgeRegistry<RegionType>> regionTypes;
	
	public static IForgeRegistry<RegionType> getRegionTypeRegistry()
	{
		return Objects.requireNonNull(regionTypes, "Region types registry is not registered").get();
	}
	
	public static void registerRegistries(NewRegistryEvent event)
	{
		regionTypes = event.create(new RegistryBuilder<RegionType>().setName(REGION_TYPES).disableSync());
	}
}
