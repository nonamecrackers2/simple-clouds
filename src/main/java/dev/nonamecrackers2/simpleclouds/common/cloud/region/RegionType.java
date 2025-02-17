package dev.nonamecrackers2.simpleclouds.common.cloud.region;

import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

@Deprecated
@ScheduledForRemoval
public interface RegionType
{
	RegionType.Result getCloudTypeIndexAt(float x, float z, float scale, int totalCloudTypes);
	
	@Deprecated
	public static record Result(int index, float fade) {}
}
