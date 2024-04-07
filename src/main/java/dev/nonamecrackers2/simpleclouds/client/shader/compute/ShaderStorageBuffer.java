package dev.nonamecrackers2.simpleclouds.client.shader.compute;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL43;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.systems.RenderSystem;

public class ShaderStorageBuffer extends BufferObject
{
	protected ShaderStorageBuffer(int id, int binding, int usage)
	{
		super(GL43.GL_SHADER_STORAGE_BUFFER, id, binding, usage);
	}
	
	public void uploadData(ByteBuffer buffer)
	{
		RenderSystem.assertOnRenderThread();
		this.assertValid();
		GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, this.id);
		GlStateManager._glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, buffer, this.usage);
		GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
	}
	
	public void allocateBuffer(int bytes)
	{
		this.uploadData(MemoryTracker.create(bytes));
	}
}
