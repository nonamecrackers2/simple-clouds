package dev.nonamecrackers2.simpleclouds.client.framebuffer;

import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

public class CloudRenderTarget extends RenderTarget
{
	private final boolean highPrecisionDepth;
	
	public CloudRenderTarget(int width, int height, boolean clearError, boolean highPrecisionDepth)
	{
		super(true);
		this.highPrecisionDepth = highPrecisionDepth;
		RenderSystem.assertOnRenderThreadOrInit();
		this.resize(width, height, clearError);
	}
	
	@Override
	public void createBuffers(int width, int height, boolean clearErrors)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		int i = RenderSystem.maxSupportedTextureSize();
		if (width > 0 && width <= i && height > 0 && height <= i)
		{
			this.viewWidth = width;
			this.viewHeight = height;
			this.width = width;
			this.height = height;
			
			this.frameBufferId = GlStateManager.glGenFramebuffers();
			this.colorTextureId = TextureUtil.generateTextureId();
			this.depthBufferId = TextureUtil.generateTextureId();
			
			GlStateManager._bindTexture(this.depthBufferId);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, 0);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
			if (this.highPrecisionDepth)
				GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT32F, this.width, this.height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (IntBuffer)null);
			else
				GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, this.width, this.height, 0, GL30.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (IntBuffer)null);

            this.filterMode = GL11.GL_NEAREST;
			GlStateManager._bindTexture(this.colorTextureId);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
			GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, this.width, this.height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (IntBuffer)null);
			
			GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);
			GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, this.colorTextureId, 0);
			GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, this.depthBufferId, 0);

			this.checkStatus();
			this.clear(clearErrors);
			this.unbindRead();
		}
		else
		{
			throw new IllegalArgumentException("Window " + width + "x" + height + " size out of bounds (max. size: " + i + ")");
		}
	}
}
