package dev.nonamecrackers2.simpleclouds.common.init;

import java.util.function.Supplier;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.RegionType;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.VoronoiDiagramRegionType;
import dev.nonamecrackers2.simpleclouds.common.registry.SimpleCloudsRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RegionTypes
{
	private static final DeferredRegister<RegionType> REGION_TYPES = DeferredRegister.create(SimpleCloudsRegistries.REGION_TYPES, SimpleCloudsMod.MODID);
	
	public static final Supplier<RegionType> VORONOI_DIAGRAM = REGION_TYPES.register("voronoi_diagram", VoronoiDiagramRegionType::new);
	
	public static void register(IEventBus modBus)
	{
		REGION_TYPES.register(modBus);
	}
}
