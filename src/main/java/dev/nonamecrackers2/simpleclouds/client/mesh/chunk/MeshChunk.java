package dev.nonamecrackers2.simpleclouds.client.mesh.chunk;

import java.nio.ByteBuffer;
import java.util.Optional;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.nonamecrackers2.simpleclouds.client.mesh.lod.PreparedChunk;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import net.minecraft.util.Mth;
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
	private int ticksSinceLastGen;
	private boolean fadeEnabled;
	private float alpha;
	private float alphaO;
	
	public MeshChunk(PreparedChunk preparedChunk, int opaqueElements, int opaqueElementOffset, int bytesPerOpaqueElement, int transparentElements, int transparentElementOffset, int bytesPerTransparentElement, boolean useTransparency)
	{
		this.preparedChunk = preparedChunk;
		
		this.opaqueBuffers = new MeshChunk.BufferSet(opaqueElements, opaqueElementOffset, bytesPerOpaqueElement);
		if (useTransparency)
			this.transparentBuffers = Optional.of(new MeshChunk.BufferSet(transparentElements, transparentElementOffset, bytesPerTransparentElement));
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
	
	public void tick()
	{
		this.ticksSinceLastGen++;
		
		this.alphaO = this.alpha;
		if (this.fadeEnabled && this.alpha < 1.0F)
		{
			this.alpha += SimpleCloudsRenderer.CHUNK_FADE_IN_ALPHA_PER_TICK;
			if (this.alpha > 1.0F)
				this.alpha = 1.0F;
		}
	}
	
	public void setFadeEnabled(boolean flag)
	{
		this.fadeEnabled = flag;
	}
	
	public void resetAlpha()
	{
		this.alpha = 0.0F;
		this.alphaO = 0.0F;
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
	
	public void clearChunk()
	{
		this.opaqueBuffers.setTotalElementCount(0);
		this.transparentBuffers.ifPresent(bufferSet -> bufferSet.setTotalElementCount(0));
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
	
	public void resetLastGenTime()
	{
		this.ticksSinceLastGen = 0;
	}
	
	public int getTicksSinceLastGen()
	{
		return this.ticksSinceLastGen;
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
	
	public float getAlpha(float partialTick)
	{
		return Mth.lerp(partialTick, this.alphaO, this.alpha);
	}

	public void destroy()
	{
		this.opaqueBuffers.destroy();
		this.transparentBuffers.ifPresent(MeshChunk.BufferSet::destroy);
	}
	
	public static class BufferSet
	{
		private int bufferId = -1;
		private @Nullable ByteBuffer buffer;
		private int elementCount;
		private final int bufferSize;
		private final int maxElements;
		private final int elementOffset;
		
		public BufferSet(int maxElements, int elementOffset, int bytesPerElement)
		{
			this.bufferId = GL15.glGenBuffers();
			this.maxElements = maxElements;
			this.elementOffset = elementOffset;
			this.bufferSize = maxElements * bytesPerElement;
			this.buffer = MemoryUtil.memAlloc(this.bufferSize);
			GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, this.bufferId);
			GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, this.buffer, GL15.GL_DYNAMIC_DRAW);
			GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
		}
		
		public void setTotalElementCount(int count)
		{
			this.elementCount = count;
		}
		
		public int getElementCount()
		{
			return this.elementCount;
		}
		
		public int getMaxElements()
		{
			return this.maxElements;
		}
		
		public int getElementOffset()
		{
			return this.elementOffset;
		}
		
		public int getBufferSize()
		{
			return this.bufferSize;
		}
		
		public int getBufferId()
		{
			return this.bufferId;
		}
		
		public void destroy()
		{
			this.elementCount = 0;
			
			if (this.bufferId >= 0)
			{
				RenderSystem.glDeleteBuffers(this.bufferId);
				this.bufferId = -1;
			}
			
			if (this.buffer != null)
			{
				MemoryUtil.memFree(this.buffer);
				this.buffer = null;
			}
		}
	}
}
