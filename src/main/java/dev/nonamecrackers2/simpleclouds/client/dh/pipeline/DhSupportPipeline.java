package dev.nonamecrackers2.simpleclouds.client.dh.pipeline;

import javax.annotation.Nullable;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;

import dev.nonamecrackers2.simpleclouds.client.framebuffer.FrameBufferUtils;
import dev.nonamecrackers2.simpleclouds.client.framebuffer.WeightedBlendingTarget;
import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.renderer.WorldEffects;
import dev.nonamecrackers2.simpleclouds.client.renderer.pipeline.CloudsRenderPipeline;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.mixin.MixinRenderTargetAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.profiling.ProfilerFiller;

public class DhSupportPipeline implements CloudsRenderPipeline
{
	public static final DhSupportPipeline INSTANCE = new DhSupportPipeline();
	
	private DhSupportPipeline() {}
	
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
	public void afterSky(Minecraft mc, SimpleCloudsRenderer renderer, PoseStack stack, @Nullable PoseStack shadowMapStack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ, Frustum frustum) {}
	
	@Override
	public void beforeWeather(Minecraft mc, SimpleCloudsRenderer renderer, PoseStack stack, PoseStack shadowMapStack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ, Frustum frustum) {}
	
	@Override
	public void afterLevel(Minecraft mc, SimpleCloudsRenderer renderer, PoseStack stack, PoseStack shadowMapStack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ, Frustum frustum) {}
	
	@Override
	public void beforeDistantHorizonsApplyShader(Minecraft mc, SimpleCloudsRenderer renderer, @Nullable PoseStack shadowMapStack, PoseStack stack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ, Frustum frustum, int dhFbo)
	{
		RenderTarget cloudTarget = renderer.getCloudTarget();
		cloudTarget.clear(Minecraft.ON_OSX);
		RenderTarget transparencyTarget = renderer.getCloudTransparencyTarget();
		transparencyTarget.clear(Minecraft.ON_OSX);
		
		//TODO: Not using api
		GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, dhFbo);
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, ((MixinRenderTargetAccessor)cloudTarget).simpleclouds$getFrameBufferId());
		GL30.glBlitFramebuffer(0, 0, cloudTarget.width, cloudTarget.height, 0, 0, cloudTarget.width, cloudTarget.height, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, ((MixinRenderTargetAccessor)transparencyTarget).simpleclouds$getFrameBufferId());
		GL30.glBlitFramebuffer(0, 0, cloudTarget.width, cloudTarget.height, 0, 0, transparencyTarget.width, transparencyTarget.height, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, dhFbo);
	}
	
	@Override
	public void afterDistantHorizonsRender(Minecraft mc, SimpleCloudsRenderer renderer, @Nullable PoseStack shadowMapStack, PoseStack stack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ, Frustum frustum, int dhFbo)
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
		
		p.push("clouds_opaque");
		
		RenderTarget cloudTarget = renderer.getCloudTarget();
		cloudTarget.bindWrite(false);
		
		// Renders the clouds on to the cloud frame buffer
		CloudMeshGenerator generator = renderer.getMeshGenerator();
		SimpleCloudsRenderer.renderCloudsOpaque(generator, stack, projMat, partialTick, cloudR, cloudG, cloudB, frustum);
		
		// Render transparent cloud geometry
		p.popPush("clouds_transparent");
		
		WeightedBlendingTarget transparencyTarget = renderer.getCloudTransparencyTarget();
		
		if (generator.transparencyEnabled())
		{
			// We use weighted order independent transparency as we cannot easily sort the cloud mesh
			// More info here https://jcgt.org/published/0002/02/09/paper.pdf and http://casual-effects.blogspot.com/2015/03/implemented-weighted-blended-order.html
			renderer.copyDepthFromCloudsToTransparency();
			transparencyTarget.bindWrite(false);
			// Render the transparent geometry to the transparency framebuffer
			SimpleCloudsRenderer.renderCloudsTransparency(generator, stack, projMat, partialTick, cloudR, cloudG, cloudB, frustum, renderer.getFogStart(), renderer.getFogEnd());
		}
		
		p.pop();
		
		stack.popPose();
		
		// Render everything on to the main screen using a final composite shader
		p.push("clouds_composite");
		renderer.doFinalCompositePass(stack, partialTick, projMat);
		p.pop();
		
		p.pop();
		
		Matrix4f oldMcProjMat = RenderSystem.getProjectionMatrix();
		
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
			
			p.pop();
		}
		
		mc.getMainRenderTarget().bindWrite(false);
		
		//Kind of messed up, but we need to render to the main frame buffer but use the Distant Horizons LOD depth. We just temporarily use the copy of the depth buffer
		//we have in the cloud framebuffer and swap back after
		GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, cloudTarget.getDepthTextureId(), 0);
		RenderSystem.setProjectionMatrix(projMat, VertexSorting.DISTANCE_TO_ORIGIN); //Make minecraft use the DH proj mat
		
		// We can then render whatever we want to the main MC framebuffer while using DH LOD depth
		stack.pushPose();
		stack.translate(-camX, -camY, -camZ);
		renderLightning(renderer.getWorldEffectsManager(), renderer, mc, stack, partialTick, camX, camY, camZ);
		stack.popPose();
		
		RenderSystem.setProjectionMatrix(oldMcProjMat, VertexSorting.DISTANCE_TO_ORIGIN);
		GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, mc.getMainRenderTarget().getDepthTextureId(), 0);
	}
	
	private static void renderLightning(WorldEffects effects, SimpleCloudsRenderer renderer, Minecraft mc, PoseStack stack, float partialTick, double camX, double camY, double camZ)
	{
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder builder = tesselator.getBuilder();
		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		
		if (effects.hasLightningToRender())
		{
			float cachedFogStart = RenderSystem.getShaderFogStart();
			RenderSystem.setShaderFogStart(Float.MAX_VALUE);
			
			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			RenderSystem.setShader(GameRenderer::getRendertypeLightningShader);
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
			
			effects.forLightning(bolt -> 
			{
				if (bolt.getPosition().distance((float)camX, (float)camY, (float)camZ) <= SimpleCloudsConstants.CLOSE_THUNDER_CUTOFF && bolt.getFade(partialTick) > 0.5F)
					mc.level.setSkyFlashTime(2);
				float dist = bolt.getPosition().distance((float)camX, (float)camY, (float)camZ);
				bolt.render(stack, builder, partialTick, 1.0F, 1.0F, 1.0F, renderer.getFadeFactorForDistance(dist));
			});
			
			tesselator.end();
			
			RenderSystem.setShaderFogStart(cachedFogStart);
			
			RenderSystem.defaultBlendFunc();
		}
		
		RenderSystem.disableBlend();
	}
}
