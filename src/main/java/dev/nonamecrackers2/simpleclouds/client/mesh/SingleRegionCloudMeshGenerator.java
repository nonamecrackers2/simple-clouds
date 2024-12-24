package dev.nonamecrackers2.simpleclouds.client.mesh;

import java.io.IOException;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL41;

import com.google.common.collect.ImmutableMap;

import dev.nonamecrackers2.simpleclouds.client.mesh.lod.LevelOfDetailConfig;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.ComputeShader;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudInfo;
import dev.nonamecrackers2.simpleclouds.common.noise.AbstractNoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.NoiseSettings;
import net.minecraft.CrashReportCategory;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.server.packs.resources.ResourceManager;

public class SingleRegionCloudMeshGenerator extends CloudMeshGenerator
{
	private final CloudStyle style;
	private CloudInfo type;
	private float fadeStart;
	private float fadeEnd;
	private boolean needsNoiseRefreshing;
	private boolean needsFadeRefreshing;
	
	public SingleRegionCloudMeshGenerator(CloudInfo type, LevelOfDetailConfig lodConfig, int meshGenInterval, float fadeStart, float fadeEnd, CloudStyle style)
	{
		super(CloudMeshGenerator.MAIN_CUBE_MESH_GENERATOR, lodConfig, meshGenInterval);
		this.setCloudType(type);
		this.setFadeDistance(fadeStart, fadeEnd);
		this.style = style;
	}
	
	@Override
	protected Pair<Integer, Integer> determineMinimumAndMaximimumGenHeightsAt(float minX, float minZ, float maxX, float maxZ)
	{
		NoiseSettings config = this.type.noiseConfig();
		return Pair.of(config.getStartHeight(), config.getEndHeight());
	}
	
	public SingleRegionCloudMeshGenerator setFadeDistance(float fadeStart, float fadeEnd)
	{
		float fs = fadeStart;
		float fe = fadeEnd;
		if (fs > fe)
		{
			fs = fadeEnd;
			fe = fadeStart;
		}
		float newFs = fs * (float)this.getCloudAreaMaxRadius();
		float newFe = fe * (float)this.getCloudAreaMaxRadius();
		if (newFs != this.fadeStart)
			this.needsFadeRefreshing = true;
		this.fadeStart = newFs;
		if (newFe != this.fadeEnd)
			this.needsFadeRefreshing = true;
		this.fadeEnd = newFe;
		return this;
	}
	
	public CloudInfo getCloudType()
	{
		return this.type;
	}
	
	public float getFadeStart()
	{
		return this.fadeStart;
	}
	
	public float getFadeEnd()
	{
		return this.fadeEnd;
	}
	
	public void setCloudType(CloudInfo type)
	{
		this.type = type;
		this.needsNoiseRefreshing = true;
	}
	
	@Override
	protected ComputeShader createShader(ResourceManager manager) throws IOException
	{
		return ComputeShader.loadShader(this.meshShaderLoc, manager, LOCAL_SIZE, LOCAL_SIZE, LOCAL_SIZE, ImmutableMap.of("${TYPE}", "1", "${FADE_NEAR_ORIGIN}", "0", "${STYLE}", String.valueOf(this.style.getIndex())));
	}
	
	@Override
	protected void setupShader()
	{
		super.setupShader();
		
		this.shader.bindShaderStorageBuffer(LAYER_GROUPINGS_NAME, GL15.GL_STATIC_DRAW).allocateBuffer(20);
		this.shader.bindShaderStorageBuffer(NOISE_LAYERS_NAME, GL15.GL_STATIC_DRAW).allocateBuffer(AbstractNoiseSettings.Param.values().length * 4 * MAX_NOISE_LAYERS);
		
		this.shader.forUniform("FadeStart", (id, loc) -> {
			GL41.glProgramUniform1f(id, loc, this.fadeStart);
		});
		this.shader.forUniform("FadeEnd", (id, loc) -> {
			GL41.glProgramUniform1f(id, loc, this.fadeEnd);
		});
		
		this.uploadNoiseData();
		this.uploadFadeData();
		this.needsNoiseRefreshing = false;
		this.needsFadeRefreshing = false;
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
		}, AbstractNoiseSettings.Param.values().length * 4 * MAX_NOISE_LAYERS);
		
		this.shader.getShaderStorageBuffer(LAYER_GROUPINGS_NAME).writeData(b ->
		{
			b.putInt(0, 0);
			b.putInt(4, this.type.noiseConfig().layerCount());
			b.putFloat(8, this.type.storminess());
			b.putFloat(12, this.type.stormStart());
			b.putFloat(16, this.type.stormFadeDistance());
		}, 20);
	}
	
	private void uploadFadeData()
	{
		if (this.shader == null || !this.shader.isValid())
			return;
		
		this.shader.forUniform("FadeStart", (id, loc) -> {
			GL41.glProgramUniform1f(id, loc, this.fadeStart);
		});
		this.shader.forUniform("FadeEnd", (id, loc) -> {
			GL41.glProgramUniform1f(id, loc, this.fadeEnd);
		});
	}
	
	@Override
	protected int prepareMeshGen(double originX, double originY, double originZ, float meshGenOffsetX, float meshGenOffsetZ, @Nullable Frustum frustum, int interval)
	{
		if (this.needsNoiseRefreshing)
		{
			this.uploadNoiseData();
			this.needsNoiseRefreshing = false;
		}
		
		if (this.needsFadeRefreshing)
		{
			this.uploadFadeData();
			this.needsFadeRefreshing = false;
		}
		
		return super.prepareMeshGen(originX, originY, originZ, meshGenOffsetX, meshGenOffsetZ, frustum, interval);
	}
	
	@Override
	public void fillReport(CrashReportCategory category)
	{
		category.setDetail("Cloud Type", this.type);
		super.fillReport(category);
	}
}
