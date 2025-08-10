package dev.nonamecrackers2.simpleclouds.common.cloud;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.api.common.cloud.weather.WeatherType;
import dev.nonamecrackers2.simpleclouds.common.noise.StaticLayeredNoise;

public class SimpleCloudsConstants
{
	//General
	public static final CloudType EMPTY = new CloudType(SimpleCloudsMod.id("empty"), WeatherType.NONE, 0.0F, 0.0F, 0.0F, 0.0F, StaticLayeredNoise.EMPTY);
	public static final int CLOUD_SCALE = 8;
	public static final int CHUNK_SIZE = 32;
	public static final float REGION_EDGE_FADE_FACTOR = 0.005F;
	public static final int MAX_CLOUD_FORMATIONS = 8;
	//Weather
	public static final float RAIN_THRESHOLD = 0.7F; // higher values means you have to be further into the clouds to experience rain
	public static final float RAIN_FADE = 0.1F;
	public static final float RAIN_VERTICAL_FADE = 32.0F;
	public static final int LIGHTNING_SPAWN_DIAMETER = 20000;
	public static final int LIGHTNING_SPAWN_ATTEMPTS = 12;
	public static final int CLOSE_THUNDER_CUTOFF = 2000;
	public static final int THUNDER_PITCH_FULL_DIST = 3000;
	public static final int THUNDER_PITCH_MINIMUM_DIST = 5000;
	//Effects
	public static final float LIGHTNING_FLASH_STRENGTH = 1.0F;
	public static final float SOUND_METERS_PER_SECOND = 2000.0F;
	//Ambient cloud mode
	public static final float AMBIENT_MODE_FADE_START = 0.25F;
	public static final float AMBIENT_MODE_FADE_END = 0.5F;
	//Generator
	//TODO: Configurable?
	public static final int SPAWN_ATTEMPTS = 10;
	public static final float MIN_SPAWN_DIST_BETWEEN_REGIONS = 500.0F;
	public static int SPAWN_RADIUS = 10000;

	private SimpleCloudsConstants() {}
}
