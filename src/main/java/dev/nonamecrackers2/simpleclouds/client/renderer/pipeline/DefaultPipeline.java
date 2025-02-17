package dev.nonamecrackers2.simpleclouds.client.renderer.pipeline;

import javax.annotation.Nullable;

import org.joml.Matrix4f;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;

import dev.nonamecrackers2.simpleclouds.client.framebuffer.FrameBufferUtils;
import dev.nonamecrackers2.simpleclouds.client.framebuffer.WeightedBlendingTarget;
import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.world.FogRenderMode;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.material.FogType;

public class DefaultPipeline implements CloudsRenderPipeline
{
	protected DefaultPipeline() {}
	
	@Override
	public void prepare(Minecraft mc, SimpleCloudsRenderer renderer, PoseStack stack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ, Frustum frustum)
	{
		mc.getProfiler().push("shadow_map");
		
		PoseStack shadowMapStack = new PoseStack();
		shadowMapStack.setIdentity();
		renderer.renderShadowMap(shadowMapStack, camX, camY, camZ);
		
		mc.getProfiler().pop();
	}

	@Override
	public void afterSky(Minecraft mc, SimpleCloudsRenderer renderer, PoseStack stack, @Nullable PoseStack shadowMapStack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ, Frustum frustum) 
	{
		float[] cloudCol = renderer.getCloudColor(partialTick);
		float cloudR = (float)cloudCol[0];
		float cloudG = (float)cloudCol[1];
		float cloudB = (float)cloudCol[2];
	
		ProfilerFiller p = mc.getProfiler();
		
		// Clouds
		
		p.push("clouds");
        
		stack.pushPose();
		
		renderer.translateClouds(stack, camX, camY, camZ); // Prepare render for origin of camera
		
		// Render opaque cloud geometry
		p.push("clouds_opaque");
		
		// Clears the cloud framebuffer and sets it as the current one
		RenderTarget cloudTarget = renderer.getCloudTarget();
		cloudTarget.clear(Minecraft.ON_OSX);
		cloudTarget.bindWrite(false);
		
		// Renders the clouds on to the cloud frame buffer
		CloudMeshGenerator generator = renderer.getMeshGenerator();
		SimpleCloudsRenderer.renderCloudsOpaque(generator, stack, projMat, renderer.getFogStart(), renderer.getFogEnd(), partialTick, cloudR, cloudG, cloudB, frustum);
		
		// Here we copy the depth from the cloud frame buffer to the main one, so we can have correct depth information with the
		// rest of the Minecraft world
		renderer.copyDepthFromCloudsToMain();

		// Render transparent cloud geometry
		p.popPush("clouds_transparent");
		
		WeightedBlendingTarget transparencyTarget = renderer.getCloudTransparencyTarget();
		transparencyTarget.clear(Minecraft.ON_OSX);
		
		if (generator.transparencyEnabled())
		{
			// We use weighted order independent transparency as we cannot easily sort the cloud mesh
			// More info here https://jcgt.org/published/0002/02/09/paper.pdf and http://casual-effects.blogspot.com/2015/03/implemented-weighted-blended-order.html
			renderer.copyDepthFromCloudsToTransparency(); // Copy the depth data from the cloud framebuffer so we don't get weird depth issues
			transparencyTarget.bindWrite(false);
			
			// Render the transparent geometry to the transparency framebuffer
			SimpleCloudsRenderer.renderCloudsTransparency(generator, stack, projMat, renderer.getFogStart(), renderer.getFogEnd(), partialTick, cloudR, cloudG, cloudB, frustum);
		}
		
		p.pop();
		
		stack.popPose();
		
		// Render everything on to the main screen using a final composite shader
		p.push("clouds_composite");
		renderer.doFinalCompositePass(stack, partialTick, projMat);
		p.pop();
		
		p.pop();
		
		// Storm Fog
		
		if (SimpleCloudsConfig.CLIENT.renderStormFog.get())
		{
			p.push("storm_fog");
			
			// Renders the storm fog at a lower resolution
			renderer.doStormPostProcessing(stack, shadowMapStack, partialTick, projMat, camX, camY, camZ, cloudR, cloudG, cloudB);
			
			// Next we blit the storm fog to a higher resolution texture and apply a box blur
			RenderTarget target = renderer.getBlurTarget();
			target.clear(Minecraft.ON_OSX); // Clear old contents on the blur framebuffer
			target.bindWrite(true); // Bind write and resize viewport
			// Here we blit the contents of the storm fog framebuffer on to the blur framebuffer. A special function is used here
			// to preserve the alpha channel when rendering
			FrameBufferUtils.blitTargetPreservingAlpha(renderer.getStormFogTarget(), mc.getWindow().getWidth(), mc.getWindow().getHeight());
			// Blurs the storm fog
			renderer.doBlurPostProcessing(partialTick);
			// Renders the storm fog to the screen
			mc.getMainRenderTarget().bindWrite(false);
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
			renderer.getBlurTarget().blitToScreen(mc.getWindow().getWidth(), mc.getWindow().getHeight(), false);
			RenderSystem.disableBlend();
			RenderSystem.defaultBlendFunc();
			// Need to do this here because blitToScreen messes up the projection matrix and doesn't set it back
			RenderSystem.setProjectionMatrix(projMat, VertexSorting.DISTANCE_TO_ORIGIN);
			
			p.pop();
		}
		
		// Set the frame buffer back to the main one so everything else can render normally
		mc.getMainRenderTarget().bindWrite(false);
	}
	
	@Override
	public void beforeWeather(Minecraft mc, SimpleCloudsRenderer renderer, PoseStack stack, PoseStack shadowMapStack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ, Frustum frustum)
	{
		if (SimpleCloudsConfig.CLIENT.fogMode.get() == FogRenderMode.SCREEN_SPACE && mc.gameRenderer.getMainCamera().getFluidInCamera() == FogType.NONE)
		{
			renderer.doScreenSpaceWorldFog(stack, projMat, partialTick);
			mc.getMainRenderTarget().bindWrite(false);
		}
	}

	@Override
	public void afterLevel(Minecraft mc, SimpleCloudsRenderer renderer, PoseStack stack, @Nullable PoseStack shadowMapStack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ, Frustum frustum)
	{
//		mc.getProfiler().push("clouds_debug");
//		stack.pushPose();
//		renderer.translateClouds(stack, camX, camY, camZ);
//		SimpleCloudsRenderer.renderCloudsDebug(renderer.getMeshGenerator(), stack, projMat, partialTick, renderer.getFogStart(), renderer.getFogEnd(), frustum, false, true);
//		stack.popPose();
//		mc.getProfiler().pop();
	}
}
