package dev.nonamecrackers2.simpleclouds.client.mesh.chunk;

import java.nio.ByteBuffer;
import java.util.Optional;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.nonamecrackers2.simpleclouds.client.mesh.lod.PreparedChunk;
import dev.nonamecrackers2.simpleclouds.client.shader.SimpleCloudsShaders;
import net.minecraft.world.phys.AABB;

public class MeshChunk
{
	private final PreparedChunk preparedChunk;
	private final MeshChunk.BufferSet opaqueBuffers;
	private final Optional<MeshChunk.BufferSet> transparentBuffers;
	private float boundsMinX;
	private float boundsMinY;
	private float boundsMinZ;
	private float boundsMaxX;
	private float boundsMaxY;
	private float boundsMaxZ;
	private float minHeight;
	private float maxHeight;
	
	public MeshChunk(PreparedChunk preparedChunk, int vertexBufferSize, int indexBufferSize, int transparentVertexBufferSize, int transparentIndexBufferSize, boolean useTransparency)
	{
		this.preparedChunk = preparedChunk;
		
		this.opaqueBuffers = new MeshChunk.BufferSet(vertexBufferSize, indexBufferSize);
		if (useTransparency)
			this.transparentBuffers = Optional.of(new MeshChunk.BufferSet(transparentVertexBufferSize, transparentIndexBufferSize));
		else
			this.transparentBuffers = Optional.empty();
		
		AABB bounds = preparedChunk.bounds();
		this.boundsMinX = (float)bounds.minX;
		this.boundsMinY = (float)bounds.minY;
		this.boundsMinZ = (float)bounds.minZ;
		this.boundsMaxX = (float)bounds.maxX;
		this.boundsMaxY = (float)bounds.maxY;
		this.boundsMaxZ = (float)bounds.maxZ;
		this.minHeight = this.boundsMinY;
		this.maxHeight = this.boundsMaxY;
	}
	
	public PreparedChunk getChunkInfo()
	{
		return this.preparedChunk;
	}
	
	public MeshChunk.BufferSet getOpaqueBuffers()
	{
		return this.opaqueBuffers;
	}
	
	public Optional<MeshChunk.BufferSet> getTransparentBuffers()
	{
		return this.transparentBuffers;
	}
	
	public void setBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ)
	{
		this.boundsMinX = minX;
		this.boundsMinY = minY;
		this.boundsMinZ = minZ;
		this.boundsMaxX = maxX;
		this.boundsMaxY = maxY;
		this.boundsMaxZ = maxZ;
	}
	
	public void setHeights(float minHeight, float maxHeight)
	{
		this.minHeight = minHeight;
		this.maxHeight = maxHeight;
	}
	
	public float getBoundsMinX()
	{
		return this.boundsMinX;
	}

	public float getBoundsMinY()
	{
		return this.boundsMinY;
	}

	public float getBoundsMinZ()
	{
		return this.boundsMinZ;
	}

	public float getBoundsMaxX()
	{
		return this.boundsMaxX;
	}

	public float getBoundsMaxY()
	{
		return this.boundsMaxY;
	}

	public float getBoundsMaxZ()
	{
		return this.boundsMaxZ;
	}
	
	public float getMinHeight()
	{
		return this.minHeight;
	}
	
	public float getMaxHeight()
	{
		return this.maxHeight;
	}

	public void destroy()
	{
		this.opaqueBuffers.destroy();
		this.transparentBuffers.ifPresent(MeshChunk.BufferSet::destroy);
	}
	
	public static class BufferSet
	{
		private int arrayObjectId = -1;
		private int vertexBufferId = -1;
		private int indexBufferId = -1;
		private @Nullable ByteBuffer vertexBuffer;
		private @Nullable ByteBuffer indexBuffer;
		private int totalIndices;
		private int totalVertices;
		private final int vertexBufferSize;
		private final int indexBufferSize;
		
		public BufferSet(int vertexBufferSize, int indexBufferSize)
		{
			this.arrayObjectId = GL30.glGenVertexArrays();
			this.vertexBufferId = GL15.glGenBuffers();
			this.indexBufferId = GL15.glGenBuffers();
			
			GL30.glBindVertexArray(this.arrayObjectId);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vertexBufferId);
			this.vertexBuffer = MemoryTracker.create(vertexBufferSize);
			GlStateManager._glBufferData(GL15.GL_ARRAY_BUFFER, this.vertexBuffer, GL15.GL_DYNAMIC_DRAW);
			SimpleCloudsShaders.POSITION_BRIGHTNESS_NORMAL_INDEX.setupBufferState();
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.indexBufferId);
			this.indexBuffer = MemoryTracker.create(indexBufferSize);
			GlStateManager._glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, this.indexBuffer, GL15.GL_DYNAMIC_DRAW);
			
			GL30.glBindVertexArray(0);
			
			this.vertexBufferSize = vertexBufferSize;
			this.indexBufferSize = indexBufferSize;
		}
		
		public void setTotalVertices(int totalVertices)
		{
			this.totalVertices = totalVertices;
		}
		
		public int getTotalVertices()
		{
			return this.totalVertices;
		}
		
		public int getVertexBufferSize()
		{
			return this.vertexBufferSize;
		}
		
		public void setTotalIndices(int totalIndices)
		{
			this.totalIndices = totalIndices;
		}
		
		public int getTotalIndices()
		{
			return this.totalIndices;
		}
		
		public int getIndexBufferSize()
		{
			return this.indexBufferSize;
		}
		
		public int getArrayObjectId()
		{
			return this.arrayObjectId;
		}
		
		public int getVertexBufferId()
		{
			return this.vertexBufferId;
		}
		
		public int getIndexBufferId()
		{
			return this.indexBufferId;
		}
		
		public void destroy()
		{
			this.totalIndices = 0;
			this.totalVertices = 0;
			
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
}
