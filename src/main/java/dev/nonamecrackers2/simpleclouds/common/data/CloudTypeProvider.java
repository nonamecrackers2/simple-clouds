package dev.nonamecrackers2.simpleclouds.common.data;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudInfo;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

public abstract class CloudTypeProvider extends DualPathProvider
{
	private final String modid;
	private final Map<ResourceLocation, CloudInfo> cloudTypes = Maps.newHashMap();
	
	public CloudTypeProvider(String modid, PackOutput output)
	{
		super(output, "cloud_types");
		this.modid = modid;
	}
	
	protected abstract void addTypes();
	
	protected void addType(String id, CloudInfo info)
	{
		this.cloudTypes.put(new ResourceLocation(this.modid, id), info);
	}
	
	protected void addType(CloudType type)
	{
		this.addType(type.id().getPath(), type);
	}
	
	@Override
	public CompletableFuture<?> run(CachedOutput output)
	{
		this.addTypes();
		Set<ResourceLocation> ids = Sets.newHashSet();
		List<CompletableFuture<?>> futures = Lists.newArrayList();
		this.cloudTypes.forEach((id, info) -> 
		{
			if (!ids.add(id))
			{
				throw new IllegalArgumentException("Duplicate cloud type " + id);
			}
			else
			{
				JsonObject object = info.toJson();
				this.jsonForPaths(id, p -> {
					futures.add(DataProvider.saveStable(output, object, p));
				});
			}
		});
		return CompletableFuture.allOf(futures.toArray(i -> new CompletableFuture[i]));
	}

	@Override
	public String getName()
	{
		return "Cloud Types";
	}
}
