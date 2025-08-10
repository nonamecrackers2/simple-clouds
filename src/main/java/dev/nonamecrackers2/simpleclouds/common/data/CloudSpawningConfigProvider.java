package dev.nonamecrackers2.simpleclouds.common.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;

import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudSpawningConfig;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

public abstract class CloudSpawningConfigProvider extends DualPathProvider
{
	private final List<CloudSpawningConfig.Info> entries = Lists.newArrayList();
	
	public CloudSpawningConfigProvider(PackOutput output)
	{
		super(output, "cloud_spawning");
	}
	
	protected abstract void addEntries();
	
	protected void addEntry(CloudSpawningConfig.Info entry)
	{
		this.entries.add(entry);
	}
	
	@Override
	public CompletableFuture<?> run(CachedOutput output)
	{
		this.addEntries();
		Set<ResourceLocation> ids = Sets.newHashSet();
		List<CompletableFuture<?>> futures = Lists.newArrayList();
		this.entries.forEach(entry -> 
		{
			if (!ids.add(entry.cloudType()))
			{
				throw new IllegalArgumentException("Duplicate cloud spawning config entry " + entry.cloudType());
			}
			else
			{
				JsonObject object = entry.toJson();
				this.jsonForPaths(entry.cloudType(), p -> {
					futures.add(DataProvider.saveStable(output, object, p));
				});
			}
		});
		return CompletableFuture.allOf(futures.toArray(i -> new CompletableFuture[i]));
	}

	@Override
	public String getName()
	{
		return "Cloud Spawning Config";
	}

}
