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

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.AtomicCounter;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.BufferObject;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.ComputeShader;
import dev.nonamecrackers2.simpleclouds.common.noise.AbstractNoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.NoiseSettings;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public class CloudMeshGenerator implements AutoCloseable
{
	public static final int MAX_CLOUD_TYPES = 2;
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
	public static final int WORK_SIZE = 4;
	public static final int LOCAL_SIZE = 8;
	public static final int CLOUD_REGION_TEXTURE_SIZE;
	private final NoiseSettings[] noiseSettings = new NoiseSettings[MAX_CLOUD_TYPES];
	private @Nullable ComputeShader shader;
	private @Nullable ComputeShader cloudRegionShader;
	private float scrollX;
	private float scrollY;
	private float scrollZ;
	private int cloudRegionTexture;
	
	static
	{
		int radius = PRIMARY_CHUNK_SPAN / 2;
		int requiredRegionTexSize = PRIMARY_CHUNK_SPAN;
		for (CloudMeshGenerator.LevelOfDetailConfig config : LEVEL_OF_DETAIL)
		{
			radius += config.chunkScale() * config.spread();
			requiredRegionTexSize += config.spread() * 2;
		}
		EFFECTIVE_CHUNK_SPAN = radius * 2;
		CLOUD_REGION_TEXTURE_SIZE = requiredRegionTexSize * 32;
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
	
	public void init(ResourceManager manager, NoiseSettings[] settings)
	{
		if (settings.length != this.noiseSettings.length)
			throw new IllegalArgumentException("Length of noise settings must match total amount of cloud types. Received " + settings.length + ", expected " + MAX_CLOUD_TYPES);
		
		LOGGER.debug("Initializing mesh generator...");
		
		RenderSystem.assertOnRenderThreadOrInit();
		
		for (int i = 0; i < MAX_CLOUD_TYPES; i++)
			this.noiseSettings[i] = settings[i];
		
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
			this.shader.bindShaderStorageBuffer(3, GL15.GL_STATIC_DRAW).allocateBuffer(AbstractNoiseSettings.Param.values().length * 4 * MAX_NOISE_LAYERS * MAX_CLOUD_TYPES);
			this.shader.bindShaderStorageBuffer(4, GL15.GL_STATIC_DRAW).allocateBuffer(8 * MAX_CLOUD_TYPES);
			this.generateMesh(false, 0.0F, 0.0F, 0.0F, 0.5F, 1.0F, null);
			LOGGER.debug("Created mesh generator compute shader");
		}
		catch (IOException e)
		{
			LOGGER.warn("Failed to load compute shader", e);
		}
		
		if (this.cloudRegionShader != null)
		{
			this.cloudRegionShader.close();
			this.cloudRegionShader = null;
		}
		
		try
		{
			this.cloudRegionShader = ComputeShader.loadShader(CLOUD_REGIONS_GENERATOR, manager, 8, 8, 1);
			var buffer = this.cloudRegionShader.bindShaderStorageBuffer(0, GL15.GL_STATIC_DRAW);
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
				GL20.glUniform1f(loc, 1000.0F);//Mth.clamp(Mth.sin(this.test * 0.01F), 0.1F, 1.0F));
			});
			this.cloudRegionShader.forUniform("TotalCloudTypes", loc -> {
				GL20.glUniform1i(loc, MAX_CLOUD_TYPES);
			});
			this.cloudRegionShader.dispatchAndWait(CLOUD_REGION_TEXTURE_SIZE / 8, CLOUD_REGION_TEXTURE_SIZE / 8, LEVEL_OF_DETAIL.length + 1);
			LOGGER.debug("Created cloud region texture generator compute shader");
		}
		catch (IOException e)
		{
			LOGGER.warn("Failed to load cloud region compute shader", e);
		}
		
		LOGGER.debug("Total LODs: {}", LEVEL_OF_DETAIL.length + 1);
		LOGGER.debug("Highest detail (primary) chunk span: {}", PRIMARY_CHUNK_SPAN);
		LOGGER.debug("Effective chunk span with LODs (total viewable area): {}", EFFECTIVE_CHUNK_SPAN);
	}
	
	public void setScroll(float x, float y, float z)
	{
		this.scrollX = x;
		this.scrollY = y;
		this.scrollZ = z;
	}
	
	public void generateMesh(boolean addMovementSmoothing, double camX, double camY, double camZ, float threshold, float scale, @Nullable Frustum frustum)
	{
		if (this.cloudRegionShader != null)
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
		
		this.shader.<AtomicCounter>getBufferObject(0).set(0);
		this.shader.<BufferObject>getBufferObject(4).writeData(b -> 
		{
			int currentIndex = 0;
			int previousLayerIndex = 0;
			for (int i = 0; i < MAX_CLOUD_TYPES; i++)
			{
				NoiseSettings settings = this.noiseSettings[i];
				int layerCount = settings.layerCount();
				b.putInt(currentIndex, previousLayerIndex);
				currentIndex += 4;
				b.putInt(currentIndex, previousLayerIndex + layerCount);
				currentIndex += 4;
				previousLayerIndex += layerCount;
			}
		});
		this.shader.<BufferObject>getBufferObject(3).writeData(b -> 
		{
			int index = 0;
			for (int i = 0; i < MAX_CLOUD_TYPES; i++)
			{
				NoiseSettings settings = this.noiseSettings[i];
				float[] packed = settings.packForShader();
				for (int j = 0; j < packed.length && j < AbstractNoiseSettings.Param.values().length * MAX_NOISE_LAYERS; j++)
				{
					b.putFloat(index, packed[j]);
					index += 4;
				}
			}
		});
		this.shader.forUniform("Scroll", loc -> {
			GL20.glUniform3f(loc, this.scrollX, this.scrollY, this.scrollZ);
		});
		
		interface ChunkGenerator {
			void generate(int lodLevel, int lodScale, int x, int y, int z, int noOcclusionDirectionIndex, int regionRadius);
		}
		ChunkGenerator generator = (lodLevel, lodScale, x, y, z, noOcclusionDirectionIndex, regionRadius) ->
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
				this.shader.forUniform("LodLevel", loc -> {
					GL20.glUniform1i(loc, lodLevel);
				});
				this.shader.forUniform("RenderOffset", loc -> {
					GL20.glUniform3f(loc, offsetX / scale + camOffsetX / scale, offsetY / scale, offsetZ / scale + camOffsetZ / scale);
				});
				this.shader.forUniform("RegionSampleOffset", loc -> {
					GL20.glUniform2f(loc, x * 32.0F + (float)CLOUD_REGION_TEXTURE_SIZE / 2.0F, z * 32.0F + (float)CLOUD_REGION_TEXTURE_SIZE / 2.0F);
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
					generator.generate(0, 1, x, y, -r, -1, currentRadius);
					generator.generate(0, 1, x, y, r - 1, -1, currentRadius);
				}
				for (int z = -r + 1; z < r - 1; z++)
				{
					generator.generate(0, 1, -r, y, z, -1, currentRadius);
					generator.generate(0, 1, r - 1, y, z, -1, currentRadius);
				}
			}
		}
		
		for (int i = 0; i < LEVEL_OF_DETAIL.length; i++)
		{
			CloudMeshGenerator.LevelOfDetailConfig config = LEVEL_OF_DETAIL[i];
			int lodLevel = i + 1;
			for (int deltaR = 1; deltaR <= config.spread(); deltaR++)
			{
				int ySpan = Mth.ceil((float)VERTICAL_CHUNK_SPAN / (float)config.chunkScale());
				boolean noOcclusion = deltaR == 1;
				for (int y = 0; y < ySpan; y++)
				{
					int r = currentRadius / config.chunkScale() + deltaR;
					for (int x = -r; x < r; x++)
					{
						generator.generate(lodLevel, config.chunkScale(), x, y, -r, noOcclusion ? 5 : -1, currentRadius);
						generator.generate(lodLevel, config.chunkScale(), x, y, r - 1, noOcclusion ? 4 : -1, currentRadius);
					}
					for (int z = -r + 1; z < r - 1; z++)
					{
						generator.generate(lodLevel, config.chunkScale(), -r, y, z, noOcclusion ? 1 : -1, currentRadius);
						generator.generate(lodLevel, config.chunkScale(), r - 1, y, z, noOcclusion ? 0 : -1, currentRadius);
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
