package dev.nonamecrackers2.simpleclouds.client.framebuffer;

import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

// https://jcgt.org/published/0002/02/09/paper.pdf and http://casual-effects.blogspot.com/2015/03/implemented-weighted-blended-order.html
public class WeightedBlendingTarget extends RenderTarget
{
	private final boolean highPrecisionDepth;
	protected int revealageTextureId;
	
	public WeightedBlendingTarget(int width, int height, boolean clearError, boolean highPrecisionDepth)
	{
		super(true);
		RenderSystem.assertOnRenderThreadOrInit();
		this.highPrecisionDepth = highPrecisionDepth;
		this.resize(width, height, clearError);
		this.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
	}
	
	@Override
	public boolean isStencilEnabled()
	{
		return false;
	}
	
	@Override
	public void enableStencil()	{}
	
	@Override
	public void destroyBuffers()
	{
		super.destroyBuffers();
		
		if (this.revealageTextureId > -1)
		{
			TextureUtil.releaseTextureId(this.revealageTextureId);
			this.revealageTextureId = -1;
		}
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
			this.revealageTextureId = TextureUtil.generateTextureId();
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

			this.setFilterMode(GL11.GL_NEAREST);
			
			GlStateManager._bindTexture(this.colorTextureId);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
			GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, this.width, this.height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (IntBuffer)null);
			
			GlStateManager._bindTexture(this.revealageTextureId);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
			GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R8, this.width, this.height, 0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, (IntBuffer)null);
			
			GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);
			GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, this.colorTextureId, 0);
			GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL11.GL_TEXTURE_2D, this.revealageTextureId, 0);
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
	
	@Override
	public void copyDepthFrom(RenderTarget other)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, other.frameBufferId);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.frameBufferId);
		GlStateManager._glBlitFrameBuffer(0, 0, other.width, other.height, 0, 0, this.width, this.height, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}
	
	@Override
	public void setFilterMode(int mode)
	{
		this.filterMode = mode;
		GlStateManager._bindTexture(this.revealageTextureId);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mode);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mode);
		GlStateManager._bindTexture(this.colorTextureId);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mode);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mode);
		GlStateManager._bindTexture(0);
	}
	
	@Override
	public void bindWrite(boolean viewport)
	{
		if (!RenderSystem.isOnRenderThread())
			RenderSystem.recordRenderCall(() -> this._bindWrite(viewport));
		else
			this._bindWrite(viewport);
	}
	
	private void _bindWrite(boolean viewport)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		GlStateManager._glBindFramebuffer(36160, this.frameBufferId);
		if (viewport)
			GlStateManager._viewport(0, 0, this.viewWidth, this.viewHeight);
		GL20.glDrawBuffers(new int[] {GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1});
	}
	
	@Override
	public void clear(boolean clearErrors)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		this.bindWrite(true);
		
		GL30.glClearBufferfv(GL11.GL_COLOR, 0, new float[] {0.0F, 0.0F, 0.0F, 0.0F});
		GL30.glClearBufferfv(GL11.GL_COLOR, 1, new float[] {1.0F, 0.0F, 0.0F, 0.0F});
		
		GL30.glClearBufferfv(GL11.GL_DEPTH, 0, new float[] {1.0F});
		
		this.unbindWrite();
	}
	
	public int getRevealageTextureId()
	{
		return this.revealageTextureId;
	}
}
