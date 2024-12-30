package dev.nonamecrackers2.simpleclouds.client.mesh.multiregion;

import java.util.Arrays;
import java.util.Objects;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL41;

import com.google.common.base.Joiner;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.mesh.lod.LevelOfDetail;
import dev.nonamecrackers2.simpleclouds.client.mesh.lod.LevelOfDetailConfig;
import dev.nonamecrackers2.simpleclouds.client.mesh.lod.PreparedChunk;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudInfo;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.RegionType;
import dev.nonamecrackers2.simpleclouds.common.noise.AbstractNoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.NoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.registry.SimpleCloudsRegistries;
import net.minecraft.CrashReportCategory;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;

public class MultiRegionCloudMeshGenerator extends CloudMeshGenerator
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/MultiRegionCloudMeshGenerator");
	public static final int MAX_CLOUD_TYPES = 32;
	private int requiredRegionTexSize;
	private CloudInfo[] cloudTypes;
	private RegionType regionGenerator;
	private @Nullable CloudRegionTextureGenerator regionTextureGenerator;
	private boolean cloudTypesModified;
	private boolean regionGeneratorChanged;
	private @Nullable float[] currentRegionAlignX;
	private @Nullable float[] currentRegionAlignZ;
	
	public MultiRegionCloudMeshGenerator(boolean fadeNearOrigin, boolean shadedClouds, LevelOfDetailConfig lodConfig, RegionType regionGenerator, int meshGenInterval, boolean useTransparency, CloudInfo[] cloudTypes)
	{
		super(CloudMeshGenerator.MAIN_CUBE_MESH_GENERATOR, 0, fadeNearOrigin, shadedClouds, lodConfig, meshGenInterval, useTransparency);
		this.setCloudTypes(cloudTypes);
		this.regionGenerator = regionGenerator;
	}
	
	@Override
	protected Pair<Integer, Integer> determineMinimumAndMaximimumGenHeightsAt(float minX, float minZ, float maxX, float maxZ)
	{
		float[][] positions = new float[][] { {minX, minZ}, {minX, maxZ}, {maxX, minZ}, {maxX, maxZ} };
		int smallestStartHeight = 0;
		int largestEndHeight = 0;
		for (int i = 0; i < positions.length; i++)
		{
			float[] pos = positions[i];
			float x = pos[0] + this.scrollX;
			float z = pos[1] + this.scrollZ;
			RegionType.Result result = this.regionGenerator.getCloudTypeIndexAt(x, z, SimpleCloudsConstants.REGION_SCALE, this.cloudTypes.length);
			CloudInfo type = this.cloudTypes[result.index()];
			NoiseSettings config = type.noiseConfig();
			int startHeight = config.getStartHeight();
			int endHeight = config.getEndHeight();
			if (i == 0 || smallestStartHeight > startHeight)
				smallestStartHeight = startHeight;
			if (i == 0 || largestEndHeight < endHeight)
				largestEndHeight = endHeight;
		}
		return Pair.of(smallestStartHeight, largestEndHeight);
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
			this.cloudTypesModified = true;
		}
	}
	
	public RegionType getRegionGenerator()
	{
		return this.regionGenerator;
	}
	
	public void setRegionGenerator(RegionType generator)
	{
		if (this.regionGenerator != generator)
		{
			this.regionGenerator = generator;
			this.regionGeneratorChanged = true;
		}
	}
	
	@Override
	public void close()
	{
		super.close();
		
		this.closeRegionGenerator();
	}
	
	@Override
	protected void setupShader()
	{
		super.setupShader();
		
		this.shader.bindShaderStorageBuffer(NOISE_LAYERS_NAME, GL15.GL_STATIC_DRAW).allocateBuffer(AbstractNoiseSettings.Param.values().length * 4 * MAX_NOISE_LAYERS * MAX_CLOUD_TYPES);
		this.shader.bindShaderStorageBuffer(LAYER_GROUPINGS_NAME, GL15.GL_STATIC_DRAW).allocateBuffer(CloudInfo.BYTES_PER_TYPE * MAX_CLOUD_TYPES);
		
		this.uploadNoiseData();
		this.cloudTypesModified = false;
		this.regionGeneratorChanged = false;
		this.setupOrReinitializeRegionGenerator();
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
		for (LevelOfDetail config : this.lodConfig.getLods())
			requiredRegionTexSize += config.spread() * 2;
		this.requiredRegionTexSize = requiredRegionTexSize * SimpleCloudsConstants.CHUNK_SIZE;
		
		this.closeRegionGenerator();
		
		this.regionTextureGenerator = new CloudRegionTextureGenerator(this.lodConfig, this.cloudTypes, this.requiredRegionTexSize, SimpleCloudsConstants.REGION_SCALE, this.regionGenerator);
		if (this.shader != null)
			this.updateCloudRegionTextureInfoOnMeshShader();
		
		int layers = this.lodConfig.getLods().length;
		this.currentRegionAlignX = new float[layers + 1];
		this.currentRegionAlignZ = new float[layers + 1];
		
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
	protected void generateChunk(CloudMeshGenerator.ChunkGenTask task)
	{
		this.shader.forUniform("RegionSampleOffset", (id, loc) -> 
		{
			PreparedChunk chunk = task.chunk().getChunkInfo();
			int lodLevel = chunk.lodLevel();
			float alignX = this.currentRegionAlignX[lodLevel];
			float alignZ = this.currentRegionAlignZ[lodLevel];
			GL41.glProgramUniform2f(id, loc, chunk.x() * (float)SimpleCloudsConstants.CHUNK_SIZE + (float)this.requiredRegionTexSize / 2.0F + alignX, chunk.z() * (float)SimpleCloudsConstants.CHUNK_SIZE + (float)this.requiredRegionTexSize / 2.0F + alignZ);
		});
		
		super.generateChunk(task);
	}
	
	private void uploadNoiseData()
	{
		RenderSystem.assertOnRenderThreadOrInit();
		
		if (this.shader != null && this.shader.isValid())
		{
			LOGGER.debug("Uploading noise data to main mesh compute shader...");
			this.shader.getShaderStorageBuffer(LAYER_GROUPINGS_NAME).writeData(b -> 
			{
				int previousLayerIndex = 0;
				for (int i = 0; i < this.cloudTypes.length; i++)
				{
					CloudInfo type = this.cloudTypes[i];
					previousLayerIndex = type.packToBuffer(b, previousLayerIndex);
				}
				b.rewind();
			}, CloudInfo.BYTES_PER_TYPE * this.cloudTypes.length);
			
			this.shader.getShaderStorageBuffer(NOISE_LAYERS_NAME).writeData(b -> 
			{
				for (int i = 0; i < this.cloudTypes.length; i++)
				{
					NoiseSettings settings = this.cloudTypes[i].noiseConfig();
					float[] packed = settings.packForShader();
					for (int j = 0; j < packed.length && j < AbstractNoiseSettings.Param.values().length * MAX_NOISE_LAYERS; j++)
						b.putFloat(packed[j]);
				}
				b.rewind();
			}, AbstractNoiseSettings.Param.values().length * 4 * MAX_NOISE_LAYERS * this.cloudTypes.length);
		}
	}
	
	@Override
	protected int prepareMeshGen(double originX, double originY, double originZ, float meshGenOffsetX, float meshGenOffsetZ, @Nullable Frustum frustum, int interval)
	{
		if (this.cloudTypesModified)
		{
			this.uploadNoiseData();
			this.setupOrReinitializeRegionGenerator();
			this.cloudTypesModified = false;
		}
		
		if (this.regionGeneratorChanged)
		{
			this.setupOrReinitializeRegionGenerator();
			this.regionGeneratorChanged = false;
		}

		if (this.regionTextureGenerator != null)
		{
			this.regionTextureGenerator.update(this.scrollX, this.scrollZ, meshGenOffsetX, meshGenOffsetZ);
			this.regionTextureGenerator.tick();
			if (this.shader != null)
				this.shader.setSampler2DArray("RegionsSampler", this.regionTextureGenerator.getAvailableRegionTextureId(), 0);
		}
		
		for (int i = 0; i < this.currentRegionAlignX.length; i++)
		{
			this.currentRegionAlignX[i] = this.regionTextureGenerator.getTexCoordOffsetX(i);
			this.currentRegionAlignZ[i] = this.regionTextureGenerator.getTexCoordOffsetZ(i);
		}
		
		return super.prepareMeshGen(originX, originY, originZ, meshGenOffsetX, meshGenOffsetZ, frustum, interval);
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
	
	@Override
	public void fillReport(CrashReportCategory category)
	{
		category.setDetail("Cloud Types", "(" + this.cloudTypes.length + ") " + Joiner.on(", ").join(this.cloudTypes));
		category.setDetail("Region Generator", () -> {
			ResourceLocation key = SimpleCloudsRegistries.getRegionTypeRegistry().getKey(this.regionGenerator);
			if (key == null)
				return "UNKNOWN";
			else
				return key.toString();
		});
		super.fillReport(category);
	}
}
