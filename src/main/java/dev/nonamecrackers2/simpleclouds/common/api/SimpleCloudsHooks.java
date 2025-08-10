package dev.nonamecrackers2.simpleclouds.common.api;

import dev.nonamecrackers2.simpleclouds.api.common.ScAPIHooks;

public class SimpleCloudsHooks implements ScAPIHooks
{
	private boolean externalWeatherControl;

	@Override
	public void setExternalWeatherControl(boolean control)
	{
		this.externalWeatherControl = control;
	}

	@Override
	public boolean isExternalWeatherControlEnabled()
	{
		return this.externalWeatherControl;
	}
}
