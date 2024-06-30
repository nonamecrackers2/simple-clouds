package dev.nonamecrackers2.simpleclouds.common.noise;

import java.util.Collection;

import com.google.common.collect.ImmutableList;

public class StaticLayeredNoise extends AbstractLayeredNoise<StaticNoiseSettings>
{
	public StaticLayeredNoise(Collection<StaticNoiseSettings> noiseLayers)
	{
		super(ImmutableList.copyOf(noiseLayers));
	}
	
	public StaticLayeredNoise(ModifiableLayeredNoise modifiableLayeredNoise)
	{
		super(modifiableLayeredNoise.noiseLayers.stream().map(ModifiableNoiseSettings::toStatic).collect(ImmutableList.toImmutableList()));
	}
}
