package dev.nonamecrackers2.simpleclouds.client.event.impl;

import net.minecraftforge.eventbus.api.Event;

public class ModifyCloudRenderDistanceEvent extends Event
{
	private float renderDistance;
	
	public ModifyCloudRenderDistanceEvent(float renderDistance)
	{
		this.renderDistance = renderDistance;
	}
	
	public float getRenderDistance()
	{
		return this.renderDistance;
	}
	
	public void setRenderDistance(float distance)
	{
		this.renderDistance = distance;
	}
}
