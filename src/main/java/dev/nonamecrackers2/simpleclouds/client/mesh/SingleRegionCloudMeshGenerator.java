package dev.nonamecrackers2.simpleclouds.client.mesh;

import java.io.IOException;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import com.google.common.collect.ImmutableMap;

import dev.nonamecrackers2.simpleclouds.client.shader.compute.ComputeShader;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudInfo;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.noise.AbstractNoiseSettings;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.server.packs.resources.ResourceManager;

public class SingleRegionCloudMeshGenerator extends CloudMeshGenerator
{
	private CloudInfo type;
	private final float fadeStart;
	private final float fadeEnd;
	private boolean needsNoiseRefreshing;
	
	public SingleRegionCloudMeshGenerator(CloudType type, CloudMeshGenerator.LevelOfDetailConfig lodConfig, int meshGenInterval, float fadeStart, float fadeEnd)
	{
		super(CloudMeshGenerator.MAIN_CUBE_MESH_GENERATOR, lodConfig, meshGenInterval);
		this.type = type;
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
	}
	
	public void setCloudType(CloudInfo type)
	{
		this.type = type;
		this.needsNoiseRefreshing = true;
	}
	
	@Override
	protected ComputeShader createShader(ResourceManager manager) throws IOException
	{
		return ComputeShader.loadShader(this.meshShaderLoc, manager, LOCAL_SIZE, LOCAL_SIZE, LOCAL_SIZE, ImmutableMap.of("${TYPE}", "1"));
	}
	
	@Override
	protected void setupShader()
	{
		super.setupShader();
		this.shader.bindShaderStorageBuffer("LayerGroupings", GL15.GL_STATIC_DRAW).allocateBuffer(20);
		this.shader.bindShaderStorageBuffer("NoiseLayers", GL15.GL_STATIC_DRAW).allocateBuffer(AbstractNoiseSettings.Param.values().length * 4 * MAX_NOISE_LAYERS);
		this.shader.forUniform("FadeStart", loc -> {
			GL20.glUniform1f(loc, this.fadeStart);
		});
		this.shader.forUniform("FadeEnd", loc -> {
			GL20.glUniform1f(loc, this.fadeEnd);
		});
	}
	
	@Override
	protected void populateChunkGenTasks(double camX, double camY, double camZ, float scale, Frustum frustum)
	{
		if (this.needsNoiseRefreshing)
		{
			this.shader.getShaderStorageBuffer("NoiseLayers").writeData(b -> 
			{
				float[] packed = this.type.noiseConfig().packForShader();
				for (int i = 0; i < packed.length && i < AbstractNoiseSettings.Param.values().length * MAX_NOISE_LAYERS; i++)
					b.putFloat(i * 4, packed[i]);
			});
			this.shader.getShaderStorageBuffer("LayerGroupings").writeData(b ->
			{
				b.putInt(0, 0);
				b.putInt(4, this.type.noiseConfig().layerCount());
				b.putFloat(8, this.type.storminess());
				b.putFloat(12, this.type.stormStart());
				b.putFloat(16, this.type.stormFadeDistance());
			});
		}
		
		super.populateChunkGenTasks(camX, camY, camZ, scale, frustum);
	}
}
