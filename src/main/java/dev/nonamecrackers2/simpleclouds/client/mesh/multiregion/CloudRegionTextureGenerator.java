package dev.nonamecrackers2.simpleclouds.client.mesh.multiregion;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudInfo;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.RegionType;

public class CloudRegionTextureGenerator
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/CloudRegionTextureGenerator");
	private static boolean doLogging = false;
	private static final int BYTES_PER_PIXEL = 8;
	private final CloudMeshGenerator.LevelOfDetailConfig lodConfig;
	private final CloudInfo[] cloudTypes;
	private final CloudRegionTextureGenerator.BufferState[] swapBuffers = new CloudRegionTextureGenerator.BufferState[2];
	private final int textureSize;
	private final float cloudRegionScale;
	private final RegionType regionGenerator;
	private @Nullable Thread thread;
	private @Nullable Throwable threadException;
	private int finishedBufferIndex; 
	private int currentlyUploadingIndex;
	private int generatingBufferIndex;
	private float scrollX;
	private float scrollZ;
	private float offsetX;
	private float offsetZ;
	private boolean isClosing;
	
	public CloudRegionTextureGenerator(CloudMeshGenerator.LevelOfDetailConfig lodConfig, CloudInfo[] cloudTypes, int textureSize, float cloudRegionScale, RegionType regionGenerator)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		
		if (cloudTypes.length > MultiRegionCloudMeshGenerator.MAX_CLOUD_TYPES)
			throw new IllegalArgumentException("Too many cloud types! The maximum allowed is " + MultiRegionCloudMeshGenerator.MAX_CLOUD_TYPES);
		this.lodConfig = lodConfig;
		this.cloudTypes = cloudTypes;
		this.textureSize = textureSize;
		if (cloudRegionScale == 0.0F)
			throw new IllegalArgumentException("Cloud region scale cannot be zero!");
		this.cloudRegionScale = cloudRegionScale;
		this.regionGenerator = regionGenerator;
		
		for (int i = 0; i < this.swapBuffers.length; i++)
			this.swapBuffers[i] = new CloudRegionTextureGenerator.BufferState(this.textureSize, this.lodConfig.getLods().length + 1);
		
		this.thread = new Thread(() ->
		{
			while (!this.isClosing)
				this.asyncTick();
		});
		this.thread.setName("Cloud Region Texture Generator Thread");
		this.thread.setUncaughtExceptionHandler((t, e) -> this.threadException = e);
	}
	
	public CloudMeshGenerator.LevelOfDetailConfig getLodConfig()
	{
		return this.lodConfig;
	}
	
	public CloudInfo[] getCloudTypes()
	{
		return this.cloudTypes;
	}
	
	public int getTextureSize()
	{
		return this.textureSize;
	}
	
	public float getRegionScale()
	{
		return this.cloudRegionScale;
	}
	
	public boolean isStarted()
	{
		return this.thread != null && this.thread.isAlive();
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
	
	public float getTexCoordOffsetX(int lod)
	{
		var buffer = this.swapBuffers[this.finishedBufferIndex];
		float scale = 1.0F;
		if (lod > 0)
			scale = (float)this.lodConfig.getLods()[lod - 1].chunkScale();
		float bufferX = (buffer.generatedScrollX + buffer.generatedOffsetX) / scale;
		float currentX = (this.scrollX + this.offsetX) / scale;
		return currentX - bufferX;
	}
	
	public float getTexCoordOffsetZ(int lod)
	{
		var buffer = this.swapBuffers[this.finishedBufferIndex];
		float scale = 1.0F;
		if (lod > 0)
			scale = (float)this.lodConfig.getLods()[lod - 1].chunkScale();
		float bufferZ = (buffer.generatedScrollZ + buffer.generatedOffsetZ) / scale;
		float currentZ = (this.scrollZ + this.offsetZ) / scale;
		return currentZ - bufferZ;
	}
	
	public int getAvailableRegionTextureId()
	{
		return this.swapBuffers[this.finishedBufferIndex].getTextureId();
	}
	
	public void tick()
	{
		RenderSystem.assertOnRenderThread();
		
		if (this.isClosing)
			throw new IllegalStateException("This cloud region texture generator is no longer valid");
		
		if (this.threadException != null)
			throw new RuntimeException("An uncaught exception occured while generating a cloud region texture buffer", this.threadException);

		var buffer = this.swapBuffers[this.currentlyUploadingIndex];
		
		if (buffer.needsUploading())
		{
			if (doLogging)
			{
				LOGGER.debug("==========================");
				LOGGER.debug("Uploading buffer {}", this.currentlyUploadingIndex);
			}
			buffer.beginBufferUpload();
		}
		
		if (buffer.isFinishedBufferUploading())
		{
			if (doLogging)
				LOGGER.debug("Copying buffer data to texture...");
			buffer.beginTextureCopy();
		}
		
		if (buffer.isFinishedTextureCopying())
		{
			buffer.finalizeUpload();
			if (doLogging)
				LOGGER.debug("Finished uploading");
			this.finishedBufferIndex = this.currentlyUploadingIndex;
			this.currentlyUploadingIndex++;
			if (this.currentlyUploadingIndex >= this.swapBuffers.length)
				this.currentlyUploadingIndex = 0;
		}
		
		if (doLogging && buffer.isUploading())
			LOGGER.debug("--Frame--");
	}
	
	private void asyncTick()
	{
		var buffer = this.swapBuffers[this.generatingBufferIndex];
		if (buffer != null && !buffer.isUploading() && !buffer.needsUploading())
		{
			buffer.update(this.scrollX, this.scrollZ, this.offsetX, this.offsetZ);
			if (doLogging)
				LOGGER.debug("Generating texture buffer for {}", this.generatingBufferIndex);
			buffer.isGenerating = true;
			this.generateTexture(buffer);
			buffer.isGenerating = false;
			buffer.needsUploading = true;
			if (doLogging)
				LOGGER.debug("Finished generating for {}", this.generatingBufferIndex);
			this.generatingBufferIndex++;
			if (this.generatingBufferIndex >= this.swapBuffers.length)
				this.generatingBufferIndex = 0;
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
					float centerOffset = (float)buffer.textureSize / 2.0F;
					float posX = ((float)x - centerOffset) * scale + buffer.scrollX + buffer.offsetX;
					float posZ = ((float)y - centerOffset) * scale + buffer.scrollZ + buffer.offsetZ;
//					Vector2f pos = new Vector2f((float)x, (float)y).sub((float)buffer.textureSize / 2.0F, (float)buffer.textureSize / 2.0F).mul(scale).add((float)buffer.textureSize / 2.0F, (float)buffer.textureSize / 2.0F).add(buffer.scrollX, buffer.scrollZ).add(buffer.offsetX, buffer.offsetZ);
					RegionType.Result result = this.regionGenerator.getCloudTypeIndexAt(posX, posZ, this.cloudRegionScale, this.cloudTypes.length);
					buffer.textureBuffer.putFloat(index, (float)result.index());
					buffer.textureBuffer.putFloat(index + 4, result.fade());
				}
			}
		}
	}
	
	public void close()
	{
		RenderSystem.assertOnRenderThreadOrInit();
		
		this.isClosing = true;
		
		try{
			this.thread.join(5000L);
		} catch (InterruptedException e) {
			LOGGER.error("Failed to close texture generator thread: ", e);
		} finally {
			this.thread = null;
		}
		
		for (int i = 0; i < this.swapBuffers.length; i++)
		{
			if (this.swapBuffers[i] != null)
				this.swapBuffers[i].close();
			this.swapBuffers[i] = null;
		}
	}
	
	public static class BufferState
	{
		private final int textureSize;
		private final int layers;
		private final int bufferSize;
		private @Nullable ByteBuffer textureBuffer;
		private long bufferUploadFenceId = -1;
		private long textureCopyFenceId = -1;
		private int uploadBufferId;
		private int textureId = -1;
		private boolean isGenerating;
		private boolean needsUploading;
		private boolean isUploading;
		private float scrollX;
		private float scrollZ;
		private float offsetX;
		private float offsetZ;
		private float generatedScrollX;
		private float generatedScrollZ;
		private float generatedOffsetX;
		private float generatedOffsetZ;
		
		private BufferState(int textureSize, int layers)
		{
			this.textureSize = textureSize;
			this.layers = layers;
			
			this.bufferSize = this.textureSize * this.textureSize * this.layers * BYTES_PER_PIXEL;
			this.textureBuffer = MemoryTracker.create(this.bufferSize);
			
			this.uploadBufferId = GlStateManager._glGenBuffers();
			GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, this.uploadBufferId);
			GL15.glBufferData(GL21.GL_PIXEL_UNPACK_BUFFER, this.textureBuffer, GL15.GL_STREAM_DRAW);
//			GL44.glBufferStorage(GL21.GL_PIXEL_UNPACK_BUFFER, this.textureBuffer, GL30.GL_MAP_WRITE_BIT);
			GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
			
			this.textureId = TextureUtil.generateTextureId();
			GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.textureId);
			GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
			GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
			GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
			GL12.glTexImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, GL30.GL_RG32F, this.textureSize, this.textureSize, this.layers, 0, GL30.GL_RG, GL11.GL_FLOAT, (IntBuffer)null);
			GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
		}
		
		private void update(float scrollX, float scrollZ, float offsetX, float offsetZ)
		{
			if (this.isGenerating())
				throw new IllegalStateException("Cannot update parameters directly while the texture buffer is being updated");
			this.scrollX = scrollX;
			this.scrollZ = scrollZ;
			this.offsetX = offsetX;
			this.offsetZ = offsetZ;
		}
		
		private void beginBufferUpload()
		{
			if (this.textureId == -1 || this.uploadBufferId == -1 || this.textureBuffer == null)
				throw new IllegalStateException("This buffer is no longer valid!");
			
			this.needsUploading = false;
			this.isUploading = true;
			
			GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, this.uploadBufferId);
			ByteBuffer buffer = GL30.glMapBufferRange(GL21.GL_PIXEL_UNPACK_BUFFER, 0, this.bufferSize, GL30.GL_MAP_WRITE_BIT);
			MemoryUtil.memCopy(this.textureBuffer, buffer);
			GL30.glUnmapBuffer(GL21.GL_PIXEL_UNPACK_BUFFER);
			this.bufferUploadFenceId = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
			GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
		}
		
		private void beginTextureCopy()
		{
			if (this.textureId == -1 || this.uploadBufferId == -1 || this.textureBuffer == null)
				throw new IllegalStateException("This buffer is no longer valid!");
			
			GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, this.uploadBufferId);
			GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, this.textureId);
			GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 8);
//			GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, this.textureSize);
			GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, this.textureSize);
			GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
			GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
			GL12.glTexSubImage3D(GL30.GL_TEXTURE_2D_ARRAY, 0, 0, 0, 0, this.textureSize, this.textureSize, this.layers, GL30.GL_RG, GL11.GL_FLOAT, 0L);
			this.textureCopyFenceId = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
			if (GlStateManager._getError() == GL11.GL_INVALID_OPERATION)
				LOGGER.error("Something went wrong when trying to copy texture data over");
			GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
			GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
			
			if (this.bufferUploadFenceId != -1)
				GL32.glDeleteSync(this.bufferUploadFenceId);
			this.bufferUploadFenceId = -1;
		}
		
		private void finalizeUpload()
		{
			if (this.textureId == -1 || this.uploadBufferId == -1 || this.textureBuffer == null)
				throw new IllegalStateException("This buffer is no longer valid!");
			
			if (this.textureCopyFenceId != -1)
				GL32.glDeleteSync(this.textureCopyFenceId);
			this.textureCopyFenceId = -1;
			this.isUploading = false;
			
			this.generatedScrollX = this.scrollX;
			this.generatedScrollZ = this.scrollZ;
			this.generatedOffsetX = this.offsetX;
			this.generatedOffsetZ = this.offsetZ;
		}
		
		private boolean isFinishedBufferUploading()
		{
			return isFenceSignaled(this.bufferUploadFenceId);
		}
		
		private boolean isFinishedTextureCopying()
		{
			return isFenceSignaled(this.textureCopyFenceId);
		}
		
		private static boolean isFenceSignaled(long id)
		{
			if (id != -1)
				return GL32.glGetSynci(id, GL32.GL_SYNC_STATUS, null) == GL32.GL_SIGNALED;
			else
				return false;
		}
		
		public boolean needsUploading()
		{
			return this.needsUploading;
		}
		
		public boolean isGenerating()
		{
			return this.isGenerating;
		}
		
		public boolean isUploading()
		{
			return this.isUploading;
		}
		
		public int getTextureId()
		{
			return this.textureId;
		}
		
		public void close()
		{
			if (this.uploadBufferId != -1)
			{
				GlStateManager._glDeleteBuffers(this.uploadBufferId);
				this.uploadBufferId = -1;
			}
			
			if (this.textureId != 0)
			{
				TextureUtil.releaseTextureId(this.textureId);
				this.textureId = -1;
			}
			
			if (this.textureBuffer != null)
			{
				MemoryUtil.memFree(this.textureBuffer);
				this.textureBuffer = null;	
			}
			
			if (this.bufferUploadFenceId != -1)
			{
				GL32.glDeleteSync(this.bufferUploadFenceId);
				this.bufferUploadFenceId = -1;
			}
			
			if (this.textureCopyFenceId != -1)
			{
				GL32.glDeleteSync(this.textureCopyFenceId);
				this.textureCopyFenceId = -1;
			}
		}
	}
}
