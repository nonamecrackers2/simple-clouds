package dev.nonamecrackers2.simpleclouds.common.cloud.spawning;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSyntaxException;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeSource;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

public class CloudSpawningDataManager extends SimplePreparableReloadListener<JsonElement>
{
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static CloudSpawningDataManager instance;
	private CloudTypeSource source;
	private CloudSpawningConfig config;
	
	public CloudSpawningDataManager(CloudTypeSource source)
	{
		this.source = source;
		this.config = CloudSpawningConfig.EMPTY;
	}
	
	public CloudSpawningConfig getConfig()
	{
		return this.config;
	}
	
	@Override
	protected JsonElement prepare(ResourceManager manager, ProfilerFiller filler)
	{
		List<Resource> resources = manager.getResourceStack(SimpleCloudsMod.id("cloud_spawning/config.json"));
		if (!resources.isEmpty())
		{
			if (resources.size() > 1)
				LOGGER.warn("Multiple cloud spawn configs have been found. Picking highest priority");
			Resource resource = resources.get(0);
			try (Reader reader = resource.openAsReader()) {
				return GsonHelper.fromJson(GSON, reader, JsonElement.class);
			} catch (IOException e) {
				LOGGER.error("Cloudn't not load spawn config from {}", resource.sourcePackId());
			}
		}
		else
		{
			LOGGER.debug("No cloud spawn config found");
			return JsonNull.INSTANCE;
		}
		return null;
	}

	@Override
	protected void apply(JsonElement element, ResourceManager manager, ProfilerFiller filler)
	{
		if (element.isJsonNull())
			return;
		
		try 
		{
			this.config = CloudSpawningConfig.fromJson(this.source, GsonHelper.convertToJsonObject(element, "root"));
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
