package dev.nonamecrackers2.simpleclouds.common.noise;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.ListCodec;

public class StaticLayeredNoise extends AbstractLayeredNoise<StaticNoiseSettings>
{
	public static final Codec<StaticLayeredNoise> CODEC = new ListCodec<>(StaticNoiseSettings.CODEC).xmap(StaticLayeredNoise::new, s -> s.noiseLayers);
	
	public StaticLayeredNoise(Collection<StaticNoiseSettings> noiseLayers)
	{
		super(ImmutableList.copyOf(noiseLayers));
	}
	
	public StaticLayeredNoise(ModifiableLayeredNoise modifiableLayeredNoise)
	{
		super(modifiableLayeredNoise.noiseLayers.stream().map(ModifiableNoiseSettings::toStatic).collect(ImmutableList.toImmutableList()));
	}
}
