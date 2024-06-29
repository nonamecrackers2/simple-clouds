package dev.nonamecrackers2.simpleclouds.client.mesh;

import java.io.IOException;
import java.nio.IntBuffer;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.TextureUtil;

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
	private static final int MAX_CLOUD_TYPES = 32;
	private static final ResourceLocation CLOUD_REGIONS_GENERATOR = SimpleCloudsMod.id("cloud_regions");
	public static final int CLOUD_REGION_TEXTURE_SIZE;
	private CloudInfo[] cloudTypes;
	private @Nullable ComputeShader cloudRegionShader;
	private int cloudRegionTexture;
	
	static
	{
		int requiredRegionTexSize = PRIMARY_CHUNK_SPAN;
		for (CloudMeshGenerator.LevelOfDetailConfig config : LEVEL_OF_DETAIL)
			requiredRegionTexSize += config.spread() * 2;
		CLOUD_REGION_TEXTURE_SIZE = requiredRegionTexSize * 32;
	}
	
	public MultiRegionCloudMeshGenerator(CloudInfo[] cloudTypes)
	{
		super(CloudMeshGenerator.MAIN_CUBE_MESH_GENERATOR);
		this.setCloudTypes(cloudTypes);
	}
	
	public void setCloudTypes(CloudInfo[] cloudTypes)
	{
		if (cloudTypes.length > MAX_CLOUD_TYPES)
			throw new IllegalArgumentException("Too many cloud types! The maximum allowed is " + MAX_CLOUD_TYPES);
		this.cloudTypes = cloudTypes;
	}
	
	@Override
	public void close()
	{
		super.close();
		
		if (this.cloudRegionShader != null)
			this.cloudRegionShader.close();
		this.cloudRegionShader = null;
		
		if (this.cloudRegionTexture >= 0)
		{
			TextureUtil.releaseTextureId(this.cloudRegionTexture);
			this.cloudRegionTexture = -1;
		}
	}
	
	@Override
	protected ComputeShader createShader(ResourceManager manager) throws IOException
	{
		return ComputeShader.loadShader(this.meshShaderLoc, manager, LOCAL_SIZE, LOCAL_SIZE, LOCAL_SIZE, ImmutableMap.of("${TYPE}", "0"));
	}
	
	@Override
	protected void setupShader()
	{
		super.setupShader();
		this.shader.bindShaderStorageBuffer("NoiseLayers", GL15.GL_STATIC_DRAW).allocateBuffer(AbstractNoiseSettings.Param.values().length * 4 * MAX_NOISE_LAYERS * this.cloudTypes.length);
		this.shader.bindShaderStorageBuffer("LayerGroupings", GL15.GL_STATIC_DRAW).allocateBuffer(20 * this.cloudTypes.length);
	}
	
	@Override
	public void init(ResourceManager manager)
	{
		if (this.cloudRegionTexture >= 0)
		{
			TextureUtil.releaseTextureId(this.cloudRegionTexture);
			this.cloudRegionTexture = -1;
		}
		
		this.cloudRegionTexture = TextureUtil.generateTextureId();
		GL11.glBindTexture(GL12.GL_TEXTURE_3D, this.cloudRegionTexture);
		GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL12.GL_TEXTURE_3D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL42.glTexImage3D(GL12.GL_TEXTURE_3D, 0, GL30.GL_RG8, CLOUD_REGION_TEXTURE_SIZE, CLOUD_REGION_TEXTURE_SIZE, LEVEL_OF_DETAIL.length + 1, 0, GL30.GL_RG, GL11.GL_UNSIGNED_BYTE, (IntBuffer)null);
		GL42.glBindImageTexture(0, this.cloudRegionTexture, 0, true, 0, GL42.GL_READ_WRITE, GL30.GL_RG8);
		GL11.glBindTexture(GL12.GL_TEXTURE_3D, 0);
		
		LOGGER.debug("Created cloud region texture {} with size {}x{}x{}", this.cloudRegionTexture, CLOUD_REGION_TEXTURE_SIZE, CLOUD_REGION_TEXTURE_SIZE, LEVEL_OF_DETAIL.length + 1);
		
		if (this.cloudRegionShader != null)
		{
			this.cloudRegionShader.close();
			this.cloudRegionShader = null;
		}
		
		try
		{
			this.cloudRegionShader = ComputeShader.loadShader(CLOUD_REGIONS_GENERATOR, manager, 8, 8, 1);
			var buffer = this.cloudRegionShader.bindShaderStorageBuffer("LodScales", GL15.GL_STATIC_DRAW);
			buffer.allocateBuffer((LEVEL_OF_DETAIL.length + 1) * 4);
			buffer.writeData(b -> 
			{
				b.putFloat(0, 1.0F);
				for (int i = 0; i < LEVEL_OF_DETAIL.length; i++)
				{
					LevelOfDetailConfig config = LEVEL_OF_DETAIL[i];
					b.putFloat((i + 1) * 4, (float)config.chunkScale());
				}
			});
			this.cloudRegionShader.forUniform("Scale", loc -> {
				GL20.glUniform1f(loc, 2000.0F);//Mth.clamp(Mth.sin(this.test * 0.01F), 0.1F, 1.0F));
			});
			this.cloudRegionShader.forUniform("TotalCloudTypes", loc -> {
				GL20.glUniform1i(loc, this.cloudTypes.length);
			});
			this.cloudRegionShader.dispatchAndWait(CLOUD_REGION_TEXTURE_SIZE / 8, CLOUD_REGION_TEXTURE_SIZE / 8, LEVEL_OF_DETAIL.length + 1);
			LOGGER.debug("Created cloud region texture generator compute shader");
		}
		catch (IOException e)
		{
			LOGGER.warn("Failed to load cloud region compute shader", e);
		}
		
		super.init(manager);
	}
	
	@Override
	protected void generateChunk(int lodLevel, int lodScale, int x, int y, int z, float offsetX, float offsetY, float offsetZ, float scale, float camOffsetX, float camOffsetZ, int noOcclusionDirectionIndex)
	{
		this.shader.forUniform("RegionSampleOffset", loc -> {
			GL20.glUniform2f(loc, x * 32.0F + (float)CLOUD_REGION_TEXTURE_SIZE / 2.0F, z * 32.0F + (float)CLOUD_REGION_TEXTURE_SIZE / 2.0F);
		});
		super.generateChunk(lodLevel, lodScale, x, y, z, offsetX, offsetY, offsetZ, scale, camOffsetX, camOffsetZ, noOcclusionDirectionIndex);
	}
	
	@Override
	public void generateMesh(double camX, double camY, double camZ, float scale, @Nullable Frustum frustum)
	{
		boolean flag = true;
		if (this.cloudRegionShader != null && this.cloudRegionShader.isValid() && flag)
		{
			this.cloudRegionShader.forUniform("Scroll", loc -> {
				GL20.glUniform2f(loc, this.scrollX, this.scrollZ);
			});
			this.cloudRegionShader.forUniform("Offset", loc -> 
			{
				float chunkSizeUpscaled = 32.0F * scale;
				float camOffsetX = ((float)Mth.floor(camX / chunkSizeUpscaled) * 32.0F);
				float camOffsetZ = ((float)Mth.floor(camZ / chunkSizeUpscaled) * 32.0F);
				GL20.glUniform2f(loc, camOffsetX, camOffsetZ);
			});
			this.cloudRegionShader.dispatchAndWait(CLOUD_REGION_TEXTURE_SIZE / 8, CLOUD_REGION_TEXTURE_SIZE / 8, LEVEL_OF_DETAIL.length + 1);
		}
		
		if (this.shader != null && this.shader.isValid())
		{
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
			});
			
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
			});
		}
		
		super.generateMesh(camX, camY, camZ, scale, frustum);
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
