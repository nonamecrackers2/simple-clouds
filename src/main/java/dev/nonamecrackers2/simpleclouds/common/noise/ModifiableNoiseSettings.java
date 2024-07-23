package dev.nonamecrackers2.simpleclouds.common.noise;

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.Maps;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

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
	public <T> DataResult<T> encode(DynamicOps<T> ops, T prefix)
	{
		return StaticNoiseSettings.CODEC.encode(this.toStatic(), ops, prefix);
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
