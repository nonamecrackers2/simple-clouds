package dev.nonamecrackers2.simpleclouds.client.event.impl;

import javax.annotation.Nullable;

import dev.nonamecrackers2.simpleclouds.client.renderer.pipeline.CloudsRenderPipeline;
import net.neoforged.bus.api.Event;

public class DetermineCloudRenderPipelineEvent extends Event
{
	private final CloudsRenderPipeline defaultPipeline;
	private @Nullable CloudsRenderPipeline overridenPipeline;
	
	public DetermineCloudRenderPipelineEvent(CloudsRenderPipeline defaultPipeline)
	{
		this.defaultPipeline = defaultPipeline;
	}
	
	public CloudsRenderPipeline getRenderPipeline()
	{
		return this.defaultPipeline;
	}
	
	public @Nullable CloudsRenderPipeline getOverridenPipeline()
	{
		return this.overridenPipeline;
	}
	
	public void overridePipeline(@Nullable CloudsRenderPipeline pipeline)
	{
		this.overridenPipeline = pipeline;
	}
}
