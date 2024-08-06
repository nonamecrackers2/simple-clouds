package dev.nonamecrackers2.simpleclouds.common.cloud.weather;

import net.minecraft.util.StringRepresentable;

public enum WeatherType implements StringRepresentable
{
	THUNDERSTORM("thunderstorm", true),
	THUNDER("thunder", true),
	RAIN("rain", true),
	NONE("none", false);
	
	private final String id;
	private final boolean causesDarkening;
	
	private WeatherType(String id, boolean causesDarkening)
	{
		this.id = id;
		this.causesDarkening = causesDarkening;
	}
	
	public boolean causesDarkening()
	{
		return this.causesDarkening;
	}
	
	@Override
	public String getSerializedName()
	{
		return this.id;
	}
}
