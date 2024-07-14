package dev.nonamecrackers2.simpleclouds.common.noise;

import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import net.minecraft.util.Mth;

public abstract class AbstractNoiseSettings<T extends AbstractNoiseSettings<T>> implements NoiseSettings
{
	private float[] packedParameters;
	
	@SuppressWarnings("unchecked")
	public final T setParam(AbstractNoiseSettings.Param param, float value)
	{
		value = Mth.clamp(value, param.getMinInclusive(), param.getMaxInclusive());
		if (this.setParamRaw(param, value))
			this.packParameters();
		return (T)this;
	}
	
	public abstract float getParam(AbstractNoiseSettings.Param param);
	
	protected abstract boolean setParamRaw(AbstractNoiseSettings.Param param, float value);
	
	protected void packParameters()
	{
		float[] layerSettings = new float[AbstractNoiseSettings.Param.values().length];
		for (int i = 0; i < AbstractNoiseSettings.Param.values().length; i++)
		{
			AbstractNoiseSettings.Param param = AbstractNoiseSettings.Param.values()[i];
			layerSettings[i] = this.getParam(param);
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
	
	@Override
	public int layerCount()
	{
		return 1;
	}
	
	public static enum Param
	{
		HEIGHT(32.0F, 1.0F, CloudMeshGenerator.LOCAL_SIZE * CloudMeshGenerator.WORK_SIZE * CloudMeshGenerator.VERTICAL_CHUNK_SPAN),
		VALUE_OFFSET(-0.5F, -8.0F, 8.0F),
		SCALE_X(30.0F, 0.1F, 3200.0F),
		SCALE_Y(10.0F, 0.1F, 3200.0F),
		SCALE_Z(30.0F, 0.1F, 3200.0F),
		FADE_DISTANCE(10.0F, 1.0F, CloudMeshGenerator.LOCAL_SIZE * CloudMeshGenerator.WORK_SIZE * CloudMeshGenerator.VERTICAL_CHUNK_SPAN / 2),
		HEIGHT_OFFSET(0.0F, 0.0F, CloudMeshGenerator.LOCAL_SIZE * CloudMeshGenerator.WORK_SIZE * CloudMeshGenerator.VERTICAL_CHUNK_SPAN),
		VALUE_SCALE(1.0F, -10.0F, 10.0F);
		
		private final float defaultValue;
		private final float minInclusive;
		private final float maxInclusive;
		
		private Param(float value, float minInclusive, float maxInclusive)
		{
			this.defaultValue = value;
			this.minInclusive = minInclusive;
			this.maxInclusive = maxInclusive;
		}
		
		public float getDefaultValue()
		{
			return this.defaultValue;
		}
		
		public float getMinInclusive()
		{
			return this.minInclusive;
		}
		
		public float getMaxInclusive()
		{
			return this.maxInclusive;
		}
	}
}
