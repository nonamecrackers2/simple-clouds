package dev.nonamecrackers2.simpleclouds.common.cloud;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;

import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.common.noise.AbstractLayeredNoise;
import dev.nonamecrackers2.simpleclouds.common.noise.AbstractNoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.ModifiableLayeredNoise;
import dev.nonamecrackers2.simpleclouds.common.noise.ModifiableNoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.NoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.StaticLayeredNoise;
import dev.nonamecrackers2.simpleclouds.common.noise.StaticNoiseSettings;
import net.minecraft.util.Mth;

public interface CloudInfo
{
	public static final float STORMINESS_MAX = 1.0F;
	public static final float STORM_START_MAX = CloudMeshGenerator.LOCAL_SIZE * CloudMeshGenerator.WORK_SIZE * CloudMeshGenerator.VERTICAL_CHUNK_SPAN;
	public static final float STORM_FADE_DISTANCE_MAX = 1600.0F;
	
	float storminess();
	
	float stormStart();
	
	float stormFadeDistance();
	
	NoiseSettings noiseConfig();
	
	default JsonObject toJson() throws JsonSyntaxException
	{
		JsonObject object = new JsonObject();
		object.add("noise_settings", NoiseSettings.CODEC.encodeStart(JsonOps.INSTANCE, this.noiseConfig()).resultOrPartial(error -> {
			throw new JsonSyntaxException(error);
		}).orElseThrow());
//		if (this.noiseConfig() instanceof AbstractNoiseSettings<?> settings)
//		{
//			StaticNoiseSettings toSerialize;
//			if (settings instanceof StaticNoiseSettings staticSettings)
//				toSerialize = staticSettings;
//			else if (settings instanceof ModifiableNoiseSettings modifiable)
//				toSerialize = modifiable.toStatic();
//			else
//				throw new IllegalStateException("Unable to encode noise config: unknown type");
//			object.add("noise_settings", StaticNoiseSettings.CODEC.encodeStart(JsonOps.INSTANCE, toSerialize).resultOrPartial(error -> {
//				throw new JsonSyntaxException(error);
//			}).orElseThrow());
//		}
//		else if (this.noiseConfig() instanceof AbstractLayeredNoise<?> layered)
//		{
//			StaticLayeredNoise toSerialize;
//			if (layered instanceof StaticLayeredNoise staticSettings)
//				toSerialize = staticSettings;
//			else if (layered instanceof ModifiableLayeredNoise modifiable)
//				toSerialize = modifiable.toStatic();
//			else
//				throw new IllegalStateException("Unable to encode noise config: unknown type");
//			object.add("noise_layers", StaticLayeredNoise.CODEC.encodeStart(JsonOps.INSTANCE, toSerialize).resultOrPartial(error -> {
//				throw new JsonSyntaxException(error);
//			}).orElseThrow());
//		}
//		else
//		{
//			throw new IllegalStateException("Unable to encode noise config: unknown type");
//		}
		object.addProperty("storminess", Mth.clamp(this.storminess(), 0.0F, CloudInfo.STORMINESS_MAX));
		object.addProperty("storm_start", Mth.clamp(this.stormStart(), 0.0F, CloudInfo.STORM_START_MAX));
		object.addProperty("storm_fade_distance", Mth.clamp(this.stormFadeDistance(), 0.0F, CloudInfo.STORM_FADE_DISTANCE_MAX));
		return object;
	}
}
