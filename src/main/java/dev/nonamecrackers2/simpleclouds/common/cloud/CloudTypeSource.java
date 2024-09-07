package dev.nonamecrackers2.simpleclouds.common.cloud;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

public interface CloudTypeSource
{
	@Nullable CloudType getCloudTypeForId(ResourceLocation id);
	
	CloudType[] getIndexedCloudTypes();
	
	default Optional<CloudType> getCloudTypeFromRawId(String id)
	{
		ResourceLocation loc = ResourceLocation.tryParse(id);
		if (id != null)
			return Optional.ofNullable(this.getCloudTypeForId(loc));
		return Optional.empty();
	}
}
