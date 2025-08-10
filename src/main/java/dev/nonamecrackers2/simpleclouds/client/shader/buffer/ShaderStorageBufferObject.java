package dev.nonamecrackers2.simpleclouds.client.shader.buffer;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

public class ShaderStorageBufferObject implements WithBinding
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/ShaderStorageBufferObject");
	private static int maxSize = -1;
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
	
	public static int getMaxSize()
	{
		if (maxSize == -1)
			maxSize = GL11.glGetInteger(GL43.GL_MAX_SHADER_STORAGE_BLOCK_SIZE);
		return maxSize;
	}
	
	public void bindToProgram(String name, int programId)
	{
		this.bindToProgram(name, programId, true);
	}
	
	public void optionalBindToProgram(String name, int programId)
	{
		this.bindToProgram(name, programId, false);
	}
	
	private void bindToProgram(String name, int programId, boolean throwIfMissing)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		this.assertValid();
		int index = GL43.glGetProgramResourceIndex(programId, GL43.GL_SHADER_STORAGE_BLOCK, name);
		if (index == -1 && throwIfMissing)
			throw new NullPointerException("Unknown block index with name '" + name + "'");
		if (index != -1)
			GL43.glShaderStorageBlockBinding(programId, index, this.binding);
	}
	
	public void uploadData(ByteBuffer buffer)
	{
		int size = buffer.remaining();
		if (size > getMaxSize())
			throw new IllegalArgumentException("Size exceeds the SSBO maximum supported by current hardware, wanted: " + size + " bytes, maximum: " + maxSize + " bytes");
		RenderSystem.assertOnRenderThread();
		this.assertValid();
		GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, this.id);
		GlStateManager._glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, buffer, this.usage);
		GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
		this.buffer = buffer;
	}
	
	public int allocateBuffer(int bytes)
	{
		int size = Math.min(bytes, getMaxSize());
		this.uploadData(MemoryUtil.memAlloc(size));
		return size;
	}
	
	@Override
	public void close()
	{
		RenderSystem.assertOnRenderThread();
		if (this.id != -1)
		{
			LOGGER.debug("Deleting buffer id={}, binding={}", this.id, this.binding);
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
		if (size <= 0)
			throw new IllegalArgumentException("Invalid size, please use a size greater than 0");
		GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, this.id);
		consumer.accept(GL30.glMapBufferRange(GL43.GL_SHADER_STORAGE_BUFFER, 0, size, access, this.buffer));
		GlStateManager._glUnmapBuffer(GL43.GL_SHADER_STORAGE_BUFFER);
		GlStateManager._glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
	}
	
	public void readData(Consumer<ByteBuffer> consumer, int size)
	{
		this.fetchData(consumer, GL30.GL_MAP_READ_BIT, size);
	}
	
	public void writeData(Consumer<ByteBuffer> consumer, int size, boolean invalidate)
	{
		int access = GL30.GL_MAP_WRITE_BIT;
		if (invalidate)
			access |= GL30.GL_MAP_INVALIDATE_BUFFER_BIT;
		this.fetchData(consumer, access, size);
	}
	
	public void readWriteData(Consumer<ByteBuffer> consumer, int size)
	{
		this.fetchData(consumer, GL30.GL_MAP_WRITE_BIT | GL30.GL_MAP_READ_BIT, size);
	}
	
	@Override
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
	
	@Override
	public String toString()
	{
		return String.format("SSBO[binding=%s,id=%s,usage=%s]", this.binding, this.id, this.usage);
	}
}
