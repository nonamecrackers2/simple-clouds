package dev.nonamecrackers2.simpleclouds.common.noise;

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;

public class StaticNoiseSettings extends AbstractNoiseSettings<StaticNoiseSettings>
{
	public static final StaticNoiseSettings DEFAULT = new StaticNoiseSettings();
	private final Map<AbstractNoiseSettings.Param, Float> values;
	
	public StaticNoiseSettings(AbstractNoiseSettings<?> settings)
	{
		ImmutableMap.Builder<AbstractNoiseSettings.Param, Float> builder = ImmutableMap.builder();
		for (AbstractNoiseSettings.Param param : AbstractNoiseSettings.Param.values())
			builder.put(param, settings.getParam(param));
		this.values = builder.build();
		this.packParameters();
	}
	
	private StaticNoiseSettings()
	{
		ImmutableMap.Builder<AbstractNoiseSettings.Param, Float> builder = ImmutableMap.builder();
		for (AbstractNoiseSettings.Param param : AbstractNoiseSettings.Param.values())
			builder.put(param, param.getDefaultValue());
		this.values = builder.build();
		this.packParameters();
	}
	
	@Override
	public float getParam(AbstractNoiseSettings.Param param)
	{
		return Objects.requireNonNull(this.values.get(param), "Value is missing for param '" + param + "'");
	}

	@Override
	protected boolean setParamRaw(AbstractNoiseSettings.Param param, float values)
	{
		return false;
	}
	
	public ModifiableNoiseSettings toModifiable()
	{
		return new ModifiableNoiseSettings(this);
	}
}
