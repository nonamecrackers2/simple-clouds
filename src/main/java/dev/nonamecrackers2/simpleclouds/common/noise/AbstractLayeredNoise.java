package dev.nonamecrackers2.simpleclouds.common.noise;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

public abstract class AbstractLayeredNoise<T extends AbstractNoiseSettings> implements NoiseSettings
{
	protected final List<T> noiseLayers;
	
	public AbstractLayeredNoise(List<T> noiseLayers)
	{
		this.noiseLayers = noiseLayers;
	}
	
	@Override
	public float[] packForShader()
	{
		float[] values = new float[] {};
		for (NoiseSettings layer : this.noiseLayers)
			values = ArrayUtils.addAll(values, layer.packForShader());
		return values;
	}

	@Override
	public int layerCount()
	{
		int count = 0;
		for (NoiseSettings layer : this.noiseLayers)
			count += layer.layerCount();
		return count;
	}
}
