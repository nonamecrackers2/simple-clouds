package dev.nonamecrackers2.simpleclouds.common.cloud;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.weather.WeatherType;
import dev.nonamecrackers2.simpleclouds.common.noise.StaticNoiseSettings;

public class SimpleCloudsConstants
{
	//General
	public static final CloudType FALLBACK = new CloudType(SimpleCloudsMod.id("fallback"), WeatherType.NONE, 0.0F, 16.0F, 32.0F, StaticNoiseSettings.DEFAULT);
	public static final int CLOUD_SCALE = 8;
	public static final int CHUNK_SIZE = 32;
	public static final float REGION_SCALE = 2000.0F;
	//Weather
	public static final float RAIN_THRESHOLD = 0.7F; // higher values means you have to be further into the clouds to experience rain
	public static final float RAIN_FADE = 0.1F;
	public static final float RAIN_VERTICAL_FADE = 32.0F;
	public static final int LIGHTNING_SPAWN_DIAMETER = 20000;
	public static final int LIGHTNING_SPAWN_ATTEMPTS = 12;
	public static final int CLOSE_THUNDER_CUTOFF = 3000;
	public static final int THUNDER_PITCH_FULL_DIST = 3000;
	public static final int THUNDER_PITCH_MINIMUM_DIST = 5000;
	//Effects
	public static final float LIGHTNING_FLASH_STRENGTH = 1.0F;
	
	private SimpleCloudsConstants() {}
}
