package dev.nonamecrackers2.simpleclouds.client.dh.event;

import com.seibel.distanthorizons.api.DhApi;

import dev.nonamecrackers2.simpleclouds.api.client.event.ModifyCloudRenderDistanceEvent;
import dev.nonamecrackers2.simpleclouds.client.dh.pipeline.DhSupportPipeline;
import dev.nonamecrackers2.simpleclouds.client.event.impl.DetermineCloudRenderPipelineEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class SimpleCloudsDhForgeEvents
{
	@SubscribeEvent
	public static void modifyRenderDistance(ModifyCloudRenderDistanceEvent event)
	{
		float renderDistance = event.getRenderDistance();
		event.setRenderDistance(Math.min(renderDistance, (float)DhApi.Delayed.configs.graphics().chunkRenderDistance().getValue() * 16.0F));
	}
	
	@SubscribeEvent
	public static void determineRenderPipeline(DetermineCloudRenderPipelineEvent event)
	{
		event.overridePipeline(DhSupportPipeline.INSTANCE);
	}
}
