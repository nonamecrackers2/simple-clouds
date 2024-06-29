package dev.nonamecrackers2.simpleclouds.common.cloud;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class CloudTypeDataManager extends SimpleJsonResourceReloadListener
{
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	public static final CloudTypeDataManager INSTANCE = new CloudTypeDataManager();
	private Map<ResourceLocation, CloudType> cloudTypes = ImmutableMap.of();
	
	private CloudTypeDataManager()
	{
		super(GSON, "cloud_types");
	}
	
	@Override
	protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller filler)
	{
		ImmutableMap.Builder<ResourceLocation, CloudType> builder = ImmutableMap.builder();
		for (var entry : files.entrySet())
		{
			ResourceLocation id = entry.getKey();
			JsonElement element = entry.getValue();
			
//			NoiseSettings settings = NoiseSettings.STATIC.decode(JsonOps.INSTANCE, element).resultOrPartial(error -> {
//				throw new JsonSyntaxException(error);
//			}).get();
		}
	}
}
