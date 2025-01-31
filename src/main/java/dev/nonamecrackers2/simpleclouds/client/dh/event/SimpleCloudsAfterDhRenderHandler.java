package dev.nonamecrackers2.simpleclouds.client.dh.event;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiAfterRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;

import dev.nonamecrackers2.simpleclouds.client.dh.SimpleCloudsDhCompatHandler;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.renderer.pipeline.CloudsRenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class SimpleCloudsAfterDhRenderHandler extends DhApiAfterRenderEvent
{
	@Override
	public void afterRender(DhApiEventParam<Void> event)
	{
		SimpleCloudsRenderer renderer = SimpleCloudsRenderer.getInstance();
		CloudsRenderPipeline pipeline = SimpleCloudsRenderer.getRenderPipeline();
		Minecraft mc = Minecraft.getInstance();
		Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
		
		Matrix4f projMat = SimpleCloudsDhCompatHandler._getDhProjMat();
		Matrix4f modelView = SimpleCloudsDhCompatHandler._getDhModelViewMat();
		PoseStack stack = new PoseStack();
		stack.setIdentity();
		stack.last().pose().set(modelView);
		float partialTick = mc.getPartialTick();
		
		int fbo = SimpleCloudsDhCompatHandler._getDhFramebufferId();
		
		pipeline.afterDistantHorizonsRender(mc, renderer, renderer.getShadowMapStack(), stack, projMat, partialTick, camPos.x, camPos.y, camPos.z, renderer.getCullFrustum(), fbo);
		
		SimpleCloudsDhCompatHandler._updateDhFramebufferId(0);
		SimpleCloudsDhCompatHandler._updateCachedDhState(null, null);
	}
}
