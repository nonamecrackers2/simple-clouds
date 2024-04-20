package dev.nonamecrackers2.simpleclouds.common.noise;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class AbstractNoiseSettings implements NoiseSettings
{
	private float[] packedParameters;
	
	public final AbstractNoiseSettings setParam(AbstractNoiseSettings.Param param, float... values)
	{
		if (values.length != param.size)
			throw new IllegalArgumentException("Length of values does not match parameter required length");
		if (this.setParamRaw(param, values))
			this.packParameters();
		return this;
	}
	
	public final float[] getParam(AbstractNoiseSettings.Param param)
	{
		float[] internal = this.getParamRaw(param);
		return Arrays.copyOf(internal, internal.length);
	}
	
	protected abstract float[] getParamRaw(AbstractNoiseSettings.Param param);
	
	protected abstract boolean setParamRaw(AbstractNoiseSettings.Param param, float[] values);
	
	protected void packParameters()
	{
		float[] layerSettings = new float[Param.TOTAL_SIZE];
		int currentIndex = 0;
		for (var param : AbstractNoiseSettings.Param.values())
		{
			float[] value = this.getParam(param);
			for (int i = 0; i < param.getSize(); i++)
			{
				int index = currentIndex + i;
				layerSettings[index] = value[i];
			}
			currentIndex += param.getSize();
		}
		this.packedParameters = layerSettings; 
	}
	
	@Override
	public float[] packForShader()
	{
		if (this.packedParameters == null)
			this.packParameters();
		return this.packedParameters;
	}
	
	public static enum Param
	{
		HEIGHT(1, () -> new float[] { 32.0F }),
		THRESHOLD(1, () -> new float[] { 0.5F }),
		FADE_THRESHOLD(1, () -> new float[] { 0.4F }),
		SCALE(3, () -> new float[] { 30.0F, 10.0F, 30.0F });
		
		public static final int TOTAL_SIZE = Arrays.stream(values()).collect(Collectors.summingInt(Param::getSize));
		public static final int TOTAL_SIZE_BYTES = TOTAL_SIZE * 4;
		private final int size;
		private final Supplier<float[]> defaultValue;
		
		private Param(int size, Supplier<float[]> defaultValue)
		{
			this.size = size;
			this.defaultValue = defaultValue;
		}
		
		public float[] makeDefaultValue()
		{
			return this.defaultValue.get();
		}
		
		public int getSize()
		{
			return this.size;
		}
	}
}
