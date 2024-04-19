package dev.nonamecrackers2.simpleclouds.common.noise;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.Maps;

public class ModifiableNoiseSettings extends AbstractNoiseSettings
{
	private final Map<AbstractNoiseSettings.Param, float[]> values = Maps.newEnumMap(AbstractNoiseSettings.Param.class);
	
	public ModifiableNoiseSettings()
	{
		for (AbstractNoiseSettings.Param param : AbstractNoiseSettings.Param.values())
			this.values.put(param, param.makeDefaultValue());
	}
	
	@Override
	protected float[] getParamRaw(AbstractNoiseSettings.Param param)
	{
		return Objects.requireNonNull(this.values.get(param), "Value is missing for param '" + param + "'");
	}

	@Override
	protected boolean setParamRaw(AbstractNoiseSettings.Param param, float[] values)
	{
		float[] previous = this.values.get(param);
		if (!Arrays.equals(previous, values))
		{
			this.values.put(param, values);
			return true;
		}
		return false;
	}

	public StaticNoiseSettings toStatic()
	{
		return new StaticNoiseSettings(this);
	}
}
