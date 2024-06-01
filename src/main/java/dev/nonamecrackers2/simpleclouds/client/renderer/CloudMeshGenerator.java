package dev.nonamecrackers2.simpleclouds.client.renderer;

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
import org.lwjgl.opengl.GL45;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.AtomicCounter;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.BufferObject;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.ComputeShader;
import dev.nonamecrackers2.simpleclouds.common.noise.AbstractNoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.NoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.StaticNoiseSettings;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public class CloudMeshGenerator implements AutoCloseable
{
	public static final int MAX_NOISE_LAYERS = 4;
	private static final ResourceLocation CUBE_MESH_GENERATOR = SimpleCloudsMod.id("cube_mesh");
	private static final ResourceLocation CLOUD_REGIONS_GENERATOR = SimpleCloudsMod.id("cloud_regions");
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/CloudMeshGenerator");
	private static final CloudMeshGenerator.LevelOfDetailConfig[] LEVEL_OF_DETAIL = new CloudMeshGenerator.LevelOfDetailConfig[] {
		new CloudMeshGenerator.LevelOfDetailConfig(2, 4),
		new CloudMeshGenerator.LevelOfDetailConfig(4, 3),
		new CloudMeshGenerator.LevelOfDetailConfig(8, 2)
	};
	public static final int EFFECTIVE_CHUNK_SPAN; //The total span of the complete renderable area, including all level of detail layers
	public static final int PRIMARY_CHUNK_SPAN = 8; //The total span of the primary, full detail cloud area
	public static final int VERTICAL_CHUNK_SPAN = 4;
//	public static final int CHUNK_AMOUNT_SPAN_X = 16;
//	public static final int CHUNK_AMOUNT_SPAN_Y = 4;
//	public static final int CHUNK_AMOUNT_SPAN_Z = 16;
	public static final int WORK_SIZE = 4;
	public static final int LOCAL_SIZE = 8;
	private @Nullable ComputeShader shader;
	private @Nullable ComputeShader cloudRegionShader;
	private float scrollX;
	private float scrollY;
	private float scrollZ;
	private int cloudRegionTexture;
	
	static
	{
		int radius = PRIMARY_CHUNK_SPAN / 2;
		for (CloudMeshGenerator.LevelOfDetailConfig config : LEVEL_OF_DETAIL)
			radius += config.chunkScale() * config.spread();
		EFFECTIVE_CHUNK_SPAN = radius * 2;
	}
	
	protected CloudMeshGenerator() {}
	
	public static int getCloudAreaMaxRadius()
	{
		return EFFECTIVE_CHUNK_SPAN * WORK_SIZE * LOCAL_SIZE / 2;
//		int width = WORK_X * LOCAL_X * CHUNK_AMOUNT_SPAN_X;
//		int length = WORK_Z * LOCAL_Z * CHUNK_AMOUNT_SPAN_Z;
//		return width / 2 + length / 2;
	}
	
	public static int getCloudRenderDistance()
	{
		return getCloudAreaMaxRadius();
		//return Math.max(WORK_X * LOCAL_X * CHUNK_AMOUNT_SPAN_X, WORK_Z * LOCAL_Z * CHUNK_AMOUNT_SPAN_Z) / 2;
	}
	
	public int getCloudRegionTextureId()
	{
		return this.cloudRegionTexture;
	}
	
	@Override
	public void close()
	{
		if (this.shader != null)
			this.shader.close();
	}
	
	public void init(ResourceManager manager)
	{
		if (this.shader != null)
		{
			this.shader.close();
			this.shader = null;
		}
		
		try
		{
			this.shader = ComputeShader.loadShader(CUBE_MESH_GENERATOR, manager, LOCAL_SIZE, LOCAL_SIZE, LOCAL_SIZE);
			this.shader.bindAtomicCounter(0, GL15.GL_DYNAMIC_DRAW); //Counter
			this.shader.bindShaderStorageBuffer(1, GL15.GL_DYNAMIC_DRAW).allocateBuffer(368435456); //Vertex data, arbitrary size
			this.shader.bindShaderStorageBuffer(2, GL15.GL_DYNAMIC_DRAW).allocateBuffer(107108864); //Index data, arbitrary size
			this.shader.bindShaderStorageBuffer(3, GL15.GL_STATIC_DRAW).allocateBuffer(AbstractNoiseSettings.Param.values().length * 4 * MAX_NOISE_LAYERS);
			this.generateMesh(StaticNoiseSettings.DEFAULT, false, 0.0F, 0.0F, 0.0F, 0.5F, 1.0F, null);
		}
		catch (IOException e)
		{
			LOGGER.warn("Failed to load compute shader", e);
		}
		
		if (this.cloudRegionTexture >= 0)
		{
			TextureUtil.releaseTextureId(this.cloudRegionTexture);
			this.cloudRegionTexture = -1;
		}
		
		this.cloudRegionTexture = TextureUtil.generateTextureId();
		GlStateManager._bindTexture(this.cloudRegionTexture);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R32UI, 256, 256, 0, GL30.GL_RED_INTEGER, GL11.GL_UNSIGNED_INT, (IntBuffer)null);
		
		if (this.cloudRegionShader != null)
		{
			this.cloudRegionShader.close();
			this.cloudRegionShader = null;
		}
		
		try
		{
			this.cloudRegionShader = ComputeShader.loadShader(CLOUD_REGIONS_GENERATOR, manager, 8, 8, 1);
			GL42.glBindImageTexture(0, this.cloudRegionTexture, 0, false, 0, GL42.GL_READ_WRITE, GL30.GL_R32UI);
			this.cloudRegionShader.dispatchAndWait(32, 32, 1);
		}
		catch (IOException e)
		{
			LOGGER.warn("Failed to load cloud region compute shader", e);
		}
	}
	
	public void setScroll(float x, float y, float z)
	{
		this.scrollX = x;
		this.scrollY = y;
		this.scrollZ = z;
	}
	
	public void generateMesh(NoiseSettings settings, boolean addMovementSmoothing, double camX, double camY, double camZ, float threshold, float scale, @Nullable Frustum frustum)
	{
		int count = Math.min(settings.layerCount(), MAX_NOISE_LAYERS);
		
		this.shader.<AtomicCounter>getBufferObject(0).set(0);
		this.shader.forUniform("LayerCount", loc -> {
			GL20.glUniform1i(loc, count);
		});
		this.shader.<BufferObject>getBufferObject(3).writeData(b -> 
		{
			float[] packed = settings.packForShader();
			for (int i = 0; i < packed.length && i < AbstractNoiseSettings.Param.values().length * MAX_NOISE_LAYERS; i++)
				b.putFloat(i * 4, packed[i]);
		});
		this.shader.forUniform("Scroll", loc -> {
			GL20.glUniform3f(loc, this.scrollX, this.scrollY, this.scrollZ);
		});
		
		interface ChunkGenerator {
			void generate(int lodScale, int x, int y, int z, int noOcclusionDirectionIndex);
		}
		ChunkGenerator generator = (lodScale, x, y, z, noOcclusionDirectionIndex) ->
		{
			float chunkSizeUpscaled = 32.0F * scale;
			float chunkSizeLod = 32.0F * scale * lodScale;
			float offsetX = (float)x * chunkSizeLod;
			float offsetY = (float)y * chunkSizeLod;
			float offsetZ = (float)z * chunkSizeLod;
			float camOffsetX = ((float)Mth.floor(camX / chunkSizeUpscaled) * chunkSizeUpscaled);
			float camOffsetZ = ((float)Mth.floor(camZ / chunkSizeUpscaled) * chunkSizeUpscaled);
			if (frustum == null || frustum.isVisible(new AABB(offsetX, offsetY, offsetZ, offsetX + chunkSizeLod, offsetY + chunkSizeLod, offsetZ + chunkSizeLod).move(camOffsetX, 0.0F, camOffsetZ).move(-camX, -camY, -camZ)))
			{
				this.shader.forUniform("RenderOffset", loc -> {
					GL20.glUniform3f(loc, offsetX / scale + camOffsetX / scale, offsetY / scale, offsetZ / scale + camOffsetZ / scale);
				});
				this.shader.forUniform("Scale", loc -> {
					GL20.glUniform1f(loc, lodScale);
				});
				this.shader.forUniform("DoNotOccludeSide", loc -> {
					GL20.glUniform1i(loc, noOcclusionDirectionIndex);
				});
				this.shader.dispatch(WORK_SIZE, WORK_SIZE, WORK_SIZE, false);
			}
		};
		
		int currentRadius = PRIMARY_CHUNK_SPAN / 2;
		for (int r = 0; r <= currentRadius; r++)
		{
			for (int y = 0; y < VERTICAL_CHUNK_SPAN; y++)
			{
				for (int x = -r; x < r; x++)
				{
					generator.generate(1, x, y, -r, -1);
					generator.generate(1, x, y, r - 1, -1);
				}
				for (int z = -r + 1; z < r - 1; z++)
				{
					generator.generate(1, -r, y, z, -1);
					generator.generate(1, r - 1, y, z, -1);
				}
			}
		}
		
		for (CloudMeshGenerator.LevelOfDetailConfig config : LEVEL_OF_DETAIL)
		{
			for (int deltaR = 1; deltaR <= config.spread(); deltaR++)
			{
				int ySpan = Mth.ceil((float)VERTICAL_CHUNK_SPAN / (float)config.chunkScale());
				boolean noOcclusion = deltaR == 1;
				for (int y = 0; y < ySpan; y++)
				{
					int r = currentRadius / config.chunkScale() + deltaR;
					for (int x = -r; x < r; x++)
					{
						generator.generate(config.chunkScale(), x, y, -r, noOcclusion ? 5 : -1);
						generator.generate(config.chunkScale(), x, y, r - 1, noOcclusion ? 4 : -1);
					}
					for (int z = -r + 1; z < r - 1; z++)
					{
						generator.generate(config.chunkScale(), -r, y, z, noOcclusion ? 1 : -1);
						generator.generate(config.chunkScale(), r - 1, y, z, noOcclusion ? 0 : -1);
					}
				}
			}
			currentRadius = currentRadius + config.spread() * config.chunkScale();
		}
		
//		int lodSpread = 1;
//		int sizePerLod = (int)Math.pow(3, lodSpread);
//		for (int lodLevel = 2; lodLevel <= 4; lodLevel++)
//		{
//			int prevSpan = getSpanForLodLevel(lodLevel - 1);
//			int span = getSpanForLodLevel(lodLevel);
//			int spanPerLodChunk = span / 3;
//			for (int x = -sizePerLod - lodSpread; x <= sizePerLod + lodSpread; x++)
//			{
//				for (int z = -sizePerLod - lodSpread; z <= sizePerLod + lodSpread; z++)
//				{
//					if (x < -lodSpread || x > lodSpread || z < -lodSpread || z > lodSpread)
//					{
//						int y = 0;
//						int currentX = x * spanPerLodChunk - spanPerLodChunk + prevSpan / 2;
//						int currentZ = z * spanPerLodChunk - spanPerLodChunk + prevSpan / 2;
//						float offsetX = (float)currentX * chunkSize;
//						float offsetY = (float)y * chunkSize;
//						float offsetZ = (float)currentZ * chunkSize;
//						if (frustum == null || frustum.isVisible(new AABB(offsetX, offsetY, offsetZ, offsetX + chunkSize, offsetY + chunkSize, offsetZ + chunkSize).move(camOffsetX, 0.0F, camOffsetZ).move(-camX, -camY, -camZ)))
//						{
//							this.shader.forUniform("RenderOffset", loc -> {
//								GL20.glUniform3f(loc, offsetX / scale + camOffsetX / scale, offsetY / scale, offsetZ / scale + camOffsetZ / scale);
//							});
//							this.shader.forUniform("Scale", loc -> {
//								GL20.glUniform1f(loc, spanPerLodChunk);
//							});
//							this.shader.dispatch(WORK_SIZE, WORK_SIZE, WORK_SIZE, false);
//						}
//					}
//				}
//			}
//			for (int x = -r; x < r; x++)
//			{
//				for (int y = 0; y < CHUNK_AMOUNT_SPAN_Y; y++)
//				{
//					this.generateChunk(chunkSize, r, x, y, -r, frustum, camOffsetX, camOffsetZ, camX, camY, camZ, scale);
//					this.generateChunk(chunkSize, r, x, y, r - 1, frustum, camOffsetX, camOffsetZ, camX, camY, camZ, scale);
//				}
//			}
//			for (int z = -r + 1; z < r - 1; z++)
//			{
//				for (int y = 0; y < CHUNK_AMOUNT_SPAN_Y; y++)
//				{
//					this.generateChunk(chunkSize, r, -r, y, z, frustum, camOffsetX, camOffsetZ, camX, camY, camZ, scale);
//					this.generateChunk(chunkSize, r, r - 1, y, z, frustum, camOffsetX, camOffsetZ, camX, camY, camZ, scale);
//				}
//			}
//		}
		
//		int x = 0;
//		int z = 0;
//		int dx = 0;
//		int dz = -1;
//		float chunkSize = 32.0F * scale;
//		float camOffsetX = ((float)Mth.floor(camX / chunkSize) * chunkSize);
//		float camOffsetZ = ((float)Mth.floor(camZ / chunkSize) * chunkSize);
//		int t = Math.max(CHUNK_AMOUNT_SPAN_X, CHUNK_AMOUNT_SPAN_Z);//(int)Math.pow(Math.max(radiusX * 2, radiusZ * 2), 2);
//		int iterations = t*t;
//		for (int i = 0; i < iterations; i++)
//		{
//			if (-CHUNK_AMOUNT_SPAN_X/2 <= x && x <= CHUNK_AMOUNT_SPAN_X/2 && -CHUNK_AMOUNT_SPAN_Z/2 <= z && z <= CHUNK_AMOUNT_SPAN_Z/2)
//			{
//				for (int y = 0; y < CHUNK_AMOUNT_SPAN_Y; y++)
//				{
//					int span = Mth.floor(Mth.sqrt((float)i));
//					int lodLevel = -1;
//					for (int j = 0; j < LOD_LEVELS.length; j++)
//					{
//						int lodSpan = LOD_LEVELS[j];
//						if (lodLevel == -1 || (span >= lodSpan && lodLevel < j + 1))
//							lodLevel = j + 1;
//					}
//					
//					float offsetX = (float)(x - 1) * chunkSize * lodLevel;
//					float offsetY = (float)y * chunkSize * lodLevel;
//					float offsetZ = (float)(z - 1) * chunkSize * lodLevel;
//					if (frustum == null || frustum.isVisible(new AABB(offsetX, offsetY, offsetZ, offsetX + chunkSize, offsetY + chunkSize, offsetZ + chunkSize).move(camOffsetX, 0.0F, camOffsetZ).move(-camX, -camY, -camZ)))
//					{
//						this.shader.forUniform("RenderOffset", loc -> {
//							GL20.glUniform3f(loc, offsetX / scale + camOffsetX / scale, offsetY / scale, offsetZ / scale + camOffsetZ / scale);
//						});
//						this.shader.dispatch(WORK_X, WORK_Y, WORK_Z, false);
//					}
//				}
//			}
//			if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z))
//			{
//				t = dx;
//				dx = -dz;
//				dz = t;
//			}
//			x += dx;
//			z += dz;
//		}
		
//		for (int x = -radiusX; x < radiusX; x++)
//		{
//			for (int y = 0; y < CHUNK_AMOUNT_SPAN_Y; y++)
//			{
//				for (int z = -radiusZ; z < radiusZ; z++)
//				{
//					float offsetX = (float)x * chunkSize;
//					float offsetY = (float)y * chunkSize;
//					float offsetZ = (float)z * chunkSize;
//					if (frustum == null || frustum.isVisible(new AABB(offsetX, offsetY, offsetZ, offsetX + chunkSize, offsetY + chunkSize, offsetZ + chunkSize).move(camOffsetX, 0.0F, camOffsetZ).move(-camX, -camY, -camZ)))
//					{
//						this.shader.forUniform("RenderOffset", loc -> {
//							GL20.glUniform3f(loc, offsetX / scale + camOffsetX / scale, offsetY / scale, offsetZ / scale + camOffsetZ / scale);
//						});
//						this.shader.dispatch(WORK_X, WORK_Y, WORK_Z, false);
//					}
//				}
//			}
//		}
	}
	
	private static int getSpanForLodLevel(int lodLevel)
	{
		if (lodLevel <= 1)
			return 2;
		else
			return 2 * (int)Math.pow(3, lodLevel - 1);
	}
	
	public @Nullable ComputeShader getShader()
	{
		return this.shader;
	}
	
	private static record LevelOfDetailConfig(int chunkScale, int spread) {}
}
