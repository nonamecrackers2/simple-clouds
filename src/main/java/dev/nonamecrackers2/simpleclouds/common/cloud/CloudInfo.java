package dev.nonamecrackers2.simpleclouds.common.cloud;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.nonamecrackers2.simpleclouds.common.noise.NoiseSettings;

public interface CloudInfo
{
//	public static final Codec<CloudInfo> CODEC = RecordCodecBuilder.create(instance -> {
//		return instance.group(
//				Codec.FLOAT.fieldOf("storminess").forGetter(CloudInfo::storminess),
//				Codec.FLOAT.fieldOf("storm_start_y").forGetter(CloudInfo::stormStart),
//				Codec.FLOAT.fieldOf("storm_fade_distance").forGetter(CloudInfo::stormFadeDistance)
//		).apply(instance, CloudType::new);
//	});
	
	float storminess();
	
	float stormStart();
	
	float stormFadeDistance();
	
	NoiseSettings noiseConfig();
}
