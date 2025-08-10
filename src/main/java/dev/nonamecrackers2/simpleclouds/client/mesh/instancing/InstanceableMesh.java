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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

public class InstanceableMesh
{
	private int arrayObjectId = -1;
	private int vertexBufferId = -1;
	private int indexBufferId = -1;
	private @Nullable ByteBuffer vertexBuffer;
	private @Nullable ByteBuffer indexBuffer;
	private int totalIndices;
	
	public InstanceableMesh(int vertexBufferSize, int indexBufferSize, VertexFormat format, Consumer<ByteBuffer> vertexBufferGenerator, Function<ByteBuffer, Integer> indexBufferGenerator)
	{
		RenderSystem.assertOnRenderThread();
		
		this.arrayObjectId = GL30.glGenVertexArrays();
		this.vertexBufferId = GL15.glGenBuffers();
		this.indexBufferId = GL15.glGenBuffers();
		
		GL30.glBindVertexArray(this.arrayObjectId);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vertexBufferId);
		this.vertexBuffer = MemoryUtil.memAlloc(vertexBufferSize);
		vertexBufferGenerator.accept(this.vertexBuffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, this.vertexBuffer, GL15.GL_STATIC_DRAW);
		format.setupBufferState();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.indexBufferId);
		this.indexBuffer = MemoryUtil.memAlloc(indexBufferSize);
		this.totalIndices = indexBufferGenerator.apply(this.indexBuffer);
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, this.indexBuffer, GL15.GL_STATIC_DRAW);
		
		GL30.glBindVertexArray(0);
	}
	
	public static InstanceableMesh defaultSide()
	{
		return new InstanceableMesh(48, 24, DefaultVertexFormat.POSITION, buffer -> 
		{
			buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.putFloat( 1.0F);
			buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.putFloat(-1.0F);
			buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.putFloat(-1.0F);
			buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.putFloat( 1.0F);
			buffer.rewind();
		}, buffer ->
		{
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
	
	public static InstanceableMesh defaultNonCulledSide()
	{
		return new InstanceableMesh(48, 48, DefaultVertexFormat.POSITION, buffer -> 
		{
			buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.putFloat( 1.0F);
			buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.putFloat(-1.0F);
			buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.putFloat(-1.0F);
			buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.putFloat( 1.0F);
			buffer.rewind();
		}, buffer ->
		{
			buffer.putInt(0);
			buffer.putInt(1);
			buffer.putInt(2);
			buffer.putInt(0);
			buffer.putInt(2);
			buffer.putInt(3);
			
			buffer.putInt(2);
			buffer.putInt(1);
			buffer.putInt(0);
			buffer.putInt(3);
			buffer.putInt(2);
			buffer.putInt(0);
			
			buffer.rewind();
			return 12;
		});
	}
	
//	public static PreparedMesh defaultCube()
//	{
//		return new PreparedMesh(576, 144, SimpleCloudsShaders.POSITION_NORMAL, buffer -> {
//			//-x
//			buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.put((byte)-1); buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)0);
//			buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.putFloat(-1.0F); buffer.put((byte)-1); buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)0);
//			buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.putFloat( 1.0F); buffer.put((byte)-1); buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)0);
//			buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.put((byte)-1); buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)0);
//			//+x
//			buffer.putFloat( 1.0F); buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.put((byte)1); buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)0);
//			buffer.putFloat( 1.0F); buffer.putFloat( 1.0F); buffer.putFloat( 1.0F); buffer.put((byte)1); buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)0);
//			buffer.putFloat( 1.0F); buffer.putFloat( 1.0F); buffer.putFloat(-1.0F); buffer.put((byte)1); buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)0);
//			buffer.putFloat( 1.0F); buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.put((byte)1); buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)0);
//			//-y
//			buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.put((byte)0); buffer.put((byte)-1); buffer.put((byte)0); buffer.put((byte)0);
//			buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.put((byte)0); buffer.put((byte)-1); buffer.put((byte)0); buffer.put((byte)0);
//			buffer.putFloat( 1.0F); buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.put((byte)0); buffer.put((byte)-1); buffer.put((byte)0); buffer.put((byte)0);
//			buffer.putFloat( 1.0F); buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.put((byte)0); buffer.put((byte)-1); buffer.put((byte)0); buffer.put((byte)0);
//			//+y
//			buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.putFloat( 1.0F); buffer.put((byte)0); buffer.put((byte)1); buffer.put((byte)0); buffer.put((byte)0);
//			buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.putFloat(-1.0F); buffer.put((byte)0); buffer.put((byte)1); buffer.put((byte)0); buffer.put((byte)0);
//			buffer.putFloat( 1.0F); buffer.putFloat( 1.0F); buffer.putFloat(-1.0F); buffer.put((byte)0); buffer.put((byte)1); buffer.put((byte)0); buffer.put((byte)0);
//			buffer.putFloat( 1.0F); buffer.putFloat( 1.0F); buffer.putFloat( 1.0F); buffer.put((byte)0); buffer.put((byte)1); buffer.put((byte)0); buffer.put((byte)0);
//			//-z
//			buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.putFloat(-1.0F); buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)-1); buffer.put((byte)0);
//			buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)-1); buffer.put((byte)0);
//			buffer.putFloat( 1.0F); buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)-1); buffer.put((byte)0);
//			buffer.putFloat( 1.0F); buffer.putFloat( 1.0F); buffer.putFloat(-1.0F); buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)-1); buffer.put((byte)0);
//			//-z
//			buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)1); buffer.put((byte)0);
//			buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.putFloat( 1.0F); buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)1); buffer.put((byte)0);
//			buffer.putFloat( 1.0F); buffer.putFloat( 1.0F); buffer.putFloat( 1.0F); buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)1); buffer.put((byte)0);
//			buffer.putFloat( 1.0F); buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.put((byte)0); buffer.put((byte)0); buffer.put((byte)1); buffer.put((byte)0);
//			
//			buffer.rewind();
//		}, buffer -> {
//			buffer.putInt(0); buffer.putInt(1); buffer.putInt(2); buffer.putInt(0); buffer.putInt(2); buffer.putInt(3); // -x
//			buffer.putInt(4); buffer.putInt(5); buffer.putInt(6); buffer.putInt(4); buffer.putInt(6); buffer.putInt(7); // +x
//			buffer.putInt(8); buffer.putInt(9); buffer.putInt(10); buffer.putInt(8); buffer.putInt(10); buffer.putInt(11); // -y
//			buffer.putInt(12); buffer.putInt(13); buffer.putInt(14); buffer.putInt(12); buffer.putInt(14); buffer.putInt(15); // +y
//			buffer.putInt(16); buffer.putInt(17); buffer.putInt(18); buffer.putInt(16); buffer.putInt(18); buffer.putInt(19); // -z
//			buffer.putInt(20); buffer.putInt(21); buffer.putInt(22); buffer.putInt(20); buffer.putInt(22); buffer.putInt(23); // +z
//			buffer.rewind();
//			return 36;
//		});
//	}
	
	public static InstanceableMesh defaultCube()
	{
		return new InstanceableMesh(96, 144, DefaultVertexFormat.POSITION, buffer -> 
		{
			buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.putFloat(-1.0F);
			buffer.putFloat( 1.0F); buffer.putFloat(-1.0F); buffer.putFloat(-1.0F);
			buffer.putFloat( 1.0F); buffer.putFloat( 1.0F); buffer.putFloat(-1.0F);
			buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.putFloat(-1.0F);
			buffer.putFloat( 1.0F); buffer.putFloat(-1.0F); buffer.putFloat( 1.0F);
			buffer.putFloat( 1.0F); buffer.putFloat( 1.0F); buffer.putFloat( 1.0F);
			buffer.putFloat(-1.0F); buffer.putFloat( 1.0F); buffer.putFloat( 1.0F);
			buffer.putFloat(-1.0F); buffer.putFloat(-1.0F); buffer.putFloat( 1.0F);
			buffer.rewind();
		}, buffer -> 
		{
			buffer.putInt(0); buffer.putInt(1); buffer.putInt(2); buffer.putInt(0); buffer.putInt(2); buffer.putInt(3); // -z
			buffer.putInt(4); buffer.putInt(7); buffer.putInt(6); buffer.putInt(4); buffer.putInt(6); buffer.putInt(5); // +z
			buffer.putInt(7); buffer.putInt(0); buffer.putInt(3); buffer.putInt(7); buffer.putInt(3); buffer.putInt(6); // -x
			buffer.putInt(1); buffer.putInt(4); buffer.putInt(5); buffer.putInt(1); buffer.putInt(5); buffer.putInt(2); // +x
			buffer.putInt(1); buffer.putInt(0); buffer.putInt(7); buffer.putInt(1); buffer.putInt(7); buffer.putInt(4); // -y
			buffer.putInt(5); buffer.putInt(6); buffer.putInt(3); buffer.putInt(5); buffer.putInt(3); buffer.putInt(2); // +y
			buffer.rewind();
			return 36;
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
