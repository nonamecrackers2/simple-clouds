package dev.nonamecrackers2.simpleclouds.common.cloud;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.weather.WeatherType;
import dev.nonamecrackers2.simpleclouds.common.noise.StaticNoiseSettings;

public class CloudConstants
{
	public static final CloudType FALLBACK = new CloudType(SimpleCloudsMod.id("fallback"), WeatherType.NONE, 0.0F, 16.0F, 32.0F, StaticNoiseSettings.DEFAULT);
	public static final int CLOUD_SCALE = 8;
	public static final int CHUNK_SIZE = 32;
	public static final float REGION_SCALE = 2000.0F;
	
	private CloudConstants() {}
}
