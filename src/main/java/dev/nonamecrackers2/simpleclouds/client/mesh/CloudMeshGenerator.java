package dev.nonamecrackers2.simpleclouds.client.mesh;

import java.io.IOException;

import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.shader.SimpleCloudsShaders;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.ComputeShader;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.ShaderStorageBufferObject;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public abstract class CloudMeshGenerator implements AutoCloseable
{
	public static final int MAX_NOISE_LAYERS = 4;
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/CloudMeshGenerator");
	public static final CloudMeshGenerator.LevelOfDetailConfig[] LEVEL_OF_DETAIL = new CloudMeshGenerator.LevelOfDetailConfig[] {
		new CloudMeshGenerator.LevelOfDetailConfig(2, 4),
		new CloudMeshGenerator.LevelOfDetailConfig(4, 3),
		new CloudMeshGenerator.LevelOfDetailConfig(8, 2)
	};
	public static final ResourceLocation MAIN_CUBE_MESH_GENERATOR = SimpleCloudsMod.id("cube_mesh");
	public static final int EFFECTIVE_CHUNK_SPAN; //The total span of the complete renderable area, including all level of detail layers
	public static final int PRIMARY_CHUNK_SPAN = 8; //The total span of the primary, full detail cloud area
	public static final int VERTICAL_CHUNK_SPAN = 8;
	public static final int WORK_SIZE = 4;
	public static final int LOCAL_SIZE = 8;
	public static final int SIDE_BUFFER_SIZE = 368435456;
	public static final int ELEMENTS_PER_VERTEX = 7;
	public static final int BYTES_PER_VERTEX = 4 * ELEMENTS_PER_VERTEX;
	public static final int BYTES_PER_SIDE = BYTES_PER_VERTEX * 4;
	public static final int INDEX_BUFFER_SIZE = 107108864;
	protected final ResourceLocation meshShaderLoc;
	protected @Nullable ComputeShader shader;
	protected float scrollX;
	protected float scrollY;
	protected float scrollZ;
	protected int arrayObjectId = -1;
	protected int vertexBufferId = -1;
	protected int indexBufferId = -1;
	private int totalIndices;
	private int totalSides;
	private boolean testFacesFacingAway;
	
	static
	{
		int radius = PRIMARY_CHUNK_SPAN / 2;
		for (CloudMeshGenerator.LevelOfDetailConfig config : LEVEL_OF_DETAIL)
			radius += config.chunkScale() * config.spread();
		EFFECTIVE_CHUNK_SPAN = radius * 2;
	}
	
	public CloudMeshGenerator(ResourceLocation meshShaderLoc)
	{
		this.meshShaderLoc = meshShaderLoc;
	}
	
	public CloudMeshGenerator setTestFacesFacingAway(boolean flag)
	{
		this.testFacesFacingAway = flag;
		return this;
	}
	
	public static int getCloudAreaMaxRadius()
	{
		return EFFECTIVE_CHUNK_SPAN * WORK_SIZE * LOCAL_SIZE / 2;
	}
	
	@Override
	public void close()
	{
		if (this.shader != null)
			this.shader.close();
		this.shader = null;
		
		if (this.arrayObjectId >= 0)
		{
			RenderSystem.glDeleteVertexArrays(this.arrayObjectId);
			this.arrayObjectId = -1;
		}
	}
	
	protected ComputeShader createShader(ResourceManager manager) throws IOException
	{
		return ComputeShader.loadShader(this.meshShaderLoc, manager, LOCAL_SIZE, LOCAL_SIZE, LOCAL_SIZE);
	}
	
	protected void setupShader()
	{
		ShaderStorageBufferObject buffer = this.shader.bindShaderStorageBuffer("Counter", GL15.GL_DYNAMIC_DRAW);
		buffer.allocateBuffer(4);
		buffer.writeData(b -> {
			b.putInt(0, 0);
		});
		this.shader.bindShaderStorageBuffer("SideDataBuffer", GL15.GL_DYNAMIC_DRAW).allocateBuffer(SIDE_BUFFER_SIZE); //Vertex data, arbitrary size
		this.shader.bindShaderStorageBuffer("IndexBuffer", GL15.GL_DYNAMIC_DRAW).allocateBuffer(INDEX_BUFFER_SIZE); //Index data, arbitrary size
	}
	
	public void init(ResourceManager manager)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		
		if (this.arrayObjectId >= 0)
		{
			RenderSystem.glDeleteVertexArrays(this.arrayObjectId);
			this.arrayObjectId = -1;
		}
		
		if (this.vertexBufferId >= 0)
		{
			RenderSystem.glDeleteBuffers(this.vertexBufferId);
			this.vertexBufferId = -1;
		}
		
		if (this.indexBufferId >= 0)
		{
			RenderSystem.glDeleteBuffers(this.indexBufferId);
			this.indexBufferId = -1;
		}
		
		this.arrayObjectId = GL30.glGenVertexArrays();
		this.vertexBufferId = GL15.glGenBuffers();
		this.indexBufferId = GL15.glGenBuffers();
		
		GL30.glBindVertexArray(this.arrayObjectId);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vertexBufferId);
		//Vertex position
		GL20.glEnableVertexAttribArray(0);
		GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 28, 0);
		//Vertex color
		GL20.glEnableVertexAttribArray(1);
		GL20.glVertexAttribPointer(1, 1, GL11.GL_FLOAT, true, 28, 12);
		//Vertex normal
		GL20.glEnableVertexAttribArray(2);
		GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, true, 28, 16);
		
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.indexBufferId);
		
		GL30.glBindVertexArray(0);
		
		if (this.shader != null)
		{
			this.shader.close();
			this.shader = null;
		}
		
		try
		{
			this.shader = this.createShader(manager);
			this.setupShader();
			this.generateMesh(0.0D, 0.0D, 0.0D, 1.0F, null);
		}
		catch (IOException e)
		{
			LOGGER.warn("Failed to load compute shader", e);
		}
	}
	
	protected void rebindBuffers()
	{
		if (this.arrayObjectId != -1)
		{
			MutableInt totalSides = new MutableInt();
			this.shader.getShaderStorageBuffer("Counter").readData(b -> {
				totalSides.setValue(b.getInt(0));
			});
			this.totalSides = totalSides.getValue();
			this.totalIndices = this.totalSides * 6;
			int vertexBufferId = this.shader.getShaderStorageBuffer("SideDataBuffer").getId();
			int indexBufferId = this.shader.getShaderStorageBuffer("IndexBuffer").getId();
			
			GL30.glBindVertexArray(this.arrayObjectId);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBufferId);
			//Vertex position
			GL20.glEnableVertexAttribArray(0);
			GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 28, 0);
			//Vertex color
			GL20.glEnableVertexAttribArray(1);
			GL20.glVertexAttribPointer(1, 1, GL11.GL_FLOAT, true, 28, 12);
			//Vertex normal
			GL20.glEnableVertexAttribArray(2);
			GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, true, 28, 16);
			
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
			
			GL30.glBindVertexArray(0);
		}
	}
	
	public void setScroll(float x, float y, float z)
	{
		this.scrollX = x;
		this.scrollY = y;
		this.scrollZ = z;
	}
	
	protected void tryToGenerateChunk(int lodLevel, int lodScale, int x, int y, int z, int noOcclusionDirectionIndex, float scale, double camX, double camY, double camZ, @Nullable Frustum frustum)
	{
		float chunkSizeUpscaled = 32.0F * scale;
		float chunkSizeLod = 32.0F * scale * lodScale;
		float offsetX = (float)x * chunkSizeLod;
		float offsetY = (float)y * chunkSizeLod;
		float offsetZ = (float)z * chunkSizeLod;
		float camOffsetX = ((float)Mth.floor(camX / chunkSizeUpscaled) * chunkSizeUpscaled);
		float camOffsetZ = ((float)Mth.floor(camZ / chunkSizeUpscaled) * chunkSizeUpscaled);
		if (frustum == null || frustum.isVisible(new AABB(offsetX, offsetY + 1000.0F, offsetZ, offsetX + chunkSizeLod, offsetY - 1000.0F, offsetZ + chunkSizeLod).move(camOffsetX, 0.0F, camOffsetZ).move(-camX, -camY, -camZ)))
			this.generateChunk(lodLevel, lodScale, x, y, z, offsetX, offsetY, offsetZ, scale, camOffsetX, camOffsetZ, noOcclusionDirectionIndex);
	}
	
	protected void generateChunk(int lodLevel, int lodScale, int x, int y, int z, float offsetX, float offsetY, float offsetZ, float scale, float camOffsetX, float camOffsetZ, int noOcclusionDirectionIndex)
	{
		this.shader.forUniform("LodLevel", loc -> {
			GL20.glUniform1i(loc, lodLevel);
		});
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
	
	public void generateMesh(double camX, double camY, double camZ, float scale, @Nullable Frustum frustum)
	{
		if (this.shader != null && this.shader.isValid())
		{
			this.shader.getShaderStorageBuffer("Counter").writeData(b -> {
				b.putInt(0, 0);
			});
			this.shader.forUniform("Scroll", loc -> {
				GL20.glUniform3f(loc, this.scrollX, this.scrollY, this.scrollZ);
			});
			this.shader.forUniform("Origin", loc -> {
				GL20.glUniform3f(loc, (float)camX / scale, (float)camY / scale, (float)camZ / scale);
			});
			this.shader.forUniform("TestFacesFacingAway", loc -> {
				GL20.glUniform1i(loc, this.testFacesFacingAway ? 1 : 0);
			});
			
			int currentRadius = PRIMARY_CHUNK_SPAN / 2;
			for (int r = 0; r <= currentRadius; r++)
			{
				for (int y = 0; y < VERTICAL_CHUNK_SPAN; y++)
				{
					for (int x = -r; x < r; x++)
					{
						this.tryToGenerateChunk(0, 1, x, y, -r, -1, scale, camX, camY, camZ, frustum);
						this.tryToGenerateChunk(0, 1, x, y, r - 1, -1, scale, camX, camY, camZ, frustum);
					}
					for (int z = -r + 1; z < r - 1; z++)
					{
						this.tryToGenerateChunk(0, 1, -r, y, z, -1, scale, camX, camY, camZ, frustum);
						this.tryToGenerateChunk(0, 1, r - 1, y, z, -1, scale, camX, camY, camZ, frustum);
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
							this.tryToGenerateChunk(lodLevel, config.chunkScale(), x, y, -r, noOcclusion ? 5 : -1, scale, camX, camY, camZ, frustum);
							this.tryToGenerateChunk(lodLevel, config.chunkScale(), x, y, r - 1, noOcclusion ? 4 : -1, scale, camX, camY, camZ, frustum);
						}
						for (int z = -r + 1; z < r - 1; z++)
						{
							this.tryToGenerateChunk(lodLevel, config.chunkScale(), -r, y, z, noOcclusion ? 1 : -1, scale, camX, camY, camZ, frustum);
							this.tryToGenerateChunk(lodLevel, config.chunkScale(), r - 1, y, z, noOcclusion ? 0 : -1, scale, camX, camY, camZ, frustum);
						}
					}
				}
				currentRadius = currentRadius + config.spread() * config.chunkScale();
			}
			
			MutableInt totalSides = new MutableInt();
			this.shader.getShaderStorageBuffer("Counter").readData(b -> {
				totalSides.setValue(b.getInt(0));
			});
			this.totalSides = totalSides.getValue();
			this.totalIndices = this.totalSides * 6;
		}
	}
	
	public void render(PoseStack stack, Matrix4f projMat, float partialTick, float r, float g, float b)
	{
		RenderSystem.assertOnRenderThread();
		if (this.arrayObjectId != -1)
		{
			BufferUploader.reset();
			
			RenderSystem.disableBlend();
			RenderSystem.enableDepthTest();
			RenderSystem.setShaderColor(r, g, b, 1.0F);
			
			GL30.glBindVertexArray(this.arrayObjectId);
			
			RenderSystem.setShader(SimpleCloudsShaders::getCloudsShader);
			SimpleCloudsRenderer.prepareShader(RenderSystem.getShader(), stack.last().pose(), projMat);
			RenderSystem.getShader().apply();
			RenderSystem.drawElements(GL11.GL_TRIANGLES, this.totalIndices, GL11.GL_UNSIGNED_INT);
			RenderSystem.getShader().clear();
			
			GL30.glBindVertexArray(0);
			
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		}
	}
	
	public int getArrayObjectId()
	{
		return this.arrayObjectId;
	}
	
	public int getTotalIndices()
	{
		return this.totalIndices;
	}
	
	public int getTotalSides()
	{
		return this.totalSides;
	}
	
	public static record LevelOfDetailConfig(int chunkScale, int spread) {}
}
