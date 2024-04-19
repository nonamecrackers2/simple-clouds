package dev.nonamecrackers2.simpleclouds.client.shader.compute;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL43;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

public class UniformBuffer extends BufferObject
{
	protected UniformBuffer(ByteBuffer data, int id, int binding, int usage)
	{
		super(GL31.GL_UNIFORM_BUFFER, id, binding, usage);
		RenderSystem.assertOnRenderThread();
		this.assertValid();
		GlStateManager._glBindBuffer(GL31.GL_UNIFORM_BUFFER, this.id);
		GlStateManager._glBufferData(GL31.GL_UNIFORM_BUFFER, data, this.usage);
		GlStateManager._glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
	}

	public void uploadData(ByteBuffer buffer)
	{
		RenderSystem.assertOnRenderThread();
		this.assertValid();
		GlStateManager._glBindBuffer(GL31.GL_UNIFORM_BUFFER, this.id);
		GlStateManager._glBufferData(GL31.GL_UNIFORM_BUFFER, buffer, this.usage);
		GlStateManager._glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
	}
}
