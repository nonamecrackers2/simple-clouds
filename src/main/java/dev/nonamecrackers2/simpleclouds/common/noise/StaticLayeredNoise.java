package dev.nonamecrackers2.simpleclouds.common.noise;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class StaticLayeredNoise extends AbstractLayeredNoise<StaticNoiseSettings>
{
	public static final Codec<StaticLayeredNoise> CODEC = Codec.list(StaticNoiseSettings.CODEC).xmap(list -> {
		ImmutableList.Builder<StaticNoiseSettings> builder = ImmutableList.builder();
		for (StaticNoiseSettings settings : list)
			builder.add(settings);
		return new StaticLayeredNoise(builder.build());
	}, StaticLayeredNoise::getNoiseLayers);
	
	public StaticLayeredNoise(Collection<StaticNoiseSettings> noiseLayers)
	{
		super(ImmutableList.copyOf(noiseLayers));
	}
	
	private StaticLayeredNoise(ImmutableList<StaticNoiseSettings> noiseLayers)
	{
		super(noiseLayers);
	}
	
	public StaticLayeredNoise(ModifiableLayeredNoise modifiableLayeredNoise)
	{
		super(modifiableLayeredNoise.noiseLayers.stream().map(ModifiableNoiseSettings::toStatic).collect(ImmutableList.toImmutableList()));
	}
	
	@Override
	public <T> DataResult<T> encode(DynamicOps<T> ops, T prefix)
	{
		return CODEC.encode(this, ops, prefix);
	}
}
