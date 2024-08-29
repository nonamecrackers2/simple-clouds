package dev.nonamecrackers2.simpleclouds.common.cloud.weather;

import net.minecraft.util.StringRepresentable;

public enum WeatherType implements StringRepresentable
{
	THUNDERSTORM("thunderstorm", true, true, true),
	THUNDER("thunder", true, false, true),
	RAIN("rain", true, true, false),
	NONE("none", false, false, false);
	
	private final String id;
	private final boolean causesDarkening;
	private final boolean includesRain;
	private final boolean includesThunder;
	
	private WeatherType(String id, boolean causesDarkening, boolean includesRain, boolean includesThunder)
	{
		this.id = id;
		this.causesDarkening = causesDarkening;
		this.includesRain = includesRain;
		this.includesThunder = includesThunder;
	}
	
	public boolean causesDarkening()
	{
		return this.causesDarkening;
	}
	
	public boolean includesRain()
	{
		return this.includesRain;
	}
	
	public boolean includesThunder() 
	{
		return this.includesThunder;
	}
	
	@Override
	public String getSerializedName()
	{
		return this.id;
	}
}
