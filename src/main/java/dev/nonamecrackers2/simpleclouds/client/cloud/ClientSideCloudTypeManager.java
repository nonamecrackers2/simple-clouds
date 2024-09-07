package dev.nonamecrackers2.simpleclouds.client.cloud;

import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import dev.nonamecrackers2.simpleclouds.client.world.ClientCloudManager;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeDataManager;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeSource;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.cloud.weather.WeatherType;
import net.minecraft.resources.ResourceLocation;

public class ClientSideCloudTypeManager implements CloudTypeSource
{
	private static final ClientSideCloudTypeManager INSTANCE = new ClientSideCloudTypeManager();
	private final CloudTypeDataManager dataManager;
	private Map<ResourceLocation, CloudType> synced = ImmutableMap.of();
	private CloudType[] indexed = new CloudType[0];
	
	private ClientSideCloudTypeManager()
	{
		this.dataManager = new CloudTypeDataManager(CloudTypeDataManager.Environment.CLIENT);
	}
	
	public CloudTypeDataManager getClientSideDataManager()
	{
		return this.dataManager;
	}
	
	public void clearCloudTypes()
	{
		this.synced = ImmutableMap.of();
	}
	
	@Override
	public CloudType getCloudTypeForId(ResourceLocation id)
	{
		return this.getCloudTypes().get(id);
	}

	@Override
	public CloudType[] getIndexedCloudTypes()
	{
		if (this.indexed.length > 0)
			return this.indexed;
		else
			return this.dataManager.getIndexedCloudTypes();
	}
	
	public Map<ResourceLocation, CloudType> getCloudTypes()
	{
		if (!this.synced.isEmpty())
			return this.synced;
		else
			return this.dataManager.getCloudTypes();
	}
	
	public void receiveSynced(Map<ResourceLocation, CloudType> synced, CloudType[] indexed)
	{
		this.synced = ImmutableMap.copyOf(synced);
		this.indexed = indexed;
	}
	
	public static ClientSideCloudTypeManager getInstance()
	{
		return INSTANCE;
	}
	
	public static boolean isValidClientSideSingleModeCloudType(@Nullable CloudType type)
	{
		return type != null && type.weatherType() == WeatherType.NONE;
	}
}
