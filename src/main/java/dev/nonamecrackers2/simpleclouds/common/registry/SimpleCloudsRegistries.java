package dev.nonamecrackers2.simpleclouds.common.registry;

import java.util.Objects;
import java.util.function.Supplier;

import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.RegionType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;

@ScheduledForRemoval
public class SimpleCloudsRegistries
{
	@Deprecated
	public static final ResourceLocation REGION_TYPES = SimpleCloudsMod.id("region_type");
	@Deprecated
	private static Supplier<IForgeRegistry<RegionType>> regionTypes;
	
	@Deprecated
	public static IForgeRegistry<RegionType> getRegionTypeRegistry()
	{
		return Objects.requireNonNull(regionTypes, "Region types registry is not registered").get();
	}
	
	public static void registerRegistries(NewRegistryEvent event)
	{
		//regionTypes = event.create(new RegistryBuilder<RegionType>().setName(REGION_TYPES).disableSync());
	}
}
