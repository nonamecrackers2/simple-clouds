package dev.nonamecrackers2.simpleclouds.common.cloud;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

public class CloudTypeDataManager extends SimpleJsonResourceReloadListener
{
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	public static final CloudTypeDataManager SERVER = new CloudTypeDataManager();
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
			try
			{
				JsonObject object = GsonHelper.convertToJsonObject(element, "root");
				builder.put(id, CloudType.readFromJson(object));
			}
			catch (JsonSyntaxException e)
			{
				LOGGER.warn("Failed to load cloud type '" + id + "'", e);
			}
		}
		this.cloudTypes = builder.buildKeepingLast();
		LOGGER.info("Loaded {} cloud types", this.cloudTypes.size());
	}
	
	public Map<ResourceLocation, CloudType> getCloudTypes()
	{
		return this.cloudTypes;
	}
}
