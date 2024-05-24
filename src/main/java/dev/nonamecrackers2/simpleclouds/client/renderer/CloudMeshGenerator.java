package dev.nonamecrackers2.simpleclouds.client.renderer;

import java.io.IOException;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

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
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/CloudMeshGenerator");
	private static final int[] LOD_LEVELS = new int[] {2, 6, 18};
	public static final int CHUNK_AMOUNT_SPAN_X = 16;
	public static final int CHUNK_AMOUNT_SPAN_Y = 4;
	public static final int CHUNK_AMOUNT_SPAN_Z = 16;
	public static final int WORK_X = 4;
	public static final int WORK_Y = 4;
	public static final int WORK_Z = 4;
	public static final int LOCAL_X = 8;
	public static final int LOCAL_Y = 8;
	public static final int LOCAL_Z = 8;
	private @Nullable ComputeShader shader;
	private float scrollX;
	private float scrollY;
	private float scrollZ;
	
	protected CloudMeshGenerator() {}
	
	public static int getCloudAreaMaxRadius()
	{
		int width = WORK_X * LOCAL_X * CHUNK_AMOUNT_SPAN_X;
		int length = WORK_Z * LOCAL_Z * CHUNK_AMOUNT_SPAN_Z;
		return width / 2 + length / 2;
	}
	
	public static int getCloudRenderDistance()
	{
		return Math.max(WORK_X * LOCAL_X * CHUNK_AMOUNT_SPAN_X, WORK_Z * LOCAL_Z * CHUNK_AMOUNT_SPAN_Z) / 2;
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
			this.shader = ComputeShader.loadShader(CUBE_MESH_GENERATOR, manager, LOCAL_X, LOCAL_Y, LOCAL_Z);
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
		
		int radiusX = CHUNK_AMOUNT_SPAN_X / 2;
		int radiusZ = CHUNK_AMOUNT_SPAN_Z / 2;
		float chunkSize = 32.0F * scale;
		float camOffsetX = ((float)Mth.floor(camX / chunkSize) * chunkSize);
		float camOffsetZ = ((float)Mth.floor(camZ / chunkSize) * chunkSize);
		int lodSpread = 1;
		int sizePerLod = (int)Math.pow(3, lodSpread);
		for (int lodLevel = 2; lodLevel <= 4; lodLevel++)
		{
			int prevSpan = getSpanForLodLevel(lodLevel - 1);
			int span = getSpanForLodLevel(lodLevel);
			int spanPerLodChunk = span / 3;
			for (int x = -sizePerLod - lodSpread; x <= sizePerLod + lodSpread; x++)
			{
				for (int z = -sizePerLod - lodSpread; z <= sizePerLod + lodSpread; z++)
				{
					if (x < -lodSpread || x > lodSpread || z < -lodSpread || z > lodSpread)
					{
						int y = 0;
						int currentX = x * spanPerLodChunk - spanPerLodChunk + prevSpan / 2;
						int currentZ = z * spanPerLodChunk - spanPerLodChunk + prevSpan / 2;
						float offsetX = (float)currentX * chunkSize;
						float offsetY = (float)y * chunkSize;
						float offsetZ = (float)currentZ * chunkSize;
						if (frustum == null || frustum.isVisible(new AABB(offsetX, offsetY, offsetZ, offsetX + chunkSize, offsetY + chunkSize, offsetZ + chunkSize).move(camOffsetX, 0.0F, camOffsetZ).move(-camX, -camY, -camZ)))
						{
							this.shader.forUniform("RenderOffset", loc -> {
								GL20.glUniform3f(loc, offsetX / scale + camOffsetX / scale, offsetY / scale, offsetZ / scale + camOffsetZ / scale);
							});
							this.shader.forUniform("Scale", loc -> {
								GL20.glUniform1f(loc, spanPerLodChunk);
							});
							this.shader.dispatch(WORK_X, WORK_Y, WORK_Z, false);
						}
					}
				}
			}
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
		}
		
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
	
	private void generateChunk(float chunkSize, int lodLevel, int x, int y, int z, @Nullable Frustum frustum, float camOffsetX, float camOffsetZ, double camX, double camY, double camZ, float scale)
	{
		float offsetX = (float)x * chunkSize * lodLevel;
		float offsetY = (float)y * chunkSize * lodLevel;
		float offsetZ = (float)z * chunkSize * lodLevel;
		if (frustum == null || frustum.isVisible(new AABB(offsetX, offsetY, offsetZ, offsetX + chunkSize, offsetY + chunkSize, offsetZ + chunkSize).move(camOffsetX, 0.0F, camOffsetZ).move(-camX, -camY, -camZ)))
		{
			this.shader.forUniform("RenderOffset", loc -> {
				GL20.glUniform3f(loc, offsetX / scale + camOffsetX / scale, offsetY / scale, offsetZ / scale + camOffsetZ / scale);
			});
			this.shader.dispatch(WORK_X, WORK_Y, WORK_Z, false);
		}
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
}
