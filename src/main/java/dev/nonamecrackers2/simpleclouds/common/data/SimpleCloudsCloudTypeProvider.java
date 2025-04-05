package dev.nonamecrackers2.simpleclouds.common.data;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.weather.WeatherType;
import dev.nonamecrackers2.simpleclouds.common.noise.AbstractNoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.ModifiableLayeredNoise;
import dev.nonamecrackers2.simpleclouds.common.noise.ModifiableNoiseSettings;
import net.minecraft.data.PackOutput;

public class SimpleCloudsCloudTypeProvider extends CloudTypeProvider
{
	public SimpleCloudsCloudTypeProvider(PackOutput output)
	{
		super(SimpleCloudsMod.MODID, output);
	}
	
	@Override
	protected void addTypes()
	{
		this.addType(cumulonimbus());
		this.addType(cumulus());
		this.addType(ittyBitty());
		this.addType(nimbostratus());
		this.addType(smallCumulus());
		this.addType(stratocumulus());
		this.addType(stratus());
	}
	
	private static CloudType cumulonimbus()
	{
		var noise = new ModifiableLayeredNoise();
		var layer1 = new ModifiableNoiseSettings();
		layer1.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, 10.0F);
		layer1.setParam(AbstractNoiseSettings.Param.HEIGHT, 32.0F);
		layer1.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, 0.0F);
		layer1.setParam(AbstractNoiseSettings.Param.SCALE_X, 30.0F);
		layer1.setParam(AbstractNoiseSettings.Param.SCALE_Y, 30.0F);
		layer1.setParam(AbstractNoiseSettings.Param.SCALE_Z, 30.0F);
		layer1.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, 1.0F);
		layer1.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, 1.0F);
		noise.addNoiseLayer(layer1);
		var layer2 = new ModifiableNoiseSettings();
		layer2.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, 32.0F);
		layer2.setParam(AbstractNoiseSettings.Param.HEIGHT, 256.0F);
		layer2.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, 0.0F);
		layer2.setParam(AbstractNoiseSettings.Param.SCALE_X, 400.0F);
		layer2.setParam(AbstractNoiseSettings.Param.SCALE_Y, 400.0F);
		layer2.setParam(AbstractNoiseSettings.Param.SCALE_Z, 400.0F);
		layer2.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, 0.0F);
		layer2.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, 1.0F);
		noise.addNoiseLayer(layer2);
		var layer3 = new ModifiableNoiseSettings();
		layer3.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, 16.0F);
		layer3.setParam(AbstractNoiseSettings.Param.HEIGHT, 256.0F);
		layer3.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, 0.0F);
		layer3.setParam(AbstractNoiseSettings.Param.SCALE_X, 30.0F);
		layer3.setParam(AbstractNoiseSettings.Param.SCALE_Y, 30.0F);
		layer3.setParam(AbstractNoiseSettings.Param.SCALE_Z, 30.0F);
		layer3.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, 0.0F);
		layer3.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, 0.1F);
		noise.addNoiseLayer(layer3);
		return new CloudType(SimpleCloudsMod.id("cumulonimbus"), WeatherType.THUNDERSTORM, 0.7F, 16.0F, 128.0F, 0.0F, noise);
	}
	
	private static CloudType cumulus()
	{
		var noise = new ModifiableLayeredNoise();
		var layer1 = new ModifiableNoiseSettings();
		layer1.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, 8.0F);
		layer1.setParam(AbstractNoiseSettings.Param.HEIGHT, 32.0F);
		layer1.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, 16.0F);
		layer1.setParam(AbstractNoiseSettings.Param.SCALE_X, 50.0F);
		layer1.setParam(AbstractNoiseSettings.Param.SCALE_Y, 100.0F);
		layer1.setParam(AbstractNoiseSettings.Param.SCALE_Z, 50.0F);
		layer1.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, -0.5F);
		layer1.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, 1.0F);
		noise.addNoiseLayer(layer1);
		var layer2 = new ModifiableNoiseSettings();
		layer2.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, 10.0F);
		layer2.setParam(AbstractNoiseSettings.Param.HEIGHT, 32.0F);
		layer2.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, 16.0F);
		layer2.setParam(AbstractNoiseSettings.Param.SCALE_X, 10.0F);
		layer2.setParam(AbstractNoiseSettings.Param.SCALE_Y, 10.0F);
		layer2.setParam(AbstractNoiseSettings.Param.SCALE_Z, 10.0F);
		layer2.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, 0.0F);
		layer2.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, 0.5F);
		noise.addNoiseLayer(layer2);
		return new CloudType(SimpleCloudsMod.id("cumulus"), WeatherType.NONE, 0.2F, 16.0F, 16.0F, 0.2F, noise);
	}
	
	private static CloudType ittyBitty()
	{
		var noise = new ModifiableNoiseSettings();
		noise.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, 10.0F);
		noise.setParam(AbstractNoiseSettings.Param.HEIGHT, 32.0F);
		noise.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, 0.0F);
		noise.setParam(AbstractNoiseSettings.Param.SCALE_X, 30.0F);
		noise.setParam(AbstractNoiseSettings.Param.SCALE_Y, 10.0F);
		noise.setParam(AbstractNoiseSettings.Param.SCALE_Z, 30.0F);
		noise.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, -0.8F);
		noise.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, 1.0F);
		return new CloudType(SimpleCloudsMod.id("itty_bitty"), WeatherType.NONE, 0.0F, 16.0F, 32.0F, 0.2F, noise);
	}
	
	private static CloudType nimbostratus()
	{
		var noise = new ModifiableLayeredNoise();
		var layer1 = new ModifiableNoiseSettings();
		layer1.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, 16.0F);
		layer1.setParam(AbstractNoiseSettings.Param.HEIGHT, 96.0F);
		layer1.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, 0.0F);
		layer1.setParam(AbstractNoiseSettings.Param.SCALE_X, 400.0F);
		layer1.setParam(AbstractNoiseSettings.Param.SCALE_Y, 400.0F);
		layer1.setParam(AbstractNoiseSettings.Param.SCALE_Z, 400.0F);
		layer1.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, 1.0F);
		layer1.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, 1.0F);
		noise.addNoiseLayer(layer1);
		var layer2 = new ModifiableNoiseSettings();
		layer2.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, 16.0F);
		layer2.setParam(AbstractNoiseSettings.Param.HEIGHT, 64.0F);
		layer2.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, 64.0F);
		layer2.setParam(AbstractNoiseSettings.Param.SCALE_X, 400.0F);
		layer2.setParam(AbstractNoiseSettings.Param.SCALE_Y, 400.0F);
		layer2.setParam(AbstractNoiseSettings.Param.SCALE_Z, 400.0F);
		layer2.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, 2.0F);
		layer2.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, 1.0F);
		noise.addNoiseLayer(layer2);
		var layer3 = new ModifiableNoiseSettings();
		layer3.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, 10.0F);
		layer3.setParam(AbstractNoiseSettings.Param.HEIGHT, 128.0F);
		layer3.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, 0.0F);
		layer3.setParam(AbstractNoiseSettings.Param.SCALE_X, 50.0F);
		layer3.setParam(AbstractNoiseSettings.Param.SCALE_Y, 50.0F);
		layer3.setParam(AbstractNoiseSettings.Param.SCALE_Z, 50.0F);
		layer3.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, 0.0F);
		layer3.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, 0.3F);
		noise.addNoiseLayer(layer3);
		return new CloudType(SimpleCloudsMod.id("nimbostratus"), WeatherType.THUNDERSTORM, 0.6F, 16.0F, 128.0F, 0.0F, noise);
	}
	
	private static CloudType smallCumulus()
	{
		var noise = new ModifiableNoiseSettings();
		noise.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, 10.0F);
		noise.setParam(AbstractNoiseSettings.Param.HEIGHT, 32.0F);
		noise.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, 0.0F);
		noise.setParam(AbstractNoiseSettings.Param.SCALE_X, 30.0F);
		noise.setParam(AbstractNoiseSettings.Param.SCALE_Y, 10.0F);
		noise.setParam(AbstractNoiseSettings.Param.SCALE_Z, 30.0F);
		noise.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, -0.5F);
		noise.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, 1.0F);
		return new CloudType(SimpleCloudsMod.id("small_cumulus"), WeatherType.NONE, 0.1F, 10.0F, 16.0F, 0.1F, noise);
	}
	
	private static CloudType stratocumulus()
	{
		var noise = new ModifiableLayeredNoise();
		var layer1 = new ModifiableNoiseSettings();
		layer1.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, 10.0F);
		layer1.setParam(AbstractNoiseSettings.Param.HEIGHT, 64.0F);
		layer1.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, 64.0F);
		layer1.setParam(AbstractNoiseSettings.Param.SCALE_X, 200.0F);
		layer1.setParam(AbstractNoiseSettings.Param.SCALE_Y, 80.0F);
		layer1.setParam(AbstractNoiseSettings.Param.SCALE_Z, 200.0F);
		layer1.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, -0.1F);
		layer1.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, 1.0F);
		noise.addNoiseLayer(layer1);
		var layer2 = new ModifiableNoiseSettings();
		layer2.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, 10.0F);
		layer2.setParam(AbstractNoiseSettings.Param.HEIGHT, 64.0F);
		layer2.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, 64.0F);
		layer2.setParam(AbstractNoiseSettings.Param.SCALE_X, 30.0F);
		layer2.setParam(AbstractNoiseSettings.Param.SCALE_Y, 30.0F);
		layer2.setParam(AbstractNoiseSettings.Param.SCALE_Z, 30.0F);
		layer2.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, 0.0F);
		layer2.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, 0.2F);
		noise.addNoiseLayer(layer2);
		return new CloudType(SimpleCloudsMod.id("stratocumulus"), WeatherType.NONE, 0.6F, 64.0F, 48.0F, 0.02F, noise);
	}
	
	private static CloudType stratus()
	{
		var noise = new ModifiableLayeredNoise();
		var layer1 = new ModifiableNoiseSettings();
		layer1.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, 10.0F);
		layer1.setParam(AbstractNoiseSettings.Param.HEIGHT, 32.0F);
		layer1.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, 0.0F);
		layer1.setParam(AbstractNoiseSettings.Param.SCALE_X, 500.0F);
		layer1.setParam(AbstractNoiseSettings.Param.SCALE_Y, 30.0F);
		layer1.setParam(AbstractNoiseSettings.Param.SCALE_Z, 100.0F);
		layer1.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, 1.0F);
		layer1.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, 1.0F);
		noise.addNoiseLayer(layer1);
		var layer2 = new ModifiableNoiseSettings();
		layer2.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, 10.0F);
		layer2.setParam(AbstractNoiseSettings.Param.HEIGHT, 32.0F);
		layer2.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, 0.0F);
		layer2.setParam(AbstractNoiseSettings.Param.SCALE_X, 10.0F);
		layer2.setParam(AbstractNoiseSettings.Param.SCALE_Y, 10.0F);
		layer2.setParam(AbstractNoiseSettings.Param.SCALE_Z, 10.0F);
		layer2.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, 0.0F);
		layer2.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, 0.5F);
		noise.addNoiseLayer(layer2);
		return new CloudType(SimpleCloudsMod.id("stratus"), WeatherType.RAIN, 0.5F, 0.0F, 32.0F, 0.0F, noise);
	}
}
