package dev.nonamecrackers2.simpleclouds.common.cloud;

import java.nio.ByteBuffer;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;

import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.common.cloud.weather.WeatherType;
import dev.nonamecrackers2.simpleclouds.common.noise.NoiseSettings;
import net.minecraft.util.Mth;

public interface CloudInfo
{
	public static final int BYTES_PER_TYPE = 24;
	public static final float STORMINESS_MAX = 1.0F;
	public static final float STORM_START_MAX = CloudMeshGenerator.LOCAL_SIZE * CloudMeshGenerator.WORK_SIZE * CloudMeshGenerator.VERTICAL_CHUNK_SPAN;
	public static final float STORM_FADE_DISTANCE_MAX = 1600.0F;
	public static final float TRANSPARENCY_FADE_MAX = 32.0F;
	
	WeatherType weatherType();
	
	float storminess();
	
	float stormStart();
	
	float stormFadeDistance();
	
	float transparencyFade();
	
	NoiseSettings noiseConfig();
	
	default JsonObject toJson() throws JsonSyntaxException
	{
		JsonObject object = new JsonObject();
		object.add("noise_settings", NoiseSettings.CODEC.encodeStart(JsonOps.INSTANCE, this.noiseConfig()).resultOrPartial(error -> {
			throw new JsonSyntaxException(error);
		}).orElseThrow());
		object.addProperty("weather_type", this.weatherType().getSerializedName());
		object.addProperty("storminess", Mth.clamp(this.storminess(), 0.0F, STORMINESS_MAX));
		object.addProperty("storm_start", Mth.clamp(this.stormStart(), 0.0F, STORM_START_MAX));
		object.addProperty("storm_fade_distance", Mth.clamp(this.stormFadeDistance(), 0.0F, STORM_FADE_DISTANCE_MAX));
		object.addProperty("transparency_fade", Mth.clamp(this.transparencyFade(), 0.0F, TRANSPARENCY_FADE_MAX));
		return object;
	}
	
	default int packToBuffer(ByteBuffer b, int layerIndex)
	{
		int layerCount = this.noiseConfig().layerCount();
		b.putInt(layerIndex);
		b.putInt(layerIndex + layerCount);
		b.putFloat(this.storminess());
		b.putFloat(this.stormStart());
		b.putFloat(this.stormFadeDistance());
		b.putFloat(this.transparencyFade());
		return layerIndex + layerCount;
	}
}
