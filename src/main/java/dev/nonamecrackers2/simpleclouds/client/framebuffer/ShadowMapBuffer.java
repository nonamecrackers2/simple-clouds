package dev.nonamecrackers2.simpleclouds.client.framebuffer;

import java.nio.IntBuffer;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

public class ShadowMapBuffer implements AutoCloseable
{
	private final Matrix4f projMat;
	private final float near;
	private final float far;
	private final int viewWidth;
	private final int viewHeight;
	private final int texWidth;
	private final int texHeight;
	private final boolean hasColor;
	private int bufferId = -1;
	private int depthTextureId = -1;
	private int colorTextureId = -1;
	
	public ShadowMapBuffer(int viewWidth, int viewHeight, int texWidth, int texHeight, float near, float far, boolean withColor, boolean comparisonDepth)
	{
		this.projMat = new Matrix4f().setOrtho(0.0F, viewWidth, viewHeight, 0.0F, near, far);
		this.near = near;
		this.far = far;
		this.viewWidth = viewWidth;
		this.viewHeight = viewHeight;
		this.texWidth = texHeight;
		this.texHeight = texHeight;
		this.hasColor = withColor;
		
		this.bufferId = GlStateManager.glGenFramebuffers();
		this.depthTextureId = TextureUtil.generateTextureId();
		
		GlStateManager._bindTexture(this.depthTextureId);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		if (comparisonDepth)
		{
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL30.GL_COMPARE_REF_TO_TEXTURE);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_FUNC, GL11.GL_GEQUAL);
			GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, texWidth, texHeight, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_UNSIGNED_BYTE, (IntBuffer)null);
		}
		else
		{
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, 0);
			GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, texWidth, texHeight, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (IntBuffer)null);
		}
		
		if (withColor)
		{
			this.colorTextureId = TextureUtil.generateTextureId();
			GlStateManager._bindTexture(this.colorTextureId);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, 0);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
			GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
			GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB8, texWidth, texHeight, 0, GL11.GL_RGB, GL11.GL_FLOAT, (IntBuffer)null);
		}
		
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.bufferId);
		GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, this.depthTextureId, 0);
		if (withColor)
			GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, this.colorTextureId, 0);
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
		
		checkFrameBufferStatus();
		
		GlStateManager._bindTexture(0);
	}
	
	public void bind()
	{
		RenderSystem.assertOnRenderThread();
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.bufferId);
		GlStateManager._viewport(0, 0, this.texWidth, this.texHeight);
	}
	
	public void clear(boolean osx)
	{
		RenderSystem.assertOnRenderThread();
		GlStateManager._clearDepth(1.0D);
		int bit = GL11.GL_DEPTH_BUFFER_BIT;
		if (this.hasColor())
		{
			GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 0.0F);
			bit |= GL11.GL_COLOR_BUFFER_BIT;
		}
		RenderSystem.clear(bit, osx);
	}
	
	public void unbind()
	{
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}
	
	public float getNear()
	{
		return this.near;
	}
	
	public float getFar()
	{
		return this.far;
	}
	
	public int getViewWidth()
	{
		return this.viewWidth;
	}

	public int getViewHeight()
	{
		return this.viewHeight;
	}

	public int getTexWidth()
	{
		return this.texWidth;
	}

	public int getTexHeight()
	{
		return this.texHeight;
	}

	public int getFramebufferId()
	{
		return this.bufferId;
	}
	
	public int getDepthTexId()
	{
		return this.depthTextureId;
	}
	
	public int getColorTexId()
	{
		if (!this.hasColor())
			throw new IllegalStateException("Buffer does not have color");
		return this.colorTextureId;
	}
	
	public boolean hasColor()
	{
		return this.hasColor;
	}
	
	public Matrix4f getProjMatrix()
	{
		return this.projMat;
	}
	
	@Override
	public void close()
	{
		if (this.depthTextureId != -1)
		{
			TextureUtil.releaseTextureId(this.depthTextureId);
			this.depthTextureId = -1;
		}
		
		if (this.colorTextureId != -1)
		{
			TextureUtil.releaseTextureId(this.colorTextureId);
			this.colorTextureId = -1;
		}
		
		if (this.bufferId != -1)
		{
			GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
			GlStateManager._glDeleteFramebuffers(this.bufferId);
			this.bufferId = -1;
		}
	}
	
	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "[buffer=" + this.bufferId + ",depth=" + this.depthTextureId + ",color=" + this.colorTextureId + ",width=" + this.texWidth + ",height=" + this.texHeight + "]";
	}
	
	private static void checkFrameBufferStatus()
	{
		RenderSystem.assertOnRenderThreadOrInit();
		int i = GlStateManager.glCheckFramebufferStatus(36160);
		if (i != 36053)
		{
			if (i == 36054)
				throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
			else if (i == 36055)
				throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
			else if (i == 36059)
				throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
			else if (i == 36060)
				throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
			else if (i == 36061)
				throw new RuntimeException("GL_FRAMEBUFFER_UNSUPPORTED");
			else if (i == 1285)
				throw new RuntimeException("GL_OUT_OF_MEMORY");
			else
				throw new RuntimeException("checkFramebufferStatus returned unknown status:" + i);
		}
	}
}
