package dev.nonamecrackers2.simpleclouds.client.cloud;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeDataManager;
import net.minecraft.resources.ResourceLocation;

public class ClientSideCloudTypeManager
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
	
	public Map<ResourceLocation, CloudType> getCloudTypes()
	{
		if (!this.synced.isEmpty())
			return this.synced;
		else
			return this.dataManager.getCloudTypes();
	}
	
	public CloudType[] getIndexed()
	{
		if (this.indexed.length > 0)
			return this.indexed;
		else
			return this.dataManager.getIndexedCloudTypes();
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
}
