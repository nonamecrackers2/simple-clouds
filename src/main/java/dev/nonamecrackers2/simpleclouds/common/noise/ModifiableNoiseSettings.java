package dev.nonamecrackers2.simpleclouds.common.noise;

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.Maps;

public class ModifiableNoiseSettings extends AbstractNoiseSettings<ModifiableNoiseSettings>
{
	private final Map<AbstractNoiseSettings.Param, Float> values = Maps.newEnumMap(AbstractNoiseSettings.Param.class);
	
	public ModifiableNoiseSettings()
	{
		for (AbstractNoiseSettings.Param param : AbstractNoiseSettings.Param.values())
			this.values.put(param, param.getDefaultValue());
	}
	
	public ModifiableNoiseSettings(AbstractNoiseSettings<?> settings)
	{
		for (AbstractNoiseSettings.Param param : AbstractNoiseSettings.Param.values())
			this.values.put(param, settings.getParam(param));
	}
	
	@Override
	public float getParam(AbstractNoiseSettings.Param param)
	{
		return Objects.requireNonNull(this.values.get(param), "Value is missing for param '" + param + "'");
	}

	@Override
	protected boolean setParamRaw(AbstractNoiseSettings.Param param, float value)
	{
		float previous = this.values.get(param);
		if (previous != value)
		{
			this.values.put(param, value);
			return true;
		}
		return false;
	}
	
	public StaticNoiseSettings toStatic()
	{
		return new StaticNoiseSettings(this);
	}
}
