package dev.nonamecrackers2.simpleclouds.client.renderer.pipeline;

import javax.annotation.Nullable;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;

import dev.nonamecrackers2.simpleclouds.client.framebuffer.FrameBufferUtils;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import net.minecraft.client.Minecraft;

public class DefaultPipeline implements CloudsRenderPipeline
{
	protected DefaultPipeline() {}
	
	@Override
	public void prepare(Minecraft mc, SimpleCloudsRenderer renderer, PoseStack stack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ)
	{
		mc.getProfiler().push("shadow_map");
		PoseStack shadowMapStack = new PoseStack();
		shadowMapStack.setIdentity();
		renderer.renderShadowMap(shadowMapStack, camX, camY, camZ);
		mc.getProfiler().pop();
	}

	@Override
	public void afterSky(Minecraft mc, SimpleCloudsRenderer renderer, PoseStack stack, @Nullable PoseStack shadowMapStack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ) 
	{
		float[] cloudCol = renderer.getCloudColor(partialTick);
		float cloudR = (float)cloudCol[0];
		float cloudG = (float)cloudCol[1];
		float cloudB = (float)cloudCol[2];
	
		if (SimpleCloudsConfig.CLIENT.renderStormFog.get())
		{
			mc.getProfiler().push("storm_fog");
			renderer.doStormPostProcessing(stack, shadowMapStack, partialTick, projMat, camX, camY, camZ, cloudR, cloudG, cloudB);
			renderer.getBlurTarget().clear(Minecraft.ON_OSX);
			renderer.getBlurTarget().bindWrite(true);
			FrameBufferUtils.blitTargetPreservingAlpha(renderer.getStormFogTarget(), mc.getWindow().getWidth(), mc.getWindow().getHeight());
			renderer.doBlurPostProcessing(partialTick);
			mc.getMainRenderTarget().bindWrite(false);
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
			renderer.getBlurTarget().blitToScreen(mc.getWindow().getWidth(), mc.getWindow().getHeight(), false);
			RenderSystem.disableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setProjectionMatrix(projMat, VertexSorting.DISTANCE_TO_ORIGIN);
			mc.getProfiler().pop();
		}
		
		renderer.getCloudTarget().clear(Minecraft.ON_OSX);
        renderer.getCloudTarget().bindWrite(false);
        
        mc.getProfiler().push("clouds");
		stack.pushPose();
		renderer.translateClouds(stack, camX, camY, camZ);
		renderer.getMeshGenerator().render(stack, projMat, partialTick, cloudR, cloudG, cloudB);
		stack.popPose();
		mc.getProfiler().pop();
		
		renderer.copyDepthFromCloudsToMain();

		mc.getProfiler().push("clouds_post");
		renderer.doCloudPostProcessing(stack, partialTick, projMat);
		mc.getProfiler().pop();
		
		mc.getMainRenderTarget().bindWrite(false);

		mc.getProfiler().push("clouds_blit");
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		renderer.getCloudTarget().blitToScreen(mc.getWindow().getWidth(), mc.getWindow().getHeight(), false);
		RenderSystem.disableBlend();
		RenderSystem.defaultBlendFunc();
		mc.getProfiler().pop();
		
        RenderSystem.setProjectionMatrix(projMat, VertexSorting.DISTANCE_TO_ORIGIN);
	}

	@Override
	public void afterLevel(Minecraft mc, SimpleCloudsRenderer renderer, PoseStack stack, @Nullable PoseStack shadowMapStack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ) {}
}
