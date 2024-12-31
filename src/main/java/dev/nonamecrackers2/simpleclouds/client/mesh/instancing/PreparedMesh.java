package dev.nonamecrackers2.simpleclouds.client.mesh.instancing;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

public class PreparedMesh
{
	private int arrayObjectId = -1;
	private int vertexBufferId = -1;
	private int indexBufferId = -1;
	private @Nullable ByteBuffer vertexBuffer;
	private @Nullable ByteBuffer indexBuffer;
	private int totalIndices;
	
	public PreparedMesh(int vertexBufferSize, int indexBufferSize, VertexFormat format, Consumer<ByteBuffer> vertexBufferGenerator, Function<ByteBuffer, Integer> indexBufferGenerator)
	{
		RenderSystem.assertOnRenderThread();
		
		this.arrayObjectId = GL30.glGenVertexArrays();
		this.vertexBufferId = GL15.glGenBuffers();
		this.indexBufferId = GL15.glGenBuffers();
		
		GL30.glBindVertexArray(this.arrayObjectId);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vertexBufferId);
		this.vertexBuffer = MemoryTracker.create(vertexBufferSize);
		vertexBufferGenerator.accept(this.vertexBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, this.vertexBuffer, GL15.GL_STATIC_DRAW);
		format.setupBufferState();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.indexBufferId);
		this.indexBuffer = MemoryTracker.create(indexBufferSize);
		this.totalIndices = indexBufferGenerator.apply(this.indexBuffer);
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, this.indexBuffer, GL15.GL_STATIC_DRAW);
		
		GL30.glBindVertexArray(0);
	}
	
	public static PreparedMesh defaultSide()
	{
		return new PreparedMesh(48, 24, DefaultVertexFormat.POSITION, buffer -> {
			buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.putFloat( 1.0F);
			buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.putFloat(-1.0F);
			buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.putFloat(-1.0F);
			buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.putFloat( 1.0F);
			buffer.rewind();
		}, buffer -> {
			buffer.putInt(0);
			buffer.putInt(1);
			buffer.putInt(2);
			buffer.putInt(0);
			buffer.putInt(2);
			buffer.putInt(3);
			buffer.rewind();
			return 6;
		});
	}
	
	public static PreparedMesh defaultCube()
	{
		return new PreparedMesh(96, 144, DefaultVertexFormat.POSITION, buffer -> {
			buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.putFloat(-1.0F);
			buffer.putFloat( 1.0F); buffer.putFloat(-1.0F); buffer.putFloat(-1.0F);
			buffer.putFloat( 1.0F); buffer.putFloat( 1.0F); buffer.putFloat(-1.0F);
			buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.putFloat(-1.0F);
			buffer.putFloat( 1.0F); buffer.putFloat(-1.0F); buffer.putFloat( 1.0F);
			buffer.putFloat( 1.0F); buffer.putFloat( 1.0F); buffer.putFloat( 1.0F);
			buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.putFloat( 1.0F);
			buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.putFloat( 1.0F);
			buffer.rewind();
		}, buffer -> {
			buffer.putInt(0); buffer.putInt(1); buffer.putInt(2); buffer.putInt(0); buffer.putInt(2); buffer.putInt(3); // -z
			buffer.putInt(4); buffer.putInt(7); buffer.putInt(6); buffer.putInt(4); buffer.putInt(6); buffer.putInt(5); // +z
			buffer.putInt(7); buffer.putInt(0); buffer.putInt(3); buffer.putInt(7); buffer.putInt(3); buffer.putInt(6); // -x
			buffer.putInt(1); buffer.putInt(4); buffer.putInt(5); buffer.putInt(1); buffer.putInt(5); buffer.putInt(2); // +x
			buffer.putInt(1); buffer.putInt(0); buffer.putInt(7); buffer.putInt(1); buffer.putInt(7); buffer.putInt(4); // -y
			buffer.putInt(5); buffer.putInt(6); buffer.putInt(3); buffer.putInt(5); buffer.putInt(3); buffer.putInt(2); // +y
			buffer.rewind();
			return 6;
		});
	}
	
	public void drawInstanced(int count)
	{
		RenderSystem.assertOnRenderThread();
		
		GL30.glBindVertexArray(this.arrayObjectId);
		GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, this.totalIndices, GL11.GL_UNSIGNED_INT, 0L, count);
	}
	
	public void destroy()
	{
		this.totalIndices = 0;
		
		if (this.arrayObjectId >= 0)
		{
			RenderSystem.glDeleteVertexArrays(this.arrayObjectId);
			this.arrayObjectId = -1;
		}
		
		if (this.vertexBufferId >= 0)
		{
			RenderSystem.glDeleteBuffers(this.vertexBufferId);
			this.vertexBufferId = -1;
		}
		
		if (this.vertexBuffer != null)
		{
			MemoryUtil.memFree(this.vertexBuffer);
			this.vertexBuffer = null;
		}
		
		if (this.indexBufferId >= 0)
		{
			RenderSystem.glDeleteBuffers(this.indexBufferId);
			this.indexBufferId = -1;
		}
		
		if (this.indexBuffer != null)
		{
			MemoryUtil.memFree(this.indexBuffer);
			this.indexBuffer = null;
		}
	}
}
