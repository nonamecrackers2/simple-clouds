package dev.nonamecrackers2.simpleclouds.client.mesh;

import java.io.IOException;
import java.nio.IntBuffer;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL42;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.ComputeShader;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudInfo;
import dev.nonamecrackers2.simpleclouds.common.noise.AbstractNoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.NoiseSettings;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;

public class MultiRegionCloudMeshGenerator extends CloudMeshGenerator
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/MultiRegionCloudMeshGenerator");
	public static final int MAX_CLOUD_TYPES = 32;
	private static final ResourceLocation CLOUD_REGIONS_GENERATOR = SimpleCloudsMod.id("cloud_regions");
	private final CloudStyle style;
	private int requiredRegionTexSize;
	private boolean needsRegionTextureUpdated;
	private CloudInfo[] cloudTypes;
	private @Nullable ComputeShader cloudRegionShader;
	private int cloudRegionTexture = -1;
	private int cloudRegionUnit = -1;
	private boolean needsNoiseRefreshing;
	private boolean fadeNearOrigin;
	private float fadeStart;
	private float fadeEnd;
	
	public MultiRegionCloudMeshGenerator(CloudInfo[] cloudTypes, CloudMeshGenerator.LevelOfDetailConfig lodConfig, int meshGenInterval, CloudStyle style)
	{
		super(CloudMeshGenerator.MAIN_CUBE_MESH_GENERATOR, lodConfig, meshGenInterval);
		this.setCloudTypes(cloudTypes);
		this.style = style;
	}

	public MultiRegionCloudMeshGenerator setFadeNearOrigin(float fadeStart, float fadeEnd)
	{
		if (fadeStart > fadeEnd)
		{
			this.fadeStart = fadeEnd * (float)this.getCloudAreaMaxRadius();
			this.fadeEnd = fadeStart * (float)this.getCloudAreaMaxRadius();
		}
		else
		{
			this.fadeStart = fadeStart * (float)this.getCloudAreaMaxRadius();
			this.fadeEnd = fadeEnd * (float)this.getCloudAreaMaxRadius();
		}
		this.fadeNearOrigin = true;
		return this;
	}
	
	@Override
	public void setLodConfig(CloudMeshGenerator.LevelOfDetailConfig lodConfig)
	{
		if (lodConfig != this.lodConfig)
		{
			super.setLodConfig(lodConfig);
			this.needsRegionTextureUpdated = true;
		}
	}
	
	public void setCloudTypes(CloudInfo[] cloudTypes)
	{
		if (cloudTypes.length > MAX_CLOUD_TYPES)
			throw new IllegalArgumentException("Too many cloud types! The maximum allowed is " + MAX_CLOUD_TYPES);
		this.cloudTypes = cloudTypes;
		this.needsNoiseRefreshing = true;
	}
	
	@Override
	public void close()
	{
		super.close();
		
		if (this.cloudRegionShader != null)
			this.cloudRegionShader.close();
		this.cloudRegionShader = null;
		
		this.freeRegionTexture();
	}
	
	@Override
	protected ComputeShader createShader(ResourceManager manager) throws IOException
	{
		return ComputeShader.loadShader(this.meshShaderLoc, manager, LOCAL_SIZE, LOCAL_SIZE, LOCAL_SIZE, ImmutableMap.of("${TYPE}", "0", "${FADE_NEAR_ORIGIN}", this.fadeNearOrigin ? "1" : "0", "${STYLE}", String.valueOf(this.style.getIndex())));
	}
	
	@Override
	protected void setupShader()
	{
		super.setupShader();
		this.shader.bindShaderStorageBuffer("NoiseLayers", GL15.GL_STATIC_DRAW).allocateBuffer(AbstractNoiseSettings.Param.values().length * 4 * MAX_NOISE_LAYERS * MAX_CLOUD_TYPES);
		this.shader.bindShaderStorageBuffer("LayerGroupings", GL15.GL_STATIC_DRAW).allocateBuffer(20 * MAX_CLOUD_TYPES);
		if (this.cloudRegionUnit == -1)
			throw new IllegalStateException("Cloud region texture unit must be valid");
		this.shader.setImageUnit("regions", this.cloudRegionUnit);
		if (this.fadeNearOrigin)
		{
			this.shader.forUniform("FadeStart", (id, loc) -> {
				GL41.glProgramUniform1f(id, loc, this.fadeStart);
			});
			this.shader.forUniform("FadeEnd", (id, loc) -> {
				GL41.glProgramUniform1f(id, loc, this.fadeEnd);
			});
		}
		this.uploadNoiseData();
		this.needsNoiseRefreshing = false;
	}
	
	private void freeRegionTexture()
	{
		if (this.cloudRegionUnit >= 0)
		{
			LOGGER.debug("Freeing cloud region texture binding");
		 	ComputeShader.freeImageUnit(this.cloudRegionUnit);
			this.cloudRegionUnit = -1;
		}
		
		if (this.cloudRegionTexture >= 0)
		{
			LOGGER.debug("Freeing cloud region texture ID");
			TextureUtil.releaseTextureId(this.cloudRegionTexture);
			this.cloudRegionTexture = -1;
		}
	}
	
	private void createRegionTexture()
	{
		RenderSystem.assertOnRenderThreadOrInit();
	
		int requiredRegionTexSize = this.lodConfig.getPrimaryChunkSpan();
		for (CloudMeshGenerator.LevelOfDetail config : this.lodConfig.getLods())
			requiredRegionTexSize += config.spread() * 2;
		this.requiredRegionTexSize = requiredRegionTexSize * 32;
		
		this.freeRegionTexture();
		
		this.cloudRegionTexture = TextureUtil.generateTextureId();
		GL11.glBindTexture(GL12.GL_TEXTURE_3D, this.cloudRegionTexture);
		GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL42.glTexImage3D(GL12.GL_TEXTURE_3D, 0, GL30.GL_RG32F, this.requiredRegionTexSize, this.requiredRegionTexSize, this.lodConfig.getLods().length + 1, 0, GL30.GL_RG, GL11.GL_UNSIGNED_BYTE, (IntBuffer)null);
		this.cloudRegionUnit = ComputeShader.getAndUseImageUnit();
		GL42.glBindImageTexture(this.cloudRegionUnit, this.cloudRegionTexture, 0, true, 0, GL42.GL_READ_WRITE, GL30.GL_RG32F);
		GL11.glBindTexture(GL12.GL_TEXTURE_3D, 0);
		
		LOGGER.debug("Created cloud region texture {} with size {}x{}x{}", this.cloudRegionTexture, this.requiredRegionTexSize, this.requiredRegionTexSize, this.lodConfig.getLods().length + 1);
	}
	
	private void setupCloudRegionShader()
	{
		RenderSystem.assertOnRenderThreadOrInit();
		
		if (this.cloudRegionShader == null || !this.cloudRegionShader.isValid())
			return;
		
		var buffer = this.cloudRegionShader.getShaderStorageBuffer("LodScales");
		buffer.writeData(b -> 
		{
			b.putFloat(0, 1.0F);
			for (int i = 0; i < this.lodConfig.getLods().length; i++)
			{
				LevelOfDetail config = this.lodConfig.getLods()[i];
				b.putFloat((i + 1) * 4, (float)config.chunkScale());
			}
		}, (this.lodConfig.getLods().length + 1) * 4);
		
		this.cloudRegionShader.forUniform("Scale", (id, loc) -> {
			GL41.glProgramUniform1f(id, loc, 2000.0F);//Mth.clamp(Mth.sin(this.test * 0.01F), 0.1F, 1.0F));
		});
		
		if (this.cloudRegionUnit == -1)
			throw new IllegalStateException("Cloud region texture unit must be valid");
		
		this.cloudRegionShader.setImageUnit("mainImage", this.cloudRegionUnit);
		
		LOGGER.debug("Cloud region shader has been prepared");
	}
	
	@Override
	protected void initExtra(ResourceManager manager)
	{
		this.freeRegionTexture();
		
		if (this.cloudRegionShader != null)
		{
			LOGGER.debug("Freeing cloud region shader");
			this.cloudRegionShader.close();
			this.cloudRegionShader = null;
		}
		
		try
		{
			LOGGER.debug("Creating cloud region compute shader");
			this.cloudRegionShader = ComputeShader.loadShader(CLOUD_REGIONS_GENERATOR, manager, 8, 8, 1);
			this.cloudRegionShader.bindShaderStorageBuffer("LodScales", GL15.GL_STATIC_DRAW).allocateBuffer((this.lodConfig.getLods().length + 1) * 4);
			this.createRegionTexture();
			this.setupCloudRegionShader();
			this.needsRegionTextureUpdated = false;
			//this.cloudRegionShader.dispatchAndWait(this.requiredRegionTexSize / 8, this.requiredRegionTexSize / 8, this.lodConfig.getLods().length + 1);
		}
		catch (IOException e)
		{
			LOGGER.warn("Failed to load cloud region compute shader", e);
		}
	}
	
	@Override
	protected void generateChunk(int lodLevel, int lodScale, int x, int y, int z, float offsetX, float offsetY, float offsetZ, float scale, float camOffsetX, float camOffsetZ, int noOcclusionDirectionIndex)
	{
		this.shader.forUniform("RegionSampleOffset", (id, loc) -> {
			GL41.glProgramUniform2f(id, loc, x * 32.0F + (float)this.requiredRegionTexSize / 2.0F, z * 32.0F + (float)this.requiredRegionTexSize / 2.0F);
		});
		super.generateChunk(lodLevel, lodScale, x, y, z, offsetX, offsetY, offsetZ, scale, camOffsetX, camOffsetZ, noOcclusionDirectionIndex);
	}
	
	private void uploadNoiseData()
	{
		RenderSystem.assertOnRenderThreadOrInit();
		
		if (this.cloudRegionShader != null && this.cloudRegionShader.isValid())
		{
			LOGGER.debug("Uploading total cloud types to cloud region shader...");
			this.cloudRegionShader.forUniform("TotalCloudTypes", (id, loc) -> {
				GL41.glProgramUniform1i(id, loc, this.getTotalCloudTypes());
			});
		}
		
		if (this.shader != null && this.shader.isValid())
		{
			LOGGER.debug("Uploading noise data to main mesh compute shader...");
			this.shader.getShaderStorageBuffer("LayerGroupings").writeData(b -> 
			{
				int currentIndex = 0;
				int previousLayerIndex = 0;
				for (int i = 0; i < this.cloudTypes.length; i++)
				{
					CloudInfo type = this.cloudTypes[i];
					int layerCount = type.noiseConfig().layerCount();
					b.putInt(currentIndex, previousLayerIndex);
					currentIndex += 4;
					b.putInt(currentIndex, previousLayerIndex + layerCount);
					currentIndex += 4;
					b.putFloat(currentIndex, type.storminess());
					currentIndex += 4;
					b.putFloat(currentIndex, type.stormStart());
					currentIndex += 4;
					b.putFloat(currentIndex, type.stormFadeDistance());
					currentIndex += 4;
					previousLayerIndex += layerCount;
				}
			}, 20 * this.cloudTypes.length);
			
			this.shader.getShaderStorageBuffer("NoiseLayers").writeData(b -> 
			{
				int index = 0;
				for (int i = 0; i < this.cloudTypes.length; i++)
				{
					NoiseSettings settings = this.cloudTypes[i].noiseConfig();
					float[] packed = settings.packForShader();
					for (int j = 0; j < packed.length && j < AbstractNoiseSettings.Param.values().length * MAX_NOISE_LAYERS; j++)
					{
						b.putFloat(index, packed[j]);
						index += 4;
					}
				}
			}, AbstractNoiseSettings.Param.values().length * 4 * MAX_NOISE_LAYERS * this.cloudTypes.length);
		}
		
	}
	
	@Override
	protected void populateChunkGenTasks(double camX, double camY, double camZ, float scale, Frustum frustum)
	{
		if (this.needsRegionTextureUpdated)
		{
			this.createRegionTexture();
			this.setupCloudRegionShader();
			this.needsRegionTextureUpdated = false;
		}
		
		if (this.needsNoiseRefreshing)
		{
			this.uploadNoiseData();
			this.needsNoiseRefreshing = false;
		}
		
		if (this.cloudRegionShader != null && this.cloudRegionShader.isValid())
		{
			this.cloudRegionShader.forUniform("Scroll", (id, loc) -> {
				GL41.glProgramUniform2f(id, loc, this.scrollX, this.scrollZ);
			});
			this.cloudRegionShader.forUniform("Offset", (id, loc) -> 
			{
				float chunkSizeUpscaled = 32.0F * scale;
				float camOffsetX = ((float)Mth.floor(camX / chunkSizeUpscaled) * 32.0F);
				float camOffsetZ = ((float)Mth.floor(camZ / chunkSizeUpscaled) * 32.0F);
				GL41.glProgramUniform2f(id, loc, camOffsetX, camOffsetZ);
			});
			this.cloudRegionShader.dispatchAndWait(this.requiredRegionTexSize / 8, this.requiredRegionTexSize / 8, this.lodConfig.getLods().length + 1);
		}
		
		super.populateChunkGenTasks(camX, camY, camZ, scale, frustum);
	}
	
	public int getCloudRegionTextureId()
	{
		return this.cloudRegionTexture;
	}
	
	public int getTotalCloudTypes()
	{
		return this.cloudTypes.length;
	}
}
