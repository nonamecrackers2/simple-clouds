package dev.nonamecrackers2.simpleclouds.client.shader.compute;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.systems.RenderSystem;

public class ShaderStorageBufferObject
{
	protected int id;
	protected final int binding;
	protected final int usage;
	protected @Nullable ByteBuffer buffer;
	
	public ShaderStorageBufferObject(int id, int binding, int usage)
	{
		this.id = id;
		this.binding = binding;
		this.usage = usage;
	}
	
	public void uploadData(ByteBuffer buffer)
	{
		RenderSystem.assertOnRenderThread();
		this.assertValid();
		GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, this.id);
		GlStateManager._glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, buffer, this.usage);
		GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
		this.buffer = buffer;
	}
	
	public void allocateBuffer(int bytes)
	{
		this.uploadData(MemoryTracker.create(bytes));
	}
	
	public void close()
	{
		RenderSystem.assertOnRenderThread();
		if (this.id != -1)
		{
			ComputeShader.LOGGER.debug("Deleting buffer id={}, binding={}", this.id, this.binding);
			GL15.glDeleteBuffers(this.id);
			this.id = -1;
		}
		if (this.buffer != null)
		{
			MemoryUtil.memFree(this.buffer);
			this.buffer = null;
		}
	}
	
	public void fetchData(Consumer<ByteBuffer> consumer, int access, int size)
	{
		RenderSystem.assertOnRenderThread();
		this.assertValid();
		GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, this.id);
		consumer.accept(GL30.glMapBufferRange(GL43.GL_SHADER_STORAGE_BUFFER, 0, size, access, this.buffer));
		GlStateManager._glUnmapBuffer(GL43.GL_SHADER_STORAGE_BUFFER);
		GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
	}
	
	public void readData(Consumer<ByteBuffer> consumer, int size)
	{
		this.fetchData(consumer, GL30.GL_MAP_READ_BIT, size);
	}
	
	public void writeData(Consumer<ByteBuffer> consumer, int size)
	{
		this.fetchData(consumer, GL30.GL_MAP_WRITE_BIT, size);
	}
	
	public void readWriteData(Consumer<ByteBuffer> consumer, int size)
	{
		this.fetchData(consumer, GL30.GL_MAP_WRITE_BIT | GL30.GL_MAP_READ_BIT, size);
	}
	
	public int getBinding()
	{
		return this.binding;
	}
	
	public int getId()
	{
		return this.id;
	}
	
	public int getUsage()
	{
		return this.usage;
	}
	
	protected void assertValid()
	{
		if (this.id == -1)
			throw new IllegalStateException("Buffer is no longer valid!");
	}
}
