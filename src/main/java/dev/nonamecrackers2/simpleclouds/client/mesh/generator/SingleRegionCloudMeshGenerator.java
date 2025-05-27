package dev.nonamecrackers2.simpleclouds.client.mesh.generator;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL15;

import dev.nonamecrackers2.simpleclouds.client.mesh.lod.LevelOfDetailConfig;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudInfo;
import dev.nonamecrackers2.simpleclouds.common.noise.AbstractNoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.NoiseSettings;
import net.minecraft.CrashReportCategory;
import net.minecraft.client.renderer.culling.Frustum;

public final class SingleRegionCloudMeshGenerator extends CloudMeshGenerator
{
	private CloudInfo type;
	private boolean needsNoiseRefreshing;
	
	protected SingleRegionCloudMeshGenerator(boolean shadedClouds, LevelOfDetailConfig lodConfig, int meshGenInterval, boolean useTransparency, boolean fixedMeshDataSectionSize, CloudInfo type)
	{
		super(CloudMeshGenerator.MAIN_CUBE_MESH_GENERATOR, 1, false, shadedClouds, lodConfig, meshGenInterval, useTransparency, fixedMeshDataSectionSize);
		this.setCloudType(type);
		this.setFadeDistances(0.5F, 1.0F);
	}
	
	@Override
	protected CloudMeshGenerator.ChunkGenSettings determineChunkGenSettings(float minX, float minZ, float maxX, float maxZ)
	{
		NoiseSettings config = this.type.noiseConfig();
		int startHeight = config.getStartHeight();
		int endHeight = config.getEndHeight();
		if (startHeight == endHeight)
			return skip();
		return heights(startHeight, endHeight);
	}
	
	public CloudInfo getCloudType()
	{
		return this.type;
	}
	
	public void setCloudType(CloudInfo type)
	{
		this.type = type;
		this.needsNoiseRefreshing = true;
	}
	
	@Override
	protected void setupShader()
	{
		super.setupShader();
		
		this.shader.bindShaderStorageBuffer(LAYER_GROUPINGS_NAME, GL15.GL_STATIC_DRAW).allocateBuffer(CloudInfo.BYTES_PER_TYPE);
		this.shader.bindShaderStorageBuffer(NOISE_LAYERS_NAME, GL15.GL_STATIC_DRAW).allocateBuffer(AbstractNoiseSettings.Param.values().length * 4 * MAX_NOISE_LAYERS);
		
		this.uploadNoiseData();
		this.needsNoiseRefreshing = false;
	}
	
	private void uploadNoiseData()
	{
		if (this.shader == null || !this.shader.isValid())
			return;
		
		this.shader.getShaderStorageBuffer(NOISE_LAYERS_NAME).writeData(b -> 
		{
			float[] packed = this.type.noiseConfig().packForShader();
			for (int i = 0; i < packed.length && i < AbstractNoiseSettings.Param.values().length * MAX_NOISE_LAYERS; i++)
				b.putFloat(i * 4, packed[i]);
		}, AbstractNoiseSettings.Param.values().length * 4 * MAX_NOISE_LAYERS, false);
		
		this.shader.getShaderStorageBuffer(LAYER_GROUPINGS_NAME).writeData(b ->
		{
			this.type.packToBuffer(b, 0);
			b.rewind();
		}, CloudInfo.BYTES_PER_TYPE, false);
	}
	
	@Override
	protected int prepareMeshGen(double originX, double originY, double originZ, float meshGenOffsetX, float meshGenOffsetZ, @Nullable Frustum frustum, int interval, float partialTick)
	{
		if (this.needsNoiseRefreshing)
		{
			this.uploadNoiseData();
			this.needsNoiseRefreshing = false;
		}
		
		return super.prepareMeshGen(originX, originY, originZ, meshGenOffsetX, meshGenOffsetZ, frustum, interval, partialTick);
	}
	
	@Override
	public void fillReport(CrashReportCategory category)
	{
		category.setDetail("Cloud Type", this.type);
		super.fillReport(category);
	}
}
