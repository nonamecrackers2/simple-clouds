package dev.nonamecrackers2.simpleclouds.common.cloud;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;

import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.common.noise.NoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.StaticLayeredNoise;
import dev.nonamecrackers2.simpleclouds.common.noise.StaticNoiseSettings;
import net.minecraft.util.GsonHelper;

public record CloudType(float storminess, float stormStart, float stormFadeDistance, NoiseSettings noiseConfig) implements CloudInfo
{
	private static float getOptionalRangedParam(JsonObject object, String name, float defaultValue, float min, float max) throws JsonSyntaxException
	{
		float value = GsonHelper.getAsFloat(object, name, defaultValue);
		if (value < min || value > max)
			throw new JsonSyntaxException("'" + name + "' is out of bounds. MIN: " + min + ", MAX: " + max);
		return value;
	}
	
	public static CloudType readFromJson(JsonObject object) throws JsonSyntaxException
	{
		NoiseSettings settings;
		if (object.has("noise_settings"))
		{
			settings = StaticNoiseSettings.CODEC.parse(JsonOps.INSTANCE, object.get("noise_settings")).resultOrPartial(error -> {
				throw new JsonSyntaxException(error);
			}).orElseThrow();
		}
		else if (object.has("noise_layers"))
		{
			settings = StaticLayeredNoise.CODEC.parse(JsonOps.INSTANCE, object.get("noise_layers")).resultOrPartial(error -> {
				throw new JsonSyntaxException(error);
			}).orElseThrow();
		}
		else
		{
			throw new JsonSyntaxException("No noise settings specified. Please include 'noise_settings' or 'noise_layers'");
		}
			
		if (settings.layerCount() > CloudMeshGenerator.MAX_NOISE_LAYERS)
			throw new JsonSyntaxException("Too many noise layers. Maximum amount of layers allowed is " + CloudMeshGenerator.MAX_NOISE_LAYERS);
		
		float storminess = getOptionalRangedParam(object, "storminess", 0.0F, 0.0F, CloudInfo.STORMINESS_MAX);
		float stormStart = getOptionalRangedParam(object, "storm_start", 16.0F, 0.0F, CloudInfo.STORM_START_MAX);
		float stormFadeDistance = getOptionalRangedParam(object, "storm_fade_distance", 32.0F, 0.0F, CloudInfo.STORM_FADE_DISTANCE_MAX);
		
		return new CloudType(storminess, stormStart, stormFadeDistance, settings);
	}
}
