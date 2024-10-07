package dev.nonamecrackers2.simpleclouds.client.mesh;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.system.MemoryUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.shader.SimpleCloudsShaders;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.ComputeShader;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.ShaderStorageBufferObject;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import net.minecraft.CrashReportCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public abstract class CloudMeshGenerator
{
	public static final int MAX_NOISE_LAYERS = 4;
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/CloudMeshGenerator");
	public static final ResourceLocation MAIN_CUBE_MESH_GENERATOR = SimpleCloudsMod.id("cube_mesh");
	public static final int VERTICAL_CHUNK_SPAN = 8;
	public static final int WORK_SIZE = 4;
	public static final int LOCAL_SIZE = 8;
	public static final int MAX_SIDE_BUFFER_SIZE = 335544320;
	public static final int BYTES_PER_VERTEX = 20;
	public static final int BYTES_PER_SIDE = BYTES_PER_VERTEX * 4;
	public static final int MAX_INDEX_BUFFER_SIZE = 100663296;
	protected final ResourceLocation meshShaderLoc;
	protected final Queue<Runnable> chunkGenTasks = Queues.newArrayDeque();
	protected CloudMeshGenerator.LevelOfDetailConfig lodConfig;
	protected int meshGenInterval;
	protected int tasksPerTick;
	protected @Nullable ComputeShader shader;
	protected float scrollX;
	protected float scrollY;
	protected float scrollZ;
	protected int arrayObjectId = -1;
	protected int vertexBufferId = -1;
	protected int indexBufferId = -1;
	private @Nullable ByteBuffer vertexBuffer;
	private @Nullable ByteBuffer indexBuffer;
	protected int totalIndices;
	protected int totalSides;
	protected boolean testFacesFacingAway;
	private double currentCamX;
	private double currentCamY;
	private double currentCamZ;
	private float currentScale;
	private float cullDistance;
	private boolean lodConfigWasChanged;
	private int sideBufferSize;
	private int indexBufferSize;
	
	public CloudMeshGenerator(ResourceLocation meshShaderLoc, CloudMeshGenerator.LevelOfDetailConfig lodConfig, int meshGenInterval)
	{
		this.meshShaderLoc = meshShaderLoc;
		this.setLodConfig(lodConfig);
		this.setMeshGenInterval(meshGenInterval);
	}
	
	public void setLodConfig(CloudMeshGenerator.LevelOfDetailConfig config)
	{
		if (config != this.lodConfig)
		{
			this.lodConfig = Objects.requireNonNull(config);
			this.lodConfigWasChanged = true;
		}
	}
	
	public CloudMeshGenerator.LevelOfDetailConfig getLodConfig()
	{
		return this.lodConfig;
	}
	
	public void setMeshGenInterval(int interval)
	{
		if (interval <= 0)
			throw new IllegalArgumentException("Please input a mesh gen interval greater than 0");
		this.meshGenInterval = interval;
	}
	
	public CloudMeshGenerator setTestFacesFacingAway(boolean flag)
	{
		this.testFacesFacingAway = flag;
		return this;
	}
	
	public int getCloudAreaMaxRadius()
	{
		return this.lodConfig.getEffectiveChunkSpan() * WORK_SIZE * LOCAL_SIZE / 2;
	}
	
	public void setCullDistance(float dist)
	{
		if (dist <= 0.0F)
			throw new IllegalArgumentException("Cull distance must be greater than zero");
		this.cullDistance = dist;
	}
	
	public void disableCullDistance()
	{
		this.cullDistance = 0.0F;
	}
	
	public void close()
	{
		RenderSystem.assertOnRenderThreadOrInit();
		
		GL42.glMemoryBarrier(GL42.GL_ALL_BARRIER_BITS);
		this.chunkGenTasks.clear();
		
		if (this.shader != null)
			this.shader.close();
		this.shader = null;
		
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
	}
	
	protected ComputeShader createShader(ResourceManager manager) throws IOException
	{
		return ComputeShader.loadShader(this.meshShaderLoc, manager, LOCAL_SIZE, LOCAL_SIZE, LOCAL_SIZE);
	}
	
	protected void setupShader()
	{
		ShaderStorageBufferObject buffer = this.shader.bindShaderStorageBuffer("Counter", GL15.GL_DYNAMIC_COPY);
		buffer.allocateBuffer(4);
		buffer.writeData(b -> {
			b.putInt(0, 0);
		}, 4);
		this.sideBufferSize = this.shader.bindShaderStorageBuffer("SideDataBuffer", GL15.GL_DYNAMIC_COPY).allocateBuffer(MAX_SIDE_BUFFER_SIZE); //Vertex data, arbitrary size
		this.indexBufferSize = this.shader.bindShaderStorageBuffer("IndexBuffer", GL15.GL_DYNAMIC_COPY).allocateBuffer(MAX_SIDE_BUFFER_SIZE); //Index data, arbitrary size
	}
	
	public final GeneratorInitializeResult init(ResourceManager manager)
	{
		GeneratorInitializeResult.Builder builder = GeneratorInitializeResult.builder();
				
		if (!RenderSystem.isOnRenderThreadOrInit())
			return builder.errorUnknown(new IllegalStateException("Init not called on render thread"), "Head").build();
		
		GL42.glMemoryBarrier(GL42.GL_ALL_BARRIER_BITS);
		this.chunkGenTasks.clear();
		
		LOGGER.debug("Beginning mesh generator initialization");
		
		this.totalIndices = 0;
		this.totalSides = 0;
		
		if (this.arrayObjectId >= 0)
		{
			LOGGER.debug("Freeing VBA");
			RenderSystem.glDeleteVertexArrays(this.arrayObjectId);
			this.arrayObjectId = -1;
		}
		
		if (this.vertexBufferId >= 0)
		{
			LOGGER.debug("Freeing vertex buffer");
			RenderSystem.glDeleteBuffers(this.vertexBufferId);
			this.vertexBufferId = -1;
		}
		
		if (this.vertexBuffer != null)
		{
			MemoryUtil.memFree(this.vertexBuffer);
			this.vertexBuffer = null;
		}
		
		if (this.indexBufferId >= 0)
		{
			LOGGER.debug("Freeing index buffer");
			RenderSystem.glDeleteBuffers(this.indexBufferId);
			this.indexBufferId = -1;
		}
		
		if (this.indexBuffer != null)
		{
			MemoryUtil.memFree(this.vertexBuffer);
			this.indexBuffer = null;
		}
		
		if (this.shader != null)
		{
			LOGGER.debug("Freeing mesh compute shader");
			this.shader.close();
			this.shader = null;
		}
		
		try
		{
			this.initExtra(manager);
		}
		catch (Exception e)
		{
			builder.errorUnknown(e, "Init Extra");
		}
		
		try
		{
			LOGGER.debug("Creating mesh compute shader...");
			this.shader = this.createShader(manager);
			this.setupShader();
			this.onLodConfigChanged();
			this.lodConfigWasChanged = false;
		}
		catch (IOException e)
		{
			//LOGGER.warn("Failed to load compute shader", e);
			builder.errorCouldNotLoadMeshScript(e, "Compute Shader");
		}
		catch (Exception e)
		{
			builder.errorRecommendations(e, "Compute Shader");
		}
		
		this.arrayObjectId = GL30.glGenVertexArrays();
		this.vertexBufferId = GL15.glGenBuffers();
		this.indexBufferId = GL15.glGenBuffers();
		
		GL30.glBindVertexArray(this.arrayObjectId);
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vertexBufferId);
		this.vertexBuffer = MemoryUtil.memAlloc(this.sideBufferSize);
		GlStateManager._glBufferData(GL15.GL_ARRAY_BUFFER, this.vertexBuffer, GL15.GL_DYNAMIC_DRAW);
//		//Vertex position
//		GL20.glEnableVertexAttribArray(0);
//		GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 20, 0);
//		//Vertex brightness
//		GL20.glEnableVertexAttribArray(1);
//		GL20.glVertexAttribPointer(1, 1, GL11.GL_FLOAT, true, 20, 12);
//		//Vertex normal index
//		GL20.glEnableVertexAttribArray(2);
//		//GL30.glVertexAttribIPointer(2, 1, GL11.GL_INT, 20, 16);
//		GL30.glVertexAttribIPointer(2, 1, GL11.GL_INT, 20, 16);
		SimpleCloudsShaders.POSITION_BRIGHTNESS_NORMAL_INDEX.setupBufferState();
		
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.indexBufferId);
		this.indexBuffer = MemoryUtil.memAlloc(this.indexBufferSize);
		GlStateManager._glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, this.indexBuffer, GL15.GL_DYNAMIC_DRAW);
		
		GL30.glBindVertexArray(0);
		
		LOGGER.debug("Created VBA with vertex buffer size {} and index buffer size {}", this.sideBufferSize, this.indexBufferSize);
		
		ComputeShader.printDebug();
		
		LOGGER.debug("Finished initializing mesh generator");
		
		return builder.build();
	}
	
	protected void initExtra(ResourceManager manager) {}
	
	protected void copyDataOver(int totalSides)
	{
		if (this.vertexBufferId != -1 && this.indexBufferId != -1 && this.shader != null && this.shader.isValid())
		{
			GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
			
			this.totalSides = totalSides;
			this.totalIndices = this.totalSides * 6;
			
			if (this.totalSides > 0)
			{
				int vertexBufferId = this.shader.getShaderStorageBuffer("SideDataBuffer").getId();
				int indexBufferId = this.shader.getShaderStorageBuffer("IndexBuffer").getId();
				GlStateManager._glBindBuffer(GL31.GL_COPY_READ_BUFFER, vertexBufferId);
				GlStateManager._glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, this.vertexBufferId);
				GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER, 0, 0, this.totalSides * BYTES_PER_SIDE);
				GlStateManager._glBindBuffer(GL31.GL_COPY_READ_BUFFER, indexBufferId);
				GlStateManager._glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, this.indexBufferId);
				GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER, 0, 0, this.totalIndices * 4);
				GlStateManager._glBindBuffer(GL31.GL_COPY_READ_BUFFER, 0);
				GlStateManager._glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, 0);
			}
		}
	}
	
	public void setScroll(float x, float y, float z)
	{
		this.scrollX = x;
		this.scrollY = y;
		this.scrollZ = z;
	}
	
	protected void generateChunk(int lodLevel, int lodScale, int x, int y, int z, float offsetX, float offsetY, float offsetZ, float scale, float camOffsetX, float camOffsetZ, int noOcclusionDirectionIndex)
	{
		this.shader.forUniform("LodLevel", (id, loc) -> {
			GL41.glProgramUniform1i(id, loc, lodLevel);
		});
		this.shader.forUniform("RenderOffset", (id, loc) -> {
			GL41.glProgramUniform3f(id, loc, offsetX / scale + camOffsetX / scale, offsetY / scale, offsetZ / scale + camOffsetZ / scale);
		});
		this.shader.forUniform("Scale", (id, loc) -> {
			GL41.glProgramUniform1f(id, loc, lodScale);
		});
		this.shader.forUniform("DoNotOccludeSide", (id, loc) -> {
			GL41.glProgramUniform1i(id, loc, noOcclusionDirectionIndex);
		});
		this.shader.dispatch(WORK_SIZE, WORK_SIZE, WORK_SIZE, false);
	}
	
	protected void doMeshGenning(double camX, double camY, double camZ, float scale, int tasksPerTick)
	{
		this.shader.forUniform("Scroll", (id, loc) -> {
			GL41.glProgramUniform3f(id, loc, this.scrollX, this.scrollY, this.scrollZ);
		});
		this.shader.forUniform("Origin", (id, loc) -> {
			GL41.glProgramUniform3f(id, loc, (float)camX / scale, (float)camY / scale, (float)camZ / scale);
		});
		this.shader.forUniform("TestFacesFacingAway", (id, loc) -> {
			GL41.glProgramUniform1i(id, loc, this.testFacesFacingAway ? 1 : 0);
		});
		
		for (int i = 0; i < tasksPerTick; i++)
		{
			Runnable task = this.chunkGenTasks.poll();
			if (task != null)
				task.run();
			else
				break;
		}
	}
	
	public void generateMesh(float scale)
	{
		RenderSystem.assertOnRenderThread();
		
		if (this.shader == null || !this.shader.isValid())
			return;
		
		if (this.lodConfigWasChanged)
		{
			this.onLodConfigChanged();
			this.lodConfigWasChanged = false;
		}
		
		this.populateChunkGenTasks(0.0D, 0.0D, 0.0D, scale, null, 1);
		
		if (!this.chunkGenTasks.isEmpty())
			this.doMeshGenning(0.0D, 0.0D, 0.0D, scale, this.chunkGenTasks.size());
		
		MutableInt totalSides = new MutableInt();
		this.shader.getShaderStorageBuffer("Counter").readWriteData(b -> {
			totalSides.setValue(b.getInt(0));
			b.putInt(0, 0);
		}, 4);
		this.copyDataOver(totalSides.getValue());
	}
	
	public void tick(double camX, double camY, double camZ, float scale, @Nullable Frustum frustum)
	{
		RenderSystem.assertOnRenderThread();
		
		if (this.shader == null || !this.shader.isValid())
			return;
		
		if (this.chunkGenTasks.isEmpty())
		{
			MutableInt totalSides = new MutableInt();
			this.shader.getShaderStorageBuffer("Counter").readWriteData(b -> {
				totalSides.setValue(b.getInt(0));
				b.putInt(0, 0);
			}, 4);
			this.copyDataOver(totalSides.getValue());
			
			if (this.lodConfigWasChanged)
			{
				this.onLodConfigChanged();
				this.lodConfigWasChanged = false;
			}
			this.tasksPerTick = this.populateChunkGenTasks(camX, camY, camZ, scale, frustum, this.meshGenInterval);
			this.currentCamX = camX;
			this.currentCamY = camY;
			this.currentCamZ = camZ;
			this.currentScale = scale;
		}
		else
		{
			this.shader.getShaderStorageBuffer("Counter").readWriteData(b -> {}, 4);
		}
		
		if (!this.chunkGenTasks.isEmpty())
			this.doMeshGenning(this.currentCamX, this.currentCamY, this.currentCamZ, this.currentScale, this.tasksPerTick);
	}
	
	protected void onLodConfigChanged()
	{
		this.shader.forUniform("TotalLodLevels", (id, loc) -> {
			GL41.glProgramUniform1i(id, loc, this.lodConfig.getLods().length);
		});
	}
	
	protected int populateChunkGenTasks(double camX, double camY, double camZ, float scale, @Nullable Frustum frustum, int genInterval)
	{
		int chunkCount = 0;
		float chunkSizeUpscaled = (float)SimpleCloudsConstants.CHUNK_SIZE * scale;
		float globalOffsetX = ((float)Mth.floor(camX / chunkSizeUpscaled) * chunkSizeUpscaled);
		float globalOffsetZ = ((float)Mth.floor(camZ / chunkSizeUpscaled) * chunkSizeUpscaled);
		for (CloudMeshGenerator.PreparedChunk chunk : this.lodConfig.preparedChunks)
		{
			if (chunk.checkIfVisibleAndQueue(this, camX, camY, camZ, scale, globalOffsetX, globalOffsetZ, frustum))
				chunkCount++;
		}
		return Mth.ceil((float)chunkCount / (float)genInterval);
	}
	
	public void render(PoseStack stack, Matrix4f projMat, float partialTick, float r, float g, float b)
	{
		RenderSystem.assertOnRenderThread();
	
		if (this.arrayObjectId != -1 && this.totalIndices > 0)
		{
			BufferUploader.reset();
			
			RenderSystem.disableBlend();
			RenderSystem.enableDepthTest();
			RenderSystem.setShaderColor(r, g, b, 1.0F);
			
			GL30.glBindVertexArray(this.arrayObjectId);
			
			RenderSystem.setShader(SimpleCloudsShaders::getCloudsShader);
			
			SimpleCloudsRenderer.setupCloudShaderLights();
			RenderSystem.getShader().setDefaultUniforms(VertexFormat.Mode.QUADS, stack.last().pose(), projMat, Minecraft.getInstance().getWindow());
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
	
	public int getSideBufferSize()
	{
		return this.sideBufferSize;
	}
	
	public int getIndexBufferSize()
	{
		return this.indexBufferSize;
	}
	
	public void fillReport(CrashReportCategory category)
	{
		category.setDetail("Compute Shader", this.shader);
		category.setDetail("Level Of Details", 1 + this.lodConfig.lods.length);
		category.setDetail("Generation Frame Interval", this.meshGenInterval);
		category.setDetail("Total Prepared Chunks", this.lodConfig.preparedChunks.size());
		category.setDetail("Tasks Per Frame", this.tasksPerTick);
		category.setDetail("Scroll", String.format("X: %s, Y: %s, Z: %s", this.scrollX, this.scrollY, this.scrollZ));
		category.setDetail("Array Object ID", this.arrayObjectId);
		category.setDetail("Vertex Buffer ID", this.vertexBufferId);
		category.setDetail("Index Buffer ID", this.indexBufferId);
		category.setDetail("Total Indices", this.totalIndices);
		category.setDetail("Total Triangles", this.totalSides * 2);
		category.setDetail("Test Occluded Faces", this.testFacesFacingAway);
	}
	
	@Override
	public String toString()
	{
		return String.format("%s[shader_name=%s]", this.getClass().getSimpleName(), this.meshShaderLoc);
	}
	
	public static class LevelOfDetailConfig
	{
		private final CloudMeshGenerator.LevelOfDetail[] lods;
		private final int primaryChunkSpan;
		private final int effectiveChunkSpan;
		private List<CloudMeshGenerator.PreparedChunk> preparedChunks;
		private int primaryChunkCount;
		
		public LevelOfDetailConfig(int primaryChunkSpan, CloudMeshGenerator.LevelOfDetail... lods)
		{
			this.primaryChunkSpan = primaryChunkSpan;
			this.lods = lods;
			int radius = primaryChunkSpan / 2;
			for (var lod : this.lods)
				radius += lod.chunkScale() * lod.spread();
			this.effectiveChunkSpan = radius * 2;
			this.prepareChunks();
		}
		
		private void prepareChunks()
		{
			ImmutableList.Builder<CloudMeshGenerator.PreparedChunk> builder = ImmutableList.builder();
			
			int currentRadius = this.primaryChunkSpan / 2;
			int primaryChunkCount = 0;
			for (int r = 0; r <= currentRadius; r++)
			{
				for (int y = 0; y < VERTICAL_CHUNK_SPAN; y++)
				{
					for (int x = -r; x < r; x++)
					{
						builder.add(new CloudMeshGenerator.PreparedChunk(0, 1, x, y, -r, -1));
						builder.add(new CloudMeshGenerator.PreparedChunk(0, 1, x, y, r - 1, -1));
						primaryChunkCount += 2;
					}
					for (int z = -r + 1; z < r - 1; z++)
					{
						builder.add(new CloudMeshGenerator.PreparedChunk(0, 1, -r, y, z, -1));
						builder.add(new CloudMeshGenerator.PreparedChunk(0, 1, r - 1, y, z, -1));
						primaryChunkCount += 2;
					}
				}
			}
			
			for (int i = 0; i < this.lods.length; i++)
			{
				CloudMeshGenerator.LevelOfDetail config = this.lods[i];
				int chunkCount = 0;
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
							builder.add(new CloudMeshGenerator.PreparedChunk(lodLevel, config.chunkScale(), x, y, -r, noOcclusion ? 5 : -1));
							builder.add(new CloudMeshGenerator.PreparedChunk(lodLevel, config.chunkScale(), x, y, r - 1, noOcclusion ? 4 : -1));
							chunkCount += 2;
						}
						for (int z = -r + 1; z < r - 1; z++)
						{
							builder.add(new CloudMeshGenerator.PreparedChunk(lodLevel, config.chunkScale(), -r, y, z, noOcclusion ? 1 : -1));
							builder.add(new CloudMeshGenerator.PreparedChunk(lodLevel, config.chunkScale(), r - 1, y, z, noOcclusion ? 0 : -1));
							chunkCount += 2;
						}
					}
				}
				currentRadius = currentRadius + config.spread() * config.chunkScale();
				config.setChunkCount(chunkCount);
			}
			
			this.primaryChunkCount = primaryChunkCount;
			this.preparedChunks = builder.build();
		}
		
		public CloudMeshGenerator.LevelOfDetail[] getLods()
		{
			return this.lods;
		}
		
		public int getPrimaryChunkSpan()
		{
			return this.primaryChunkSpan;
		}
		
		public int getEffectiveChunkSpan()
		{
			return this.effectiveChunkSpan;
		}
		
		public int getPrimaryChunkCount()
		{
			return this.primaryChunkCount;
		}
	}
	
	public static class LevelOfDetail
	{
		private final int chunkScale;
		private final int spread;
		private int chunkCount;
		
		public LevelOfDetail(int chunkScale, int spread)
		{
			this.chunkScale = chunkScale;
			this.spread = spread;
		}
		
		private void setChunkCount(int count)
		{
			this.chunkCount = count;
		}
		
		public int chunkScale()
		{
			return this.chunkScale;
		}
		
		public int spread()
		{
			return this.spread;
		}
		
		public int chunkCount()
		{
			return this.chunkCount;
		}
	}
	
	static class PreparedChunk
	{
		final int lodLevel;
		final int lodScale;
		final int x;
		final int y;
		final int z;
		final int noOcclusionDirectionIndex;
		
		PreparedChunk(int lodLevel, int lodScale, int x, int y, int z, int noOcclusionDirectionIndex)
		{
			this.lodLevel = lodLevel;
			this.lodScale = lodScale;
			this.x = x;
			this.y = y;
			this.z = z;
			this.noOcclusionDirectionIndex = noOcclusionDirectionIndex;
		}
		
		boolean checkIfVisibleAndQueue(CloudMeshGenerator generator, double camX, double camY, double camZ, float scale, float globalOffsetX, float globalOffsetZ, @Nullable Frustum frustum)
		{
			float chunkSizeLod = (float)SimpleCloudsConstants.CHUNK_SIZE * scale * this.lodScale;
			float offsetX = (float)this.x * chunkSizeLod;
			float offsetY = (float)this.y * chunkSizeLod;
			float offsetZ = (float)this.z * chunkSizeLod;
			AABB box = new AABB(offsetX, offsetY, offsetZ, offsetX + chunkSizeLod, offsetY+ chunkSizeLod, offsetZ + chunkSizeLod).inflate(0.0D, 500.0D, 0.0D).move(globalOffsetX, 0.0F, globalOffsetZ).move(-camX, -camY, -camZ);
			if (frustum == null || frustum.isVisible(box))
			{
				double nearestCornerX = Math.max(Math.max(box.minX, -box.maxX), 0.0D);
				double nearestCornerZ = Math.max(Math.max(box.minZ, -box.maxZ), 0.0D);
				double dist =  Math.sqrt(nearestCornerX * nearestCornerX + nearestCornerZ * nearestCornerZ);
				if (generator.cullDistance <= 0.0F || dist < generator.cullDistance)
				{
					generator.chunkGenTasks.add(() -> generator.generateChunk(this.lodLevel, this.lodScale, this.x, this.y, this.z, offsetX, offsetY, offsetZ, scale, globalOffsetX, globalOffsetZ, this.noOcclusionDirectionIndex));
					return true;
				}
			}
			return false;
		}
	}
}
