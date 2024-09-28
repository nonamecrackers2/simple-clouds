package dev.nonamecrackers2.simpleclouds.common.cloud;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.LevelResource;

public class CloudTypeDataManager extends SimpleJsonResourceReloadListener implements CloudTypeSource
{
	public static final LevelResource SIMPLE_CLOUDS_FOLDER = new LevelResource("simpleclouds");
//	private static @Nullable Path simpleCloudsWorldPath;
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final CloudTypeDataManager SERVER = new CloudTypeDataManager();//new CloudTypeDataManager(CloudTypeDataManager.Environment.SERVER);
//	private final CloudTypeDataManager.Environment environment;
	private Map<ResourceLocation, CloudType> cloudTypes = ImmutableMap.of();
	private CloudType[] indexedCloudTypes = new CloudType[0];
	
	public CloudTypeDataManager()//(CloudTypeDataManager.Environment environment)
	{
		super(GSON, "cloud_types");
		//this.environment = environment;
	}
//	
//	public static void setSimpleCloudsWorldPath(@Nullable Path path)
//	{
//		if (path != null)
//			LOGGER.debug("Received simpleclouds world folder path: {}", path);
//		else
//			LOGGER.debug("Cleared simpleclouds world folder path");
//		simpleCloudsWorldPath = path;
//	}
	
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
				builder.put(id, CloudType.readFromJson(id, object));
			}
			catch (JsonSyntaxException e)
			{
				LOGGER.warn("Failed to load cloud type '" + id + "'", e);
			}
		}
		this.cloudTypes = builder.buildKeepingLast();
		
		this.indexedCloudTypes = this.cloudTypes.values().stream().sorted(Comparator.comparing(t -> t.id().toString())).toArray(i -> new CloudType[i]);
		
//		Path directory = this.environment.getPath();
//		
//		CloudType[] indexed = new CloudType[this.cloudTypes.size()];
//		int index = 0;
//		for (var type : this.cloudTypes.values())
//		{
//			indexed[index] = type;
//			index++;
//		}
//		this.indexedCloudTypes = indexed;
//		
//		if (directory != null)
//		{
//			boolean remakeFile = false;
//			File file = directory.resolve("indexed_cloud_types_" + this.environment.getEnvironmentString() + ".json").toFile();
//			File directoryFile = directory.toFile();
//			if (!directoryFile.exists())
//				directoryFile.mkdirs();
//			
//			try (FileReader reader = new FileReader(file))
//			{
//				JsonElement element = GSON.fromJson(reader, JsonElement.class);
//				if (element == null)
//					throw new JsonSyntaxException(new NullPointerException("ROOT was null"));
//				JsonArray array = GsonHelper.convertToJsonArray(element, "root");
//				int count = array.size();
//				if (count != this.cloudTypes.size())
//				{
//					remakeFile = true;
//				}
//				else
//				{
//					CloudType[] readIndexedBuilder = new CloudType[count];
//					for (int i = 0; i < count; i++)
//					{
//						String id = GsonHelper.convertToString(array.get(i), "cloud type");
//						ResourceLocation loc = ResourceLocation.tryParse(id);
//						if (loc != null && this.cloudTypes.containsKey(loc))
//						{
//							readIndexedBuilder[i] = this.cloudTypes.get(loc);
//						}
//						else
//						{
//							remakeFile = true;
//							break;
//						}
//					}
//					if (!remakeFile)
//						this.indexedCloudTypes = readIndexedBuilder;
//				}
//			}
//			catch (FileNotFoundException e)
//			{
//				LOGGER.debug("Could not find indexed cloud types file. Remaking...");
//				remakeFile = true;
//			}
//			catch (IOException | JsonParseException e)
//			{
//				LOGGER.error("Failed to read indexed cloud types file for environment '" + this.environment.getEnvironmentString() + "'", e);
//				remakeFile = true;
//			}
//			
//			if (remakeFile)
//			{
//				LOGGER.debug("Saving new indexed cloud types...");
//				
//				try (FileWriter writer = new FileWriter(file))
//				{
//					JsonArray array = new JsonArray();
//					for (int i = 0; i < this.indexedCloudTypes.length; i++)
//						array.add(this.indexedCloudTypes[i].id().toString());
//					GSON.toJson(array, writer);
//				}
//				catch (IOException | JsonParseException e)
//				{
//					LOGGER.error("Failed to write indexed cloud types file for environment '" + this.environment.getEnvironmentString() + "'", e);
//				}
//			}
//		}
//		else
//		{
//			LOGGER.warn("Could not find path to saved indexed cloud types");
//		}
		
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
//	
//	public static enum Environment
//	{
//		CLIENT("client", () -> FMLPaths.GAMEDIR.get().resolve(SIMPLE_CLOUDS_FOLDER.getId())),
//		SERVER("server", () -> simpleCloudsWorldPath);
//		
//		private final String envString;
//		private final Supplier<Path> pathGetter;
//		
//		private Environment(String envString, Supplier<Path> pathGetter)
//		{
//			this.envString = envString;
//			this.pathGetter = pathGetter;
//		}
//		
//		public String getEnvironmentString()
//		{
//			return this.envString;
//		}
//		
//		public @Nullable Path getPath()
//		{
//			return this.pathGetter.get();
//		}
//	}
}
