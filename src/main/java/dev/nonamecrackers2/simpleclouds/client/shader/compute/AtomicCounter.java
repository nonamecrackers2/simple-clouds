package dev.nonamecrackers2.simpleclouds.client.shader.compute;

import java.nio.ByteBuffer;

import org.apache.commons.lang3.mutable.MutableInt;
import org.lwjgl.opengl.GL42;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.systems.RenderSystem;

public class AtomicCounter extends BufferObject
{
	protected AtomicCounter(int type, int id, int binding, int usage)
	{
		super(type, id, binding, usage);
		RenderSystem.assertOnRenderThread();
		this.assertValid();
		GlStateManager._glBindBuffer(type, this.id);
		ByteBuffer buffer = MemoryTracker.create(4);
		buffer.putInt(0, 0);
		GlStateManager._glBufferData(type, buffer, usage);
		GlStateManager._glBindBuffer(type, 0);
	}
	
	public int get()
	{
		MutableInt value = new MutableInt(-1);
		this.readData(b -> {
			value.setValue(b.getInt());
		});
		if (value.intValue() == -1)
			throw new RuntimeException("Failed to fetch atomic counter value");
		return value.intValue();
	}
	
	public void set(int value)
	{
		this.writeData(b -> {
			b.putInt(0, value);
		});
	}
}
