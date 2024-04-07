package dev.nonamecrackers2.simpleclouds.client.shader.compute;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import org.lwjgl.opengl.GL15;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

public class BufferObject implements AutoCloseable
{
	protected final int type;
	protected int id;
	protected final int binding;
	protected final int usage;
	
	protected BufferObject(int type, int id, int binding, int usage)
	{
		this.type = type;
		this.id = id;
		this.binding = binding;
		this.usage = usage;
	}
	
	@Override
	public void close()
	{
		RenderSystem.assertOnRenderThread();
		if (this.id != -1)
		{
			ComputeShader.LOGGER.debug("Deleting buffer id={}, binding={}", this.id, this.binding);
			RenderSystem.glDeleteBuffers(this.id);
			this.id = -1;
		}
	}
	
	public void fetchData(Consumer<ByteBuffer> consumer, int access)
	{
		RenderSystem.assertOnRenderThread();
		this.assertValid();
		//TODO: Change this, not always a shader storage buffer
		GlStateManager._glBindBuffer(this.type, this.id);
		consumer.accept(GlStateManager._glMapBuffer(this.type, access));
		GlStateManager._glUnmapBuffer(this.type);
		GlStateManager._glBindBuffer(this.type, 0);
	}
	
	public void readData(Consumer<ByteBuffer> consumer)
	{
		this.fetchData(consumer, GL15.GL_READ_ONLY);
	}
	
	public void writeData(Consumer<ByteBuffer> consumer)
	{
		this.fetchData(consumer, GL15.GL_WRITE_ONLY);
	}
	
	public void readWriteData(Consumer<ByteBuffer> consumer)
	{
		this.fetchData(consumer, GL15.GL_READ_WRITE);
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
