package dev.nonamecrackers2.simpleclouds.client.cloud.spawning;

import java.util.Objects;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeSource;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudSpawningDataManager;

public class ClientSideCloudSpawningManager
{
	private static CloudSpawningDataManager instance;
	
	public static void optionalInitializeOnClient(CloudTypeSource source)
	{
		if (instance == null)
			instance = new CloudSpawningDataManager(source);
	}
	
	public static CloudSpawningDataManager getClientInstance()
	{
		return Objects.requireNonNull(instance, "Not initialized");
	}
}
