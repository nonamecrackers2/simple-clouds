package dev.nonamecrackers2.simpleclouds.client.mesh;

import java.awt.Color;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.mesh.chunk.MeshChunk;
import dev.nonamecrackers2.simpleclouds.client.mesh.lod.LevelOfDetailConfig;
import dev.nonamecrackers2.simpleclouds.client.mesh.lod.PreparedChunk;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.shader.SimpleCloudsShaders;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.ComputeShader;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.ShaderStorageBufferObject;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.mixin.MixinFrustumAccessor;
import net.minecraft.CrashReportCategory;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * Abstract mesh generator class that generates a cloud vertex mesh using computer shaders. Implementations are only available on the render thread.
 * <p><p>
 * Use {@link CloudMeshGenerator#init} to initialize the mesh generator. <b>This will initialize all needed buffers</b>. Note
 * that this is an expensive class and having multiple instances in one environment can cause GPU memory to run out quick (including
 * available SSBO bindings).
 * <p><p>
 * Use {@link CloudMeshGenerator#tick} each frame to generate the mesh at a fixed interval of frames 
 * (defined by {@link CloudMeshGenerator#setMeshGenInterval}) or use {@link CloudMeshGenerator#generateMesh} to generate
 * it in a single call.
 * <p><p>
 * Use {@link CloudMeshGenerator#render} to render the currently generated cloud mesh.
 * 
 * @author nonamecrackers2
 */
public abstract class CloudMeshGenerator
{
	public static final int MAX_NOISE_LAYERS = 4;
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/CloudMeshGenerator");
	public static final ResourceLocation MAIN_CUBE_MESH_GENERATOR = SimpleCloudsMod.id("cube_mesh");
	public static final int VERTICAL_CHUNK_SPAN = 8;
	public static final int LOCAL_SIZE = 8;
	public static final int WORK_SIZE = SimpleCloudsConstants.CHUNK_SIZE / LOCAL_SIZE;
	public static final int MAX_VERTEX_BUFFER_SIZE = 335544320;
	public static final int BYTES_PER_VERTEX = 20;
	public static final int BYTES_PER_SIDE = BYTES_PER_VERTEX * 4;
	public static final int MAX_INDEX_BUFFER_SIZE = 100663296;
	public static final String VERTEX_BUFFER_NAME = "VertexBuffer";
	public static final String INDEX_BUFFER_NAME = "IndexBuffer";
	public static final String TOTAL_SIDES_NAME = "TotalSides";
	public static final String SIDES_PER_CHUNK_NAME = "SidesPerChunk";
	public static final String NOISE_LAYERS_NAME = "NoiseLayers";
	public static final String LAYER_GROUPINGS_NAME = "LayerGroupings";
	protected final ResourceLocation meshShaderLoc;
	protected final LevelOfDetailConfig lodConfig;
	protected @Nullable List<MeshChunk> chunks;
	protected final List<CloudMeshGenerator.ChunkGenTask> completedGenTasks = Lists.newArrayList();
	protected final Queue<CloudMeshGenerator.ChunkGenTask> chunkGenTasks = Queues.newArrayDeque();
	protected int meshGenInterval;
	protected int tasksPerTick;
	protected @Nullable ComputeShader shader;
	protected CloudMeshGenerator.MeshGenResult meshGenResult = CloudMeshGenerator.MeshGenResult.NOT_INITIALIZED;
	protected float scrollX;
	protected float scrollY;
	protected float scrollZ;
	protected int totalIndices;
	protected int totalSides;
	protected boolean testFacesFacingAway;
	private float cullDistance;
	private int vertexBufferSize;
	private int indexBufferSize;
	
	/**
	 * Creates a cloud mesh generator, <b>but does not initialize it for generating</b> (use {@link CloudMeshGenerator#init})
	 * 
	 * @param meshShaderLoc
	 * The location of the cloud mesh generator compute shader
	 * @param lodConfig
	 * A level of detail configuration
	 * @param meshGenInterval
	 * The frame interval at which the generate the cloud mesh
	 */
	public CloudMeshGenerator(ResourceLocation meshShaderLoc, LevelOfDetailConfig lodConfig, int meshGenInterval)
	{
		this.meshShaderLoc = meshShaderLoc;
		this.lodConfig = lodConfig;
		this.setMeshGenInterval(meshGenInterval);
	}
	
	public LevelOfDetailConfig getLodConfig()
	{
		return this.lodConfig;
	}
	
	/**
	 * Sets the frame interval at which to generate the cloud mesh by when using
	 * {@link CloudMeshGenerator#tick}.
	 * <p><p>
	 * The mesh gen interval will spread out the amount of chunks to generate
	 * meshes for across the amount of frames specified by interval evenly. This
	 * decreases the load on the GPU and can improve performance at higher numbers,
	 * at a cost of some stuttery-ness in the way the clouds update.
	 * 
	 * @param interval
	 */
	public void setMeshGenInterval(int interval)
	{
		if (interval <= 0)
			throw new IllegalArgumentException("Please input a mesh gen interval greater than 0");
		this.meshGenInterval = interval;
	}
	
	/**
	 * Specifies if faces not facing the camera should be tested during
	 * mesh generation on the GPU for whether they should be generated or not.
	 * <p><p>
	 * Enabling can improve performance at the cost of some visual artifacts
	 * or an incomplete cloud mesh
	 * 
	 * @param flag
	 * @return
	 */
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
	
	public void setScroll(float x, float y, float z)
	{
		this.scrollX = x;
		this.scrollY = y;
		this.scrollZ = z;
	}
	
	public int getTotalIndices()
	{
		return this.totalIndices;
	}
	
	public int getTotalSides()
	{
		return this.totalSides;
	}
	
	public int getVertexBufferSize()
	{
		return this.vertexBufferSize;
	}
	
	public int getIndexBufferSize()
	{
		return this.indexBufferSize;
	}
	
	public CloudMeshGenerator.MeshGenResult getMeshGenResult()
	{
		return this.meshGenResult;
	}
	
	public void close()
	{
		RenderSystem.assertOnRenderThreadOrInit();
		
		GL42.glMemoryBarrier(GL42.GL_ALL_BARRIER_BITS);
		this.chunkGenTasks.clear();
		this.completedGenTasks.clear();
		
		this.totalIndices = 0;
		this.totalSides = 0;
		
		if (this.shader != null)
			this.shader.close();
		this.shader = null;
		
		if (this.chunks != null)
		{
			for (MeshChunk chunk : this.chunks)
				chunk.destroy();
			this.chunks = null;
		}
	}
	
	public final RendererInitializeResult init(ResourceManager manager)
	{
		RendererInitializeResult.Builder builder = RendererInitializeResult.builder();
				
		if (!RenderSystem.isOnRenderThreadOrInit())
			return builder.errorUnknown(new IllegalStateException("Init not called on render thread"), "Mesh Generator; Head").build();
		
		GL42.glMemoryBarrier(GL42.GL_ALL_BARRIER_BITS);
		this.chunkGenTasks.clear();
		this.completedGenTasks.clear();
		
		LOGGER.debug("Beginning mesh generator initialization");
		
		this.totalIndices = 0;
		this.totalSides = 0;
		
		if (this.shader != null)
		{
			LOGGER.debug("Freeing mesh compute shader");
			this.shader.close();
			this.shader = null;
		}
		
		if (this.chunks != null)
		{
			for (MeshChunk chunk : this.chunks)
				chunk.destroy();
			this.chunks = null;
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
		}
		catch (IOException e)
		{
			//LOGGER.warn("Failed to load compute shader", e);
			builder.errorCouldNotLoadMeshScript(e, "Mesh Generator; Compute Shader");
		}
		catch (Exception e)
		{
			builder.errorRecommendations(e, "Mesh Generator; Compute Shader");
		}
		
		List<PreparedChunk> preparedChunks = this.getLodConfig().getPreparedChunks();
		ImmutableList.Builder<MeshChunk> meshChunks = ImmutableList.builder();
		int totalPreparedChunks = preparedChunks.size();
		int vertexBufferSizePerChunk = Mth.floor(this.vertexBufferSize / totalPreparedChunks);
		int indexBufferSizePerChunk = Mth.floor(this.indexBufferSize / totalPreparedChunks);
		for (PreparedChunk chunk : preparedChunks)
			meshChunks.add(new MeshChunk(chunk, vertexBufferSizePerChunk, indexBufferSizePerChunk));
		this.chunks = meshChunks.build();
		LOGGER.debug("Initialized {} chunk mesh buffers", this.chunks.size());
		
		LOGGER.debug("Created VBA with vertex buffer size {} and index buffer size {}", this.vertexBufferSize, this.indexBufferSize);
		
		ComputeShader.printDebug();
		
		LOGGER.debug("Finished initializing mesh generator");
		
		return builder.build();
	}
	
	protected ComputeShader createShader(ResourceManager manager) throws IOException
	{
		return ComputeShader.loadShader(this.meshShaderLoc, manager, LOCAL_SIZE, LOCAL_SIZE, LOCAL_SIZE);
	}
	
	protected void setupShader()
	{
		ShaderStorageBufferObject totalSidesBuffer = this.shader.bindShaderStorageBuffer(TOTAL_SIDES_NAME, GL15.GL_DYNAMIC_COPY);
		totalSidesBuffer.allocateBuffer(4);
		totalSidesBuffer.writeData(b -> {
			b.putInt(0, 0);
		}, 4);
		
		this.vertexBufferSize = this.shader.bindShaderStorageBuffer(VERTEX_BUFFER_NAME, GL15.GL_DYNAMIC_COPY).allocateBuffer(MAX_VERTEX_BUFFER_SIZE); //Vertex data, arbitrary size
		this.indexBufferSize = this.shader.bindShaderStorageBuffer(INDEX_BUFFER_NAME, GL15.GL_DYNAMIC_COPY).allocateBuffer(MAX_INDEX_BUFFER_SIZE); //Index data, arbitrary size
		
		ShaderStorageBufferObject sidesPerChunkBuffer = this.shader.bindShaderStorageBuffer(SIDES_PER_CHUNK_NAME, GL15.GL_DYNAMIC_COPY);
		int bufferSize = this.getLodConfig().getPreparedChunks().size() * 4;
		sidesPerChunkBuffer.allocateBuffer(bufferSize);
		sidesPerChunkBuffer.writeData(b -> {
			for (int i = 0; i < this.getLodConfig().getPreparedChunks().size(); i++)
				b.putInt(0);
			b.rewind();
		}, bufferSize);
		
		this.shader.forUniform("TotalLodLevels", (id, loc) -> {
			GL41.glProgramUniform1i(id, loc, this.lodConfig.getLods().length);
		});
	}
	
	protected void initExtra(ResourceManager manager) {}
	
	/**
	 * Generates the entire cloud mesh at the origin at once
	 */
	public void generateMesh()
	{
		RenderSystem.assertOnRenderThread();
		
		if (this.shader == null || !this.shader.isValid())
			return;
		
		this.prepareMeshGen(0.0D, 0.0D, 0.0D, 0.0F, 0.0F, null, 1);
		
		if (!this.chunkGenTasks.isEmpty())
			this.doMeshGenning(this.chunkGenTasks.size());
		
		this.shader.getShaderStorageBuffer(TOTAL_SIDES_NAME).readWriteData(b -> 
		{
			this.totalSides = b.getInt(0);
			this.totalIndices = this.totalSides * 6;
			b.putInt(0, 0);
		}, 4);
		
		this.meshGenResult = this.finalizeMeshGen();
		this.completedGenTasks.clear();
	}
	
	/**
	 * Generates the cloud mesh on a per-frame basis
	 * 
	 * @param originX
	 * @param originY
	 * @param originZ
	 * @param frustum
	 */
	public void tick(double originX, double originY, double originZ, @Nullable Frustum frustum)
	{
		RenderSystem.assertOnRenderThread();
		
		if (this.shader == null || !this.shader.isValid())
			return;

		float chunkSize = (float)SimpleCloudsConstants.CHUNK_SIZE;
		float meshGenOffsetX = (float)Mth.floor(originX / chunkSize) * chunkSize;
		float meshGenOffsetZ = (float)Mth.floor(originZ / chunkSize) * chunkSize;
		
		if (this.chunkGenTasks.isEmpty()) //If we have no chunk gen tasks
		{
			//Read the amount of sides from the compute shader
			this.shader.getShaderStorageBuffer(TOTAL_SIDES_NAME).readWriteData(b -> 
			{
				this.totalSides = b.getInt(0);
				this.totalIndices = this.totalSides * 6;
				b.putInt(0, 0);
			}, 4);
			this.meshGenResult = this.finalizeMeshGen(); //Split the combined mesh data from the GPU, and store them in the VBOs for each chunk that was generated
			this.completedGenTasks.clear(); //Clear the chunk gen tasks
			
			//Prepare the next batch of chunks to generate meshes for
			this.tasksPerTick = this.prepareMeshGen(originX, originY, originZ, meshGenOffsetX, meshGenOffsetZ, frustum, this.meshGenInterval);
		}
		else
		{
			//We read the counter here to avoid weird frame spikes when in fullscreen V-Sync, not sure why it happens
			this.shader.getShaderStorageBuffer(TOTAL_SIDES_NAME).readWriteData(b -> {}, 4);
		}
		
		//If there are mesh gen tasks, we do mesh genning
		if (!this.chunkGenTasks.isEmpty())
			this.doMeshGenning(this.tasksPerTick);
	}
	
	private static CloudMeshGenerator.MeshGenResult iterateAndCopyToChunkBuffer(int copyBufferId, int copyBufferSizeBytes, Collection<MeshChunk> chunks, Function<MeshChunk, Integer> chunkBufferId, Function<MeshChunk, Integer> bytesToCopyPerChunk, Function<MeshChunk, Integer> bufferSizeBytesPerChunk)
	{
		CloudMeshGenerator.MeshGenResult result = CloudMeshGenerator.MeshGenResult.NORMAL;
		
		GlStateManager._glBindBuffer(GL31.GL_COPY_READ_BUFFER, copyBufferId);
		
		int currentBytes = 0;
		for (MeshChunk chunk : chunks)
		{
			int totalBytes = bytesToCopyPerChunk.apply(chunk);
			if (totalBytes > 0) //If the chunk has data that needs copying over
			{
				int lastBytesOffset = totalBytes;
				int maxSize = bufferSizeBytesPerChunk.apply(chunk);
				if (lastBytesOffset > maxSize) //Make sure we don't go over the maximum the chunk buffer can hold
				{
					lastBytesOffset = maxSize;
					result = CloudMeshGenerator.MeshGenResult.CHUNK_OVERFLOW;
				}
				boolean stop = false;
				if (currentBytes + lastBytesOffset > copyBufferSizeBytes) //If the the byte offset will go over the size of the copy buffer, clamp
				{
					lastBytesOffset = copyBufferSizeBytes - currentBytes;
					if (lastBytesOffset <= 0) // If it becomes negative however, we don't want to attempt to copy data over
						return CloudMeshGenerator.MeshGenResult.TOO_MANY_VERTICES;
					stop = true; // After copying this data over we will stop, since there is no more space in the copy buffer to read data from
				}
				
				GlStateManager._glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, chunkBufferId.apply(chunk));
				GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER, currentBytes, 0, lastBytesOffset);
				
				currentBytes += totalBytes;
				
				if (stop)
					return CloudMeshGenerator.MeshGenResult.TOO_MANY_VERTICES;
			}
		}
		
		return result;
	}
	
	protected CloudMeshGenerator.MeshGenResult finalizeMeshGen()
	{
		if (this.shader == null || !this.shader.isValid() || this.chunks == null)
			return CloudMeshGenerator.MeshGenResult.NOT_INITIALIZED;
		
		if (this.completedGenTasks.isEmpty())
			return CloudMeshGenerator.MeshGenResult.NO_TASKS;
		
		CloudMeshGenerator.MeshGenResult result = CloudMeshGenerator.MeshGenResult.NORMAL;
		
		RenderSystem.assertOnRenderThread();
		GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
			
		//Get the amount of total sides each chunk has and reset each counter
		this.shader.getShaderStorageBuffer(SIDES_PER_CHUNK_NAME).readWriteData(buffer -> 
		{
			for (CloudMeshGenerator.ChunkGenTask gennedChunk : this.completedGenTasks)
			{
				int index = gennedChunk.index() * 4;
				int sideCount = buffer.getInt(index);
				gennedChunk.chunk().setTotalSides(sideCount);
				buffer.putInt(index, 0);
			}
		}, this.chunks.size() * 4);
		
		List<MeshChunk> completedChunks = this.completedGenTasks.stream().map(CloudMeshGenerator.ChunkGenTask::chunk).toList();
		
		//Copy over the vertices from the main SSBO to each chunk's vertex buffer
		int vertexBufferId = this.shader.getShaderStorageBuffer(VERTEX_BUFFER_NAME).getId();
		result = iterateAndCopyToChunkBuffer(vertexBufferId, this.vertexBufferSize, completedChunks, MeshChunk::getVertexBufferId, c -> c.getTotalSides() * BYTES_PER_SIDE, MeshChunk::getVertexBufferSize);
		
		//Copy over the indices from the main SSBO to each chunk's index buffer
		int indexBufferId = this.shader.getShaderStorageBuffer(INDEX_BUFFER_NAME).getId();
		iterateAndCopyToChunkBuffer(indexBufferId, this.indexBufferSize, completedChunks, MeshChunk::getIndexBufferId, c -> c.getTotalIndices() * 4, MeshChunk::getIndexBufferSize);
		
		GlStateManager._glBindBuffer(GL31.GL_COPY_READ_BUFFER, 0);
		GlStateManager._glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, 0);
		
		return result;
	}
	
	/**
	 * Queues a list of chunk gen tasks for each chunk in this mesh generator
	 * 
	 * @param meshGenOffsetX
	 * @param meshGenOffsetZ
	 * @param frustum
	 * Culling frustum, null for no culling
	 * @param genInterval
	 * How many frames mesh genning should take
	 * @return
	 */
	protected int prepareMeshGen(double originX, double originY, double originZ, float meshGenOffsetX, float meshGenOffsetZ, @Nullable Frustum frustum, int genInterval)
	{
		this.shader.forUniform("Scroll", (id, loc) -> {
			GL41.glProgramUniform3f(id, loc, this.scrollX, this.scrollY, this.scrollZ);
		});
		this.shader.forUniform("Origin", (id, loc) -> {
			GL41.glProgramUniform3f(id, loc, (float)originX, (float)originY, (float)originZ);
		});
		this.shader.forUniform("TestFacesFacingAway", (id, loc) -> {
			GL41.glProgramUniform1i(id, loc, this.testFacesFacingAway ? 1 : 0);
		});
		
		int chunkCount = 0;
		for (int i = 0; i < this.chunks.size(); i++)
		{
			if (this.queueChunkMeshGenTask(this.chunks.get(i), i, meshGenOffsetX, meshGenOffsetZ, frustum))
				chunkCount++;
		}
		return Mth.ceil((float)chunkCount / (float)genInterval);
	}
	
	/**
	 * Queues a given chunk for mesh genning
	 * 
	 * @param chunk
	 * The given {@link MeshChunk} to generate a mesh for
	 * @param chunkIndex
	 * The index of the mesh chunk in {@code this.chunks}
	 * @param meshGenOffsetX
	 * @param meshGenOffsetZ
	 * @param frustum
	 * For frustum culling, null for no culling
	 * @return
	 */
	protected boolean queueChunkMeshGenTask(MeshChunk chunk, int chunkIndex, float meshGenOffsetX, float meshGenOffsetZ, @Nullable Frustum frustum)
	{
		PreparedChunk chunkInfo = chunk.getChunkInfo();
		AABB bounds = chunkInfo.bounds();
		float minX = (float)bounds.minX + meshGenOffsetX;
		float minZ = (float)bounds.minZ + meshGenOffsetZ;
		float maxX = (float)bounds.maxX + meshGenOffsetX;
		float maxZ = (float)bounds.maxZ + meshGenOffsetZ;
		
		if (frustum == null || ((MixinFrustumAccessor)frustum).simpleclouds$cubeInFrustum(minX, bounds.minY, minZ, maxX, bounds.maxY, maxZ))
		{
			double nearestCornerX = Math.max(Math.max(bounds.minX, -bounds.maxX), 0.0D);
			double nearestCornerZ = Math.max(Math.max(bounds.minZ, -bounds.maxZ), 0.0D);
			double dist =  Math.sqrt(nearestCornerX * nearestCornerX + nearestCornerZ * nearestCornerZ);
			
			if (this.cullDistance <= 0.0F || dist < this.cullDistance)
			{
				Pair<Integer, Integer> minMaxHeights = this.determineMinimumAndMaximimumGenHeightsAt(minX, minZ, maxX, maxZ);
				this.chunkGenTasks.add(new CloudMeshGenerator.ChunkGenTask(chunk, minX, (float)bounds.minY, minZ, maxX, (float)bounds.maxY, maxZ, chunkIndex, minX, 0.0F, minZ, minMaxHeights.getLeft(), minMaxHeights.getRight()));
				return true;
			}
		}
		return false;
	}
	
	protected abstract Pair<Integer, Integer> determineMinimumAndMaximimumGenHeightsAt(float minX, float minZ, float maxX, float maxZ);
	
	/**
	 * Does mesh generating for a given amount of chunks defined by tasksPerTick
	 * 
	 * @param tasksPerTick
	 */
	protected void doMeshGenning(int tasksPerTick)
	{
		for (int i = 0; i < tasksPerTick; i++)
		{
			CloudMeshGenerator.ChunkGenTask task = this.chunkGenTasks.poll();
			if (task != null)
			{
				this.generateChunk(task);
				this.updateMeshChunkAfterGeneration(task.chunk(), task);
				this.completedGenTasks.add(task);
			}
			else
			{
				break;
			}
		}
	}
	
	protected void updateMeshChunkAfterGeneration(MeshChunk chunk, CloudMeshGenerator.ChunkGenTask task)
	{
		chunk.setBounds(task.minX(), task.minY(), task.minZ(), task.maxX(), task.maxY(), task.maxZ());
		chunk.setHeights(task.startY(), task.endY());
	}
	
	/**
	 * Generates a given chunk, or completes a chunk gen task
	 * 
	 * @param task
	 * @param scale
	 * @param globalOffsetX
	 * @param globalOffsetZ
	 */
	protected void generateChunk(CloudMeshGenerator.ChunkGenTask task)
	{
		PreparedChunk chunkInfo = task.chunk().getChunkInfo();
		
		int lodScale = chunkInfo.lodScale();
		int lowestY = task.startY() / lodScale;
		int height = (task.endY() / lodScale) - lowestY;
		int localHeightInvocations = Mth.ceil((float)height / (float)WORK_SIZE);
		
		if (localHeightInvocations > 0)
		{
			this.shader.forUniform("ChunkIndex", (id, loc) -> {
				GL41.glProgramUniform1i(id, loc, task.index());
			});
			this.shader.forUniform("LodLevel", (id, loc) -> {
				GL41.glProgramUniform1i(id, loc, chunkInfo.lodLevel());
			});
			this.shader.forUniform("RenderOffset", (id, loc) -> {
				GL41.glProgramUniform3f(id, loc, task.x(), task.y() + lowestY, task.z());
			});
			this.shader.forUniform("Scale", (id, loc) -> {
				GL41.glProgramUniform1f(id, loc, lodScale);
			});
			this.shader.forUniform("DoNotOccludeSide", (id, loc) -> {
				GL41.glProgramUniform1i(id, loc, chunkInfo.noOcclusionDirectionIndex());
			});
			
			this.shader.dispatch(WORK_SIZE, localHeightInvocations, WORK_SIZE, false);
			GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
		}
	}
	
	/**
	 * Renders the currently generated cloud mesh using the cloud shader, and sets other relevant GL states
	 * <p><p>
	 * This method is safe to call in conjunction with {@link CloudMeshGenerator#tick}. What is currently
	 * being generated will not affect what is to be rendered.
	 * 
	 * @param stack
	 * The transformation matrix to render the clouds with
	 * @param projMat
	 * The projection matrix to render with
	 * @param partialTick
	 * @param r
	 * @param g
	 * @param b
	 * @param frustum
	 * Culling frustum, or null for no culling
	 */
	public void render(PoseStack stack, Matrix4f projMat, float partialTick, float r, float g, float b, @Nullable Frustum frustum)
	{
		RenderSystem.assertOnRenderThread();
	
		if (this.chunks != null)
		{
			BufferUploader.reset();
			
			RenderSystem.disableBlend();
			RenderSystem.enableDepthTest();
			RenderSystem.setShaderColor(r, g, b, 1.0F);
			
			RenderSystem.setShader(SimpleCloudsShaders::getCloudsShader);
			SimpleCloudsRenderer.prepareShader(RenderSystem.getShader(), stack.last().pose(), projMat);
			RenderSystem.getShader().apply();
			
			this.forRenderableMeshChunks(frustum, chunk -> {
				GL30.glBindVertexArray(chunk.getArrayObjectId());
				RenderSystem.drawElements(GL11.GL_TRIANGLES, chunk.getTotalIndices(), GL11.GL_UNSIGNED_INT);
			});
			
			GL30.glBindVertexArray(0);
			
			RenderSystem.getShader().clear();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		}
	}
	
	public void renderDebug(PoseStack stack, Matrix4f projMat, float partialTick, @Nullable Frustum frustum, boolean chunkBoundaries, boolean noiseBoundaries)
	{
		RenderSystem.assertOnRenderThread();
		
		if (this.chunks != null)
		{
			BufferUploader.reset();
			
			RenderSystem.disableBlend();
			RenderSystem.enableDepthTest();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.disableCull();
			
			Tesselator tesselator = Tesselator.getInstance();
			BufferBuilder builder = tesselator.getBuilder();
			builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
			
			this.forRenderableMeshChunks(frustum, chunk -> 
			{
				PreparedChunk preparedChunk = chunk.getChunkInfo();
				if (chunkBoundaries)
				{
					int color = Color.HSBtoRGB((float)preparedChunk.lodLevel() / ((float)this.lodConfig.getLods().length + 1), 1.0F, 1.0F);
					float r = (float)FastColor.ARGB32.red(color) / 255.0F;
					float g = (float)FastColor.ARGB32.green(color) / 255.0F;
					float b = (float)FastColor.ARGB32.blue(color) / 255.0F;
					LevelRenderer.renderLineBox(builder, chunk.getBoundsMinX() + 1.0F, chunk.getBoundsMinY() + 1.0F, chunk.getBoundsMinZ() + 1.0F, chunk.getBoundsMaxX() - 1.0F, chunk.getBoundsMaxY() - 1.0F, chunk.getBoundsMaxZ() - 1.0F, r, g, b, 1.0F);
				}
				if (noiseBoundaries)
					LevelRenderer.renderLineBox(builder, chunk.getBoundsMinX() + 1.0F, chunk.getMinHeight() + 1.0F, chunk.getBoundsMinZ() + 1.0F, chunk.getBoundsMaxX() - 1.0F, chunk.getMaxHeight() - 1.0F, chunk.getBoundsMaxZ() - 1.0F, 1.0F, 1.0F, 0.0F, 1.0F);
			});
			
			RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
			ShaderInstance shader = RenderSystem.getShader();
			SimpleCloudsRenderer.prepareShader(shader, stack.last().pose(), projMat);
			shader.LINE_WIDTH.set(2.5F);
			shader.FOG_START.set(Float.MAX_VALUE);
			shader.apply();
			BufferUploader.draw(builder.end());
			shader.clear();
			
			RenderSystem.enableCull();
			
			RenderSystem.defaultBlendFunc();
			RenderSystem.enableBlend();
			
			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			
			this.forRenderableMeshChunks(frustum, chunk -> 
			{
				PreparedChunk preparedChunk = chunk.getChunkInfo();
				if (chunkBoundaries)
				{
					int color = Color.HSBtoRGB((float)preparedChunk.lodLevel() / ((float)this.lodConfig.getLods().length + 1), 1.0F, 1.0F);
					float r = (float)FastColor.ARGB32.red(color) / 255.0F;
					float g = (float)FastColor.ARGB32.green(color) / 255.0F;
					float b = (float)FastColor.ARGB32.blue(color) / 255.0F;
					renderChunkBox(builder, chunk.getBoundsMinX() + 1.0F, chunk.getBoundsMinY() + 1.0F, chunk.getBoundsMinZ() + 1.0F, chunk.getBoundsMaxX() - 1.0F, chunk.getBoundsMaxY() - 1.0F, chunk.getBoundsMaxZ() - 1.0F, r, g, b, 0.4F);
				}
				if (noiseBoundaries)
					renderChunkBox(builder, chunk.getBoundsMinX() + 1.0F, chunk.getMinHeight() + 1.0F, chunk.getBoundsMinZ() + 1.0F, chunk.getBoundsMaxX() - 1.0F, chunk.getMaxHeight() - 1.0F, chunk.getBoundsMaxZ() - 1.0F, 1.0F, 1.0F, 0.0F, 0.4F);
			});
			
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			shader = RenderSystem.getShader();
			SimpleCloudsRenderer.prepareShader(shader, stack.last().pose(), projMat);
			shader.apply();
			BufferUploader.draw(builder.end());
			shader.clear();
			
			RenderSystem.disableBlend();
		}
	}
	
	private static void renderChunkBox(VertexConsumer consumer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a)
	{
		//-X
		consumer.vertex(minX, minY, maxZ).color(r, g, b, a).endVertex();
		consumer.vertex(minX, maxY, maxZ).color(r, g, b, a).endVertex();
		consumer.vertex(minX, maxY, minZ).color(r, g, b, a).endVertex();
		consumer.vertex(minX, minY, minZ).color(r, g, b, a).endVertex();
		
		//+X
		consumer.vertex(maxX, minY, minZ).color(r, g, b, a).endVertex();
		consumer.vertex(maxX, maxY, minZ).color(r, g, b, a).endVertex();
		consumer.vertex(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
		consumer.vertex(maxX, minY, maxZ).color(r, g, b, a).endVertex();
		
		//-Y
		consumer.vertex(maxX, minY, minZ).color(r, g, b, a).endVertex();
		consumer.vertex(maxX, minY, maxZ).color(r, g, b, a).endVertex();
		consumer.vertex(minX, minY, maxZ).color(r, g, b, a).endVertex();
		consumer.vertex(minX, minY, minZ).color(r, g, b, a).endVertex();
		
		//+Y
		consumer.vertex(minX, maxY, minZ).color(r, g, b, a).endVertex();
		consumer.vertex(minX, maxY, maxZ).color(r, g, b, a).endVertex();
		consumer.vertex(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
		consumer.vertex(maxX, maxY, minZ).color(r, g, b, a).endVertex();
		
		//-Z
		consumer.vertex(minX, minY, minZ).color(r, g, b, a).endVertex();
		consumer.vertex(minX, maxY, minZ).color(r, g, b, a).endVertex();
		consumer.vertex(maxX, maxY, minZ).color(r, g, b, a).endVertex();
		consumer.vertex(maxX, minY, minZ).color(r, g, b, a).endVertex();
		
		//+Z
		consumer.vertex(maxX, minY, maxZ).color(r, g, b, a).endVertex();
		consumer.vertex(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
		consumer.vertex(minX, maxY, maxZ).color(r, g, b, a).endVertex();
		consumer.vertex(minX, minY, maxZ).color(r, g, b, a).endVertex();
	}
	
	public void forRenderableMeshChunks(@Nullable Frustum frustum, Consumer<MeshChunk> function)
	{
		for (MeshChunk chunk : this.chunks)
		{
			if (chunk.getTotalSides() > 0)
			{
				boolean render = true;
				if (frustum != null)
					render = ((MixinFrustumAccessor)frustum).simpleclouds$cubeInFrustum(chunk.getBoundsMinX(), chunk.getBoundsMinY(), chunk.getBoundsMinZ(), chunk.getBoundsMaxX(), chunk.getBoundsMaxY(), chunk.getBoundsMaxZ());
				if (render)
					function.accept(chunk);
			}
		}
	}
	
	public void fillReport(CrashReportCategory category)
	{
		category.setDetail("Compute Shader", this.shader);
		category.setDetail("Level Of Details", 1 + this.lodConfig.getLods().length);
		category.setDetail("Generation Frame Interval", this.meshGenInterval);
		category.setDetail("Total Prepared Chunks", this.lodConfig.getPreparedChunks().size());
		category.setDetail("Tasks Per Frame", this.tasksPerTick);
		category.setDetail("Scroll", String.format("X: %s, Y: %s, Z: %s", this.scrollX, this.scrollY, this.scrollZ));
		category.setDetail("Total Mesh Chunks", this.chunks != null ? this.chunks.size() : "null");
		category.setDetail("Mesh Gen Result", this.meshGenResult);
		category.setDetail("Total Triangles", this.totalSides * 2);
		category.setDetail("Total Indices", this.totalIndices);
		category.setDetail("Test Occluded Faces", this.testFacesFacingAway);
	}
	
	@Override
	public String toString()
	{
		return String.format("%s[shader_name=%s]", this.getClass().getSimpleName(), this.meshShaderLoc);
	}
	
	protected static record ChunkGenTask(MeshChunk chunk, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int index, float x, float y, float z, int startY, int endY) {}
	
	public static enum MeshGenResult
	{
		NOT_INITIALIZED,
		NO_TASKS,
		NORMAL,
		TOO_MANY_VERTICES,
		CHUNK_OVERFLOW;
	}
}
