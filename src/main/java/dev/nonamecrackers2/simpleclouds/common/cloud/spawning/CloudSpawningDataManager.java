package dev.nonamecrackers2.simpleclouds.common.cloud.spawning;

import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

public class CloudSpawningDataManager extends SimpleJsonResourceReloadListener
{
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static CloudSpawningDataManager instance;
	private CloudTypeSource source;
	private CloudSpawningConfig config;
	
	public CloudSpawningDataManager(CloudTypeSource source)
	{
		super(GSON, "cloud_spawning");
		this.source = source;
		this.config = CloudSpawningConfig.EMPTY;
	}
	
	public CloudSpawningConfig getConfig()
	{
		return this.config;
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller filler)
	{
		JsonElement root = resources.get(SimpleCloudsMod.id("config"));
		if (root == null)
		{
			LOGGER.error("Could not find root Simple Clouds config");
			this.config = CloudSpawningConfig.EMPTY;
			return;
		}
		
		ImmutableMap.Builder<ResourceLocation, CloudSpawningConfig.Info> entries = ImmutableMap.builder();
		
		for (var entry : resources.entrySet())
		{
			if (entry.getValue() != root)
			{
				try
				{
					CloudSpawningConfig.Info info = CloudSpawningConfig.readInfo(this.source, GsonHelper.convertToJsonObject(entry.getValue(), "root"));
					entries.put(info.cloudType(), info);
				}
				catch (JsonSyntaxException | IllegalArgumentException | NullPointerException e) 
				{
					LOGGER.error("Failed to parse spawn info for file '" + entry.getKey() + "'", e);
					this.config = CloudSpawningConfig.EMPTY;
				}
			}
		}
		
		try 
		{
			this.config = CloudSpawningConfig.fromJson(this.source, GsonHelper.convertToJsonObject(root, "root"), entries.build());
		} 
		catch (JsonSyntaxException | IllegalArgumentException | NullPointerException e) 
		{
			LOGGER.error("Failed to parse cloud spawn config", e);
			this.config = CloudSpawningConfig.EMPTY;
		}
	}
	
	public static void optionalInitialize(CloudTypeSource cloudTypeSource)
	{
		if (instance == null)
			instance = new CloudSpawningDataManager(cloudTypeSource);
	}
	
	public static CloudSpawningDataManager getInstance()
	{
		return Objects.requireNonNull(instance, "Not initialized");
	}
}
