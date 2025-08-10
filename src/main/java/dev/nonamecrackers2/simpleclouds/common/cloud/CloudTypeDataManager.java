package dev.nonamecrackers2.simpleclouds.common.cloud;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
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

public class CloudTypeDataManager extends SimpleJsonResourceReloadListener implements CloudTypeSource
{
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final CloudTypeDataManager SERVER = new CloudTypeDataManager();
	private Map<ResourceLocation, CloudType> cloudTypes = ImmutableMap.of(SimpleCloudsConstants.EMPTY.id(), SimpleCloudsConstants.EMPTY);
	private CloudType[] indexedCloudTypes = new CloudType[] { SimpleCloudsConstants.EMPTY };
	
	public CloudTypeDataManager()
	{
		super(GSON, "cloud_types");
	}
	
	@Override
	protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller filler)
	{
		Map<ResourceLocation, CloudType> types = Maps.newHashMap();
		for (var entry : files.entrySet())
		{
			ResourceLocation id = entry.getKey();
			JsonElement element = entry.getValue();
			try
			{
				JsonObject object = GsonHelper.convertToJsonObject(element, "root");
				types.put(id, CloudType.readFromJson(id, object));
			}
			catch (JsonSyntaxException e)
			{
				LOGGER.warn("Failed to load cloud type '" + id + "'", e);
			}
		}
		
		this.indexedCloudTypes = Streams.concat(Stream.of(SimpleCloudsConstants.EMPTY), types.values().stream().sorted(Comparator.comparing(t -> t.id().toString()))).toArray(i -> new CloudType[i]);
		
		types.put(SimpleCloudsConstants.EMPTY.id(), SimpleCloudsConstants.EMPTY);
		this.cloudTypes = ImmutableMap.copyOf(types);
		
		LOGGER.info("Loaded {} cloud types", this.cloudTypes.size());
	}
	
	@Override
	public CloudType getCloudTypeForId(ResourceLocation id)
	{
		return this.getCloudTypes().get(id);
	}
	
	public Map<ResourceLocation, CloudType> getCloudTypes()
	{
		return this.cloudTypes;
	}
	
	@Override
	public CloudType[] getIndexedCloudTypes()
	{
		return this.indexedCloudTypes;
	}
	
	public static CloudTypeDataManager getServerInstance()
	{
		return SERVER;
	}
}
