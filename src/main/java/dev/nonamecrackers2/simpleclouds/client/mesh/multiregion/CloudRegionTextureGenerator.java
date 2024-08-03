package dev.nonamecrackers2.simpleclouds.client.mesh.multiregion;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Math;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.platform.TextureUtil;

import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudInfo;

public class CloudRegionTextureGenerator
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/CloudRegionTextureGenerator");
	private static final int BYTES_PER_PIXEL = 8;
	private final CloudMeshGenerator.LevelOfDetailConfig lodConfig;
	private final CloudInfo[] cloudTypes;
	private final CloudRegionTextureGenerator.BufferState[] swapBuffers = new CloudRegionTextureGenerator.BufferState[2];
	private final int textureSize;
	private final float cloudRegionScale;
	private @Nullable Thread thread;
	private @Nullable Throwable threadException;
	private int availableBuffer;
	private int currentBuffer;
	private float scrollX;
	private float scrollZ;
	private float offsetX;
	private float offsetZ;
	
	public CloudRegionTextureGenerator(CloudMeshGenerator.LevelOfDetailConfig lodConfig, CloudInfo[] cloudTypes, int textureSize, float cloudRegionScale)
	{
		if (cloudTypes.length > MultiRegionCloudMeshGenerator.MAX_CLOUD_TYPES)
			throw new IllegalArgumentException("Too many cloud types! The maximum allowed is " + MultiRegionCloudMeshGenerator.MAX_CLOUD_TYPES);
		this.lodConfig = lodConfig;
		this.cloudTypes = cloudTypes;
		this.textureSize = textureSize;
		this.cloudRegionScale = cloudRegionScale;
		
		for (int i = 0; i < this.swapBuffers.length; i++)
			this.swapBuffers[i] = new CloudRegionTextureGenerator.BufferState(this.textureSize, this.lodConfig.getLods().length + 1);
		
		this.thread = new Thread(() -> {
			while (true)
				this.asyncTick();
		});
		this.thread.setName("Cloud Region Texture Generator Thread");
		this.thread.setUncaughtExceptionHandler((t, e) -> this.threadException = e);
	}
	
	public void start()
	{
		if (this.thread == null)
			throw new IllegalStateException("This generator is no longer valid!");
		if (this.thread.isAlive())
			throw new IllegalStateException("This generator is already running!");
		this.thread.start();
	}
	
	public void update(float scrollX, float scrollZ, float offsetX, float offsetZ)
	{
		this.scrollX = scrollX;
		this.scrollZ = scrollZ;
		this.offsetX = offsetX;
		this.offsetZ = offsetZ;
	}
	
	public int getAvailableRegionTextureId()
	{
		return this.swapBuffers[this.availableBuffer].getTextureId();
	}
	
	public void tick()
	{
		if (this.threadException != null)
			throw new RuntimeException("An uncaught exception occured while generating a cloud region texture buffer", this.threadException);
		
		var buffer = this.swapBuffers[this.availableBuffer];
		if (buffer.checkDirtyAndUnmark())
		{
			buffer.isUploading = true;
			buffer.uploadToTexture();
			buffer.update(this.scrollX, this.scrollZ, this.offsetX, this.offsetZ);
			buffer.isUploading = false;
		}
	}
	
	private void asyncTick()
	{
		var buffer = this.swapBuffers[this.currentBuffer];
		if (buffer != null && !buffer.isUploading())
		{
			buffer.isGenerating = true;
			this.generateTexture(buffer);
			buffer.isGenerating = false;
			buffer.isDirty = true;
			this.availableBuffer = this.currentBuffer;
			this.currentBuffer++;
			if (this.currentBuffer >= this.swapBuffers.length)
				this.currentBuffer = 0;
		}
	}
	
	private void generateTexture(CloudRegionTextureGenerator.BufferState buffer)
	{
		for (int x = 0; x < buffer.textureSize; x++)
		{
			for (int y = 0; y < buffer.textureSize; y++)
			{
				for (int z = 0; z < buffer.layers; z++)
				{
					float scale = 1.0F;
					if (z > 0)
						scale = (float)this.lodConfig.getLods()[z - 1].chunkScale();
					int index = (x + y * buffer.textureSize + z * buffer.textureSize * buffer.textureSize) * BYTES_PER_PIXEL;
					Vector2d uv = new Vector2d((float)x, (float)y).sub((float)buffer.textureSize / 2.0F, (float)buffer.textureSize / 2.0F).mul(scale).add(this.scrollX, this.scrollZ).add(this.offsetX, this.offsetZ).div((double)this.cloudRegionScale);
					var info = getCloudTypeIndexAt(uv, this.cloudTypes.length);
					//buffer.textureBuffer.putFloat(index, (float)info.getLeft());
					//buffer.textureBuffer.putFloat(index + 4, info.getRight().floatValue());
				}
			}
		}
	}
	
	private static double hash12(Vector2d p)
	{
		Vector3d p3 = new Vector3d(p.x * 0.1031D, p.y * 0.1031D, p.x * 0.1031D);
		p3.sub(p3.floor(new Vector3d()));
		double dot = p3.dot(new Vector3d(p3.y + 33.33F, p3.z + 33.33F, p3.x + 33.33F));
		p3.add(dot, dot, dot);
		double finalResult = (p3.x + p3.y) * p3.z;
		return finalResult - Math.floor(finalResult);
	}
	
	public static Pair<Integer, Double> getCloudTypeIndexAt(Vector2d pos, int cloudTypes) 
	{
		Vector2d indexUv = pos.floor(new Vector2d());
		Vector2d fractUv = new Vector2d(pos).sub(indexUv);

		double minimumDist = 8.0D;  
		Vector2d closestCoord = null;
		Vector2d closestPoint = null;
		for (double y = -1.0D; y <= 1.0D; y++) 
		{
			for (double x = -1.0D; x <= 1.0D; x++) 
			{
				Vector2d neighbor = new Vector2d(x, y);
	            Vector2d point = new Vector2d(hash12(new Vector2d(indexUv).add(neighbor)) * 1.0D);
				Vector2d coord = new Vector2d(neighbor).add(point).sub(fractUv);
				double dist = coord.length();
				if (dist < minimumDist) 
				{
					minimumDist = dist;
	                closestCoord = coord;
	                closestPoint = point;
				}
			}
		}
	    minimumDist = 8.0D;
	    for (double y = -1.0D; y <= 1.0D; y++) 
		{
			for (double x = -1.0D; x <= 1.0D; x++) 
			{
				Vector2d neighbor = new Vector2d(x, y);
				Vector2d point = new Vector2d(hash12(new Vector2d(indexUv).add(neighbor)) * 1.0D);
				Vector2d coord = new Vector2d(neighbor).add(point).sub(fractUv);
				if (closestCoord.distance(coord) > 0.0D)
				{
					Vector2d firstTerm = new Vector2d(closestCoord).add(coord).mul(0.5D);
					Vector2d secondTerm = new Vector2d(coord).sub(closestCoord).normalize();
					double dot = firstTerm.dot(secondTerm);
					minimumDist = Math.min(minimumDist, dot);
				}
			}
		}
	    int index = (int)Math.floor(hash12(closestPoint) * (double)cloudTypes);
	    double fade = 1.0D - Math.min(minimumDist * 3.0D, 1.0D);
	    return Pair.of(index, fade);
	}
	
	public void close()
	{
		for (int i = 0; i < this.swapBuffers.length; i++)
		{
			if (this.swapBuffers[i] != null)
				this.swapBuffers[i].close();
			this.swapBuffers[i] = null;
		}
		
		try{
			this.thread.join(5000L); //TODO: make the thread while loop detect when we want it to stop
		} catch (InterruptedException e) {
			LOGGER.error("Failed to close texture generator thread: ", e);
		} finally {
			this.thread = null;
		}
	}
	
	public static class BufferState
	{
		private final int textureSize;
		private final int layers;
		private @Nullable ByteBuffer textureBuffer;
		private int textureId = -1;
		private boolean isGenerating;
		private boolean isDirty;
		private boolean isUploading;
		private float scrollX;
		private float scrollZ;
		private float offsetX;
		private float offsetZ;
		
		private BufferState(int textureSize, int layers)
		{
			this.textureSize = textureSize;
			this.layers = layers;
			
			int size = this.textureSize * this.textureSize * this.layers * BYTES_PER_PIXEL;
			this.textureBuffer = MemoryTracker.create(size);
			
			this.textureId = TextureUtil.generateTextureId();
			GL11.glBindTexture(GL12.GL_TEXTURE_3D, this.textureId);
			GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
			GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
			GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
			GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			GL12.glTexImage3D(GL12.GL_TEXTURE_3D, 0, GL30.GL_RG32F, this.textureSize, this.textureSize, this.layers, 0, GL30.GL_RG, GL11.GL_FLOAT, (IntBuffer)null);
			GL11.glBindTexture(GL12.GL_TEXTURE_3D, 0);
		}
		
		private void update(float scrollX, float scrollZ, float offsetX, float offsetZ)
		{
			this.scrollX = scrollX;
			this.scrollZ = scrollZ;
			this.offsetX = offsetX;
			this.offsetZ = offsetZ;
		}
		
		private void uploadToTexture()
		{
			if (this.textureId == -1 || this.textureBuffer == null)
				throw new IllegalStateException("This buffer is no longer valid!");
			GL11.glBindTexture(GL12.GL_TEXTURE_3D, this.textureId);
			GL12.glTexSubImage3D(GL12.GL_TEXTURE_3D, 0, 0, 0, 0, this.textureSize, this.textureSize, this.layers, GL30.GL_RG, GL11.GL_FLOAT, this.textureBuffer);
			GL11.glBindTexture(GL12.GL_TEXTURE_3D, 0);
		}

		public boolean checkDirtyAndUnmark()
		{
			if (this.isDirty)
			{
				this.isDirty = false;
				return true;
			}
			else
			{
				return false;
			}
		}
		
		public boolean isGenerating()
		{
			return this.isGenerating;
		}
		
		public boolean isUploading()
		{
			return this.isUploading;
		}
		
		public boolean canUseTexture()
		{
			return !this.isGenerating();
		}
		
		public int getTextureId()
		{
			return this.textureId;
		}
		
		public void close()
		{
			if (this.textureBuffer != null)
			{
				MemoryUtil.memFree(this.textureBuffer);
				this.textureBuffer = null;	
			}
			
			if (this.textureId >= 0)
			{
				TextureUtil.releaseTextureId(this.textureId);
				this.textureId = -1;
			}
		}
	}
}
