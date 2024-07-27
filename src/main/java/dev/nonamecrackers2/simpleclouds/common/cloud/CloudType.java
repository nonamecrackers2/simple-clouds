package dev.nonamecrackers2.simpleclouds.common.cloud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;

import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.common.noise.NoiseSettings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public record CloudType(ResourceLocation id, WeatherType weatherType, float storminess, float stormStart, float stormFadeDistance, NoiseSettings noiseConfig) implements CloudInfo
{
	private static float getOptionalRangedParam(JsonObject object, String name, float defaultValue, float min, float max) throws JsonSyntaxException
	{
		float value = GsonHelper.getAsFloat(object, name, defaultValue);
		if (value < min || value > max)
			throw new JsonSyntaxException("'" + name + "' is out of bounds. MIN: " + min + ", MAX: " + max);
		return value;
	}
	
	public static CloudType readFromJson(ResourceLocation id, JsonObject object) throws JsonSyntaxException
	{
		JsonElement element = object.has("noise_settings") ? object.get("noise_settings") : object.get("noise_layers");
		if (element == null)
			throw new JsonSyntaxException("Please include one of 'noise_settings' or 'noise_layers'");
		NoiseSettings settings = NoiseSettings.CODEC.parse(JsonOps.INSTANCE, element).resultOrPartial(error -> {
			throw new JsonSyntaxException(error);
		}).orElseThrow();
			
		if (settings.layerCount() > CloudMeshGenerator.MAX_NOISE_LAYERS)
			throw new JsonSyntaxException("Too many noise layers. Maximum amount of layers allowed is " + CloudMeshGenerator.MAX_NOISE_LAYERS);

		WeatherType weatherType = WeatherType.NONE;
		if (object.has("weather_type"))
		{
			String rawWeatherTypeId = GsonHelper.getAsString(object, "weather_type");
			boolean match = false;
			for (WeatherType type : WeatherType.values())
			{
				if (type.getSerializedName().equals(rawWeatherTypeId))
				{
					weatherType = type;
					match = true;
					break;
				}
			}
			if (!match)
				throw new JsonSyntaxException("Unknown weather type '" + rawWeatherTypeId + "'");
		}
		
		float storminess = getOptionalRangedParam(object, "storminess", 0.0F, 0.0F, CloudInfo.STORMINESS_MAX);
		float stormStart = getOptionalRangedParam(object, "storm_start", 16.0F, 0.0F, CloudInfo.STORM_START_MAX);
		float stormFadeDistance = getOptionalRangedParam(object, "storm_fade_distance", 32.0F, 0.0F, CloudInfo.STORM_FADE_DISTANCE_MAX);
		
		return new CloudType(id, weatherType, storminess, stormStart, stormFadeDistance, settings);
	}
}
