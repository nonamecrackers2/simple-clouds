package dev.nonamecrackers2.simpleclouds.common.cloud.region;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeSource;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import net.minecraft.resources.ResourceLocation;

public interface CloudGetter extends CloudTypeSource
{
	CloudGetter EMPTY = new CloudGetter()
	{
		@Override
		public CloudType getCloudTypeForId(ResourceLocation id)
		{
			if (id.toString().equals("simpleclouds:empty"))
				return SimpleCloudsConstants.EMPTY;
			return null;
		}
		
		@Override
		public CloudType[] getIndexedCloudTypes()
		{
			return new CloudType[] { SimpleCloudsConstants.EMPTY };
		}
		
		@Override
		public List<CloudRegion> getClouds()
		{
			return ImmutableList.of();
		}
		
		@Override
		public Pair<CloudType, Float> getCloudTypeAtPosition(float x, float z)
		{
			return Pair.of(SimpleCloudsConstants.EMPTY, 0.0F);
		}
	};
	
	List<CloudRegion> getClouds();
	
	Pair<CloudType, Float> getCloudTypeAtPosition(float x, float z);
	
	default Pair<CloudType, Float> getCloudTypeAtWorldPos(float x, float z)
	{
		return this.getCloudTypeAtPosition(x / (float)SimpleCloudsConstants.CLOUD_SCALE, z / (float)SimpleCloudsConstants.CLOUD_SCALE);
	}
}
