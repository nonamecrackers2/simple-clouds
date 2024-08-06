package dev.nonamecrackers2.simpleclouds.client.mesh.multiregion;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL41;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.mesh.CloudStyle;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.ComputeShader;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudConstants;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudInfo;
import dev.nonamecrackers2.simpleclouds.common.noise.AbstractNoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.NoiseSettings;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;

public class MultiRegionCloudMeshGenerator extends CloudMeshGenerator
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/MultiRegionCloudMeshGenerator");
	public static final int MAX_CLOUD_TYPES = 32;
	private final CloudStyle style;
	private int requiredRegionTexSize;
	private CloudInfo[] cloudTypes;
	private @Nullable CloudRegionTextureGenerator regionTextureGenerator;
	private boolean needsNoiseRefreshing;
	private boolean fadeNearOrigin;
	private float fadeStart;
	private float fadeEnd;
	private @Nullable float[] currentRegionAlignX;
	private @Nullable float[] currentRegionAlignZ;
	
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
	
	public CloudInfo[] getCloudTypes()
	{
		return this.cloudTypes;
	}
	
	public int getTotalCloudTypes()
	{
		return this.cloudTypes.length;
	}
	
	public void setCloudTypes(CloudInfo[] cloudTypes)
	{
		Objects.requireNonNull(cloudTypes, "Cloud types cannot be null");
		if (cloudTypes.length > MAX_CLOUD_TYPES)
			throw new IllegalArgumentException("Too many cloud types! The maximum allowed is " + MAX_CLOUD_TYPES);
		if (!Arrays.equals(this.cloudTypes, cloudTypes))
		{
			this.cloudTypes = cloudTypes;
			this.needsNoiseRefreshing = true;
		}
	}
	
	@Override
	public void close()
	{
		super.close();
		
		this.closeRegionGenerator();
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
	
	private void closeRegionGenerator()
	{
		if (this.regionTextureGenerator != null)
		{
			this.regionTextureGenerator.close();
			this.regionTextureGenerator = null;
		}
	}
	
	private void setupOrReinitializeRegionGenerator()
	{
		RenderSystem.assertOnRenderThreadOrInit();
	
		int requiredRegionTexSize = this.lodConfig.getPrimaryChunkSpan();
		for (CloudMeshGenerator.LevelOfDetail config : this.lodConfig.getLods())
			requiredRegionTexSize += config.spread() * 2;
		this.requiredRegionTexSize = requiredRegionTexSize * CloudConstants.CHUNK_SIZE;
		
		this.closeRegionGenerator();
		
		this.regionTextureGenerator = new CloudRegionTextureGenerator(this.lodConfig, this.cloudTypes, this.requiredRegionTexSize, 2000.0F);
		if (this.shader != null)
			this.updateCloudRegionTextureInfoOnMeshShader();
		
		LOGGER.debug("Created cloud region texture generator with size {}x{}x{}", this.requiredRegionTexSize, this.requiredRegionTexSize, this.lodConfig.getLods().length + 1);
	}
	
	private void updateCloudRegionTextureInfoOnMeshShader()
	{
		this.shader.setSampler2DArray("RegionsSampler", this.regionTextureGenerator.getAvailableRegionTextureId(), 0);
		this.shader.forUniform("RegionsTexSize", (id, loc) -> {
			GL41.glProgramUniform1i(id, loc, this.requiredRegionTexSize);
		});
	}
	
	@Override
	protected void generateChunk(int lodLevel, int lodScale, int x, int y, int z, float offsetX, float offsetY, float offsetZ, float scale, float camOffsetX, float camOffsetZ, int noOcclusionDirectionIndex)
	{
		this.shader.forUniform("RegionSampleOffset", (id, loc) -> 
		{
			float alignX = this.currentRegionAlignX[lodLevel];
			float alignZ = this.currentRegionAlignZ[lodLevel];
			GL41.glProgramUniform2f(id, loc, x * (float)CloudConstants.CHUNK_SIZE + (float)this.requiredRegionTexSize / 2.0F + alignX, z * (float)CloudConstants.CHUNK_SIZE + (float)this.requiredRegionTexSize / 2.0F + alignZ);
		});
		super.generateChunk(lodLevel, lodScale, x, y, z, offsetX, offsetY, offsetZ, scale, camOffsetX, camOffsetZ, noOcclusionDirectionIndex);
	}
	
	private void uploadNoiseData()
	{
		RenderSystem.assertOnRenderThreadOrInit();
		
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
	protected void onLodConfigChanged()
	{
		super.onLodConfigChanged();
		
		this.setupOrReinitializeRegionGenerator();
		
		int layers = this.lodConfig.getLods().length;
		this.currentRegionAlignX = new float[layers + 1];
		this.currentRegionAlignZ = new float[layers + 1];
	}
	
	@Override
	protected void populateChunkGenTasks(double camX, double camY, double camZ, float scale, Frustum frustum)
	{
		if (this.needsNoiseRefreshing)
		{
			this.uploadNoiseData();
			this.needsNoiseRefreshing = false;
		}

		if (this.regionTextureGenerator != null)
		{
			if (!this.regionTextureGenerator.isStarted())
				this.regionTextureGenerator.start();
			float chunkSizeUpscaled = (float)CloudConstants.CHUNK_SIZE * scale;
			float camOffsetX = ((float)Mth.floor(camX / chunkSizeUpscaled) * (float)CloudConstants.CHUNK_SIZE);
			float camOffsetZ = ((float)Mth.floor(camZ / chunkSizeUpscaled) * (float)CloudConstants.CHUNK_SIZE);
			this.regionTextureGenerator.update(this.scrollX, this.scrollZ, camOffsetX, camOffsetZ);
			this.regionTextureGenerator.tick();
			if (this.shader != null)
				this.shader.setSampler2DArray("RegionsSampler", this.regionTextureGenerator.getAvailableRegionTextureId(), 0);
		}
		
		for (int i = 0; i < this.currentRegionAlignX.length; i++)
		{
			this.currentRegionAlignX[i] = this.regionTextureGenerator.getTexCoordOffsetX(i);
			this.currentRegionAlignZ[i] = this.regionTextureGenerator.getTexCoordOffsetZ(i);
		}
		
		super.populateChunkGenTasks(camX, camY, camZ, scale, frustum);
	}
	
	public int getCloudRegionTextureId()
	{
		if (this.regionTextureGenerator == null)
			return -1;
		return this.regionTextureGenerator.getAvailableRegionTextureId();
	}
	
	public @Nullable CloudRegionTextureGenerator getCloudRegionTextureGenerator()
	{
		return this.regionTextureGenerator;
	}
}
