package dev.nonamecrackers2.simpleclouds.common.noise;

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class StaticNoiseSettings extends AbstractNoiseSettings<StaticNoiseSettings>
{
	public static final Codec<StaticNoiseSettings> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
				Codec.FLOAT.fieldOf("height").forGetter(i -> i.getParam(AbstractNoiseSettings.Param.HEIGHT)),
				Codec.FLOAT.fieldOf("value_offset").forGetter(i -> i.getParam(AbstractNoiseSettings.Param.VALUE_OFFSET)),
				Codec.FLOAT.fieldOf("scale_x").forGetter(i -> i.getParam(AbstractNoiseSettings.Param.SCALE_X)),
				Codec.FLOAT.fieldOf("scale_y").forGetter(i -> i.getParam(AbstractNoiseSettings.Param.SCALE_Y)),
				Codec.FLOAT.fieldOf("scale_z").forGetter(i -> i.getParam(AbstractNoiseSettings.Param.SCALE_Z)),
				Codec.FLOAT.fieldOf("fade_distance").forGetter(i -> i.getParam(AbstractNoiseSettings.Param.FADE_DISTANCE)),
				Codec.FLOAT.fieldOf("height_offset").forGetter(i -> i.getParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET)),
				Codec.FLOAT.fieldOf("value_scale").forGetter(i -> i.getParam(AbstractNoiseSettings.Param.VALUE_SCALE))
		).apply(instance, (height, valueOffset, scaleX, scaleY, scaleZ, fadeDistance, heightOffset, valueScale) -> {
			var modifiable = new ModifiableNoiseSettings();
			modifiable.setParam(AbstractNoiseSettings.Param.HEIGHT, height);
			modifiable.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, valueOffset);
			modifiable.setParam(AbstractNoiseSettings.Param.SCALE_X, scaleX);
			modifiable.setParam(AbstractNoiseSettings.Param.SCALE_Y, scaleY);
			modifiable.setParam(AbstractNoiseSettings.Param.SCALE_Z, scaleZ);
			modifiable.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, fadeDistance);
			modifiable.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, heightOffset);
			modifiable.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, valueScale);
			return modifiable.toStatic();
		});
	});
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
}
