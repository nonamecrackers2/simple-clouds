package dev.nonamecrackers2.simpleclouds.common.noise;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;

public interface NoiseSettings
{
	@SuppressWarnings("rawtypes")
	public static final List<Decoder> VALID_DECODERS = ImmutableList.of(StaticNoiseSettings.CODEC, StaticLayeredNoise.CODEC);
	public static final Codec<NoiseSettings> CODEC = new Codec<>()
	{
		@SuppressWarnings("unchecked")
		@Override
		public <T> DataResult<Pair<NoiseSettings, T>> decode(DynamicOps<T> ops, T input)
		{
			for (Decoder<NoiseSettings> decoder : VALID_DECODERS)
			{
				DataResult<Pair<NoiseSettings, T>> result = decoder.decode(ops, input);
				if (result.result().isPresent())
					return result;
			}
			return DataResult.error(() -> "Could not decode noise settings");
		}
		
		@Override
		public <T> DataResult<T> encode(NoiseSettings input, DynamicOps<T> ops, T prefix)
		{
			return input.encode(ops, prefix);
		}
	};
	public static final NoiseSettings EMPTY = new NoiseSettings()
	{
		@Override
		public float[] packForShader()
		{
			return new float[0];
		}
		
		@Override
		public int layerCount()
		{
			return 0;
		}
		
		@Override
		public <T> DataResult<T> encode(DynamicOps<T> ops, T prefix)
		{
			return DataResult.success(ops.emptyList());
		}
	};
	
	<T> DataResult<T> encode(DynamicOps<T> ops, T prefix);
	
	float[] packForShader();
	
	int layerCount();
}
