package dev.nonamecrackers2.simpleclouds.client.mesh.generator;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.mesh.LevelOfDetailOptions;
import dev.nonamecrackers2.simpleclouds.client.mesh.RendererInitializeResult;
import dev.nonamecrackers2.simpleclouds.client.mesh.chunk.MeshChunk;
import dev.nonamecrackers2.simpleclouds.client.mesh.instancing.InstanceableMesh;
import dev.nonamecrackers2.simpleclouds.client.mesh.lod.LevelOfDetailConfig;
import dev.nonamecrackers2.simpleclouds.client.mesh.lod.PreparedChunk;
import dev.nonamecrackers2.simpleclouds.client.shader.buffer.BindingManager;
import dev.nonamecrackers2.simpleclouds.client.shader.buffer.ShaderStorageBufferObject;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.ComputeShader;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudInfo;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.mixin.MixinFrustumAccessor;
import net.minecraft.CrashReportCategory;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
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
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/CloudMeshGenerator");
	
	public static final ResourceLocation MAIN_CUBE_MESH_GENERATOR = SimpleCloudsMod.id("cube_mesh");
	public static final int MAX_NOISE_LAYERS = 4;
	public static final int VERTICAL_CHUNK_SPAN = 8;
	public static final int LOCAL_SIZE = 8;
	public static final int WORK_SIZE = SimpleCloudsConstants.CHUNK_SIZE / LOCAL_SIZE;
	public static final int TICKS_UNTIL_FADE_RESET = 120;
	
	//Opaque
	public static final int BYTES_PER_SIDE_INFO = 24;
	public static final int MAX_SIDE_INFO_BUFFER_SIZE = 50331648;
	public static final String SIDE_INFO_BUFFER_NAME = "SideInfoBuffer";
	public static final String TOTAL_SIDES_NAME = "TotalSides";
	public static final String SIDES_PER_CHUNK_NAME = "SidesPerChunk";
	//Transparent
	public static final int BYTES_PER_CUBE_INFO = 24;
	public static final int MAX_TRANSPARENT_CUBE_INFO_BUFFER_SIZE = 50331648;
	public static final String TRANSPARENT_CUBE_INFO_BUFFER_NAME = "TransparentCubeInfoBuffer";
	public static final String TRANSPARENT_TOTAL_CUBES_NAME = "TotalTransparentCubes";
	public static final String TRANSPARENT_CUBES_PER_CHUNK_NAME = "TransparentCubesPerChunk";
	
	public static final String NOISE_LAYERS_NAME = "NoiseLayers";
	public static final String LAYER_GROUPINGS_NAME = "LayerGroupings";
	
	protected final ResourceLocation meshShaderLoc;
	protected final int shaderType;
	protected final boolean fadeNearOrigin;
	protected final boolean shadedClouds;
	protected final boolean useTransparency;
	protected final LevelOfDetailConfig lodConfig;
	protected final boolean useFixedMeshDataSectionSize;
	protected @Nullable List<MeshChunk> chunks;
	protected final List<CloudMeshGenerator.ChunkGenTask> completedGenTasks = Lists.newArrayList();
	protected final Queue<CloudMeshGenerator.ChunkGenTask> chunkGenTasks = Queues.newArrayDeque();
	protected int meshGenInterval;
	protected int tasksPerTick;
	protected @Nullable ComputeShader shader;
	
	protected @Nullable InstanceableMesh sideMesh;
	protected @Nullable InstanceableMesh cubeMesh;
	
	// Left is for opaque geometry, right is for transparent
	protected Pair<CloudMeshGenerator.MeshGenStatus, CloudMeshGenerator.MeshGenStatus> meshGenStatus = Pair.of(CloudMeshGenerator.MeshGenStatus.NOT_INITIALIZED, CloudMeshGenerator.MeshGenStatus.NOT_INITIALIZED);
	protected float scrollX;
	protected float scrollY;
	protected float scrollZ;
	protected boolean testFacesFacingAway;
	private float fadeStart;
	private float fadeEnd;
	private float cullDistance;
	private int transparencyDistance;
	
	private int opaqueBufferSize;
	private int opaqueBufferBytesUsed;
	private int transparentBufferSize;
	private int transparentBufferBytesUsed;
	private int opaqueBytesPerChunk;
	private int transparentBytesPerChunk;
	
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
	public CloudMeshGenerator(ResourceLocation meshShaderLoc, int shaderType, boolean fadeNearOrigin, boolean shadedClouds, LevelOfDetailConfig lodConfig, int meshGenInterval, boolean useTransparency, boolean fixedMeshDataSectionSize)
	{
		this.meshShaderLoc = meshShaderLoc;
		this.shaderType = shaderType;
		this.fadeNearOrigin = fadeNearOrigin;
		this.shadedClouds = shadedClouds;
		this.useFixedMeshDataSectionSize = fixedMeshDataSectionSize;
		
		this.lodConfig = lodConfig;
		this.setMeshGenInterval(meshGenInterval);
		this.useTransparency = useTransparency;
		
		float maxRadius = this.getCloudAreaMaxRadius();
		this.fadeStart = 0.9F * maxRadius;
		this.fadeEnd = maxRadius;
		this.transparencyDistance = (int)maxRadius / 2;
	}
	
	public boolean fadeNearOriginEnabled()
	{
		return this.fadeNearOrigin;
	}
	
	public boolean shadedCloudsEnabled()
	{
		return this.shadedClouds;
	}
	
	public boolean transparencyEnabled()
	{
		return this.useTransparency;
	}
	
	public boolean usesFixedMeshDataSectionSize()
	{
		return this.useFixedMeshDataSectionSize;
	}
	
	public LevelOfDetailConfig getLodConfig()
	{
		return this.lodConfig;
	}
	
	/**
	 * Sets the frame interval at which to generate the cloud mesh by when using
	 * {@link CloudMeshGenerator#tick}.
	 * <p><p>
	 * Spreads out the amount of mesh gen compute dispatches
	 * across the amount of frames specified by this interval evenly. This
	 * decreases the load on the GPU and can improve performance at higher numbers,
	 * at a cost of some stuttery-ness in the way the clouds update as it will take
	 * longer for the entire mesh to generate.
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
	
	/**
	 * Sets the fade start and end distances as decimal percentages
	 * 
	 * @param fadeStart
	 * @param fadeEnd
	 */
	public CloudMeshGenerator setFadeDistances(float fadeStart, float fadeEnd)
	{
		float fs = fadeStart;
		float fe = fadeEnd;
		if (fs > fe)
		{
			fs = fadeEnd;
			fe = fadeStart;
		}
		this.fadeStart = fs * (float)this.getCloudAreaMaxRadius();
		this.fadeEnd = fe * (float)this.getCloudAreaMaxRadius();
		return this;
	}
	
	public CloudMeshGenerator setTransparencyRenderDistance(float percentage)
	{
		this.transparencyDistance = Mth.floor(percentage * (float)this.getCloudAreaMaxRadius());
		return this;
	}
	
	public float getFadeStart()
	{
		return this.fadeStart;
	}
	
	public float getFadeEnd()
	{
		return this.fadeEnd;
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
	
	public Pair<CloudMeshGenerator.MeshGenStatus, CloudMeshGenerator.MeshGenStatus> getMeshGenStatus()
	{
		return this.meshGenStatus;
	}
	
	public @Nullable InstanceableMesh getSideMesh()
	{
		return this.sideMesh;
	}
	
	public @Nullable InstanceableMesh getCubeMesh()
	{
		return this.cubeMesh;
	}
	
	public int getOpaqueBufferSize()
	{
		return this.opaqueBufferSize;
	}
	
	public int getOpaqueBufferBytesUsed()
	{
		return this.opaqueBufferBytesUsed;
	}
	
	public int getTransparentBufferSize()
	{
		return this.transparentBufferSize;
	}
	
	public int getTransparentBufferBytesUsed()
	{
		return this.transparentBufferBytesUsed;
	}
	
	public int getOpaqueBytesPerChunk()
	{
		return this.opaqueBytesPerChunk;
	}
	
	public int getTransparentBytesPerChunk()
	{
		return this.transparentBytesPerChunk;
	}
	
	public int getTotalMeshChunks()
	{
		if (this.chunks == null)
			return 0;
		return this.chunks.size();
	}
	
	public void close()
	{
		RenderSystem.assertOnRenderThreadOrInit();
		
		this.opaqueBufferBytesUsed = 0;
		this.opaqueBufferSize = 0;
		this.opaqueBytesPerChunk = 0;
		this.transparentBufferBytesUsed = 0;
		this.transparentBufferSize = 0;
		this.transparentBytesPerChunk = 0;
		
		GL42.glMemoryBarrier(GL42.GL_ALL_BARRIER_BITS);
		this.chunkGenTasks.clear();
		this.completedGenTasks.clear();
		
		if (this.shader != null)
			this.shader.close();
		this.shader = null;
		
		if (this.chunks != null)
		{
			for (MeshChunk chunk : this.chunks)
				chunk.destroy();
			this.chunks = null;
		}
		
		if (this.sideMesh != null)
		{
			this.sideMesh.destroy();
			this.sideMesh = null;
		}
		
		if (this.cubeMesh != null)
		{
			this.cubeMesh.destroy();
			this.cubeMesh = null;
		}
	}
	
	public boolean canRender()
	{
		return this.chunks != null;
	}
	
	public final RendererInitializeResult init(ResourceManager manager)
	{
		RendererInitializeResult.Builder builder = RendererInitializeResult.builder();
				
		if (!RenderSystem.isOnRenderThreadOrInit())
			return builder.errorUnknown(new IllegalStateException("Init not called on render thread"), "Mesh Generator; Head").build();
		
		this.opaqueBufferBytesUsed = 0;
		this.opaqueBufferSize = 0;
		this.opaqueBytesPerChunk = 0;
		this.transparentBufferBytesUsed = 0;
		this.transparentBufferSize = 0;
		this.transparentBytesPerChunk = 0;
		
		GL42.glMemoryBarrier(GL42.GL_ALL_BARRIER_BITS);
		this.chunkGenTasks.clear();
		this.completedGenTasks.clear();
		
		LOGGER.debug("Beginning mesh generator initialization");
		
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
		
		try
		{
			this.initExtra(manager);
		}
		catch (Exception e)
		{
			builder.errorUnknown(e, "Init Extra");
		}
		
		List<PreparedChunk> preparedChunks = this.getLodConfig().getPreparedChunks();
		ImmutableList.Builder<MeshChunk> meshChunks = ImmutableList.builder();
		int totalPreparedChunks = preparedChunks.size();
		this.opaqueBytesPerChunk = Mth.ceil(this.opaqueBufferSize / totalPreparedChunks);
		this.transparentBytesPerChunk = Mth.ceil(this.transparentBufferSize / totalPreparedChunks);
		if (!this.useFixedMeshDataSectionSize)
		{
			this.opaqueBytesPerChunk *= 4;
			this.transparentBytesPerChunk *= 4;
		}
		int maxOpaqueElements = Mth.floor((float)this.opaqueBytesPerChunk / (float)BYTES_PER_SIDE_INFO);
		int maxTransparentElements = Mth.floor((float)this.transparentBytesPerChunk / (float)BYTES_PER_CUBE_INFO);
		int opaqueElementOffset = 0;
		int transparentElementOffset = 0;
		for (PreparedChunk chunk : preparedChunks)
		{
			meshChunks.add(new MeshChunk(chunk, maxOpaqueElements, opaqueElementOffset, BYTES_PER_SIDE_INFO, maxTransparentElements, transparentElementOffset, BYTES_PER_CUBE_INFO, this.useTransparency));
			opaqueElementOffset += maxOpaqueElements;
			transparentElementOffset += maxTransparentElements;
		}
		this.chunks = meshChunks.build();
		
		LOGGER.debug("Opaque buffer size: {} bytes, transparent buffer size: {} bytes", this.opaqueBufferSize, this.transparentBufferSize);
		
		if (this.sideMesh != null)
			this.sideMesh.destroy();
		this.sideMesh = InstanceableMesh.defaultSide();
		
		if (this.cubeMesh != null)
			this.cubeMesh.destroy();
		this.cubeMesh = InstanceableMesh.defaultCube();
		
		BindingManager.printDebug();
		
		LOGGER.debug("Finished initializing mesh generator");
		
		return builder.build();
	}
	
	protected ComputeShader createShader(ResourceManager manager) throws IOException
	{
		ImmutableMap<String, String> parameters = ImmutableMap.of(
				"TYPE", String.valueOf(this.shaderType),
				"FADE_NEAR_ORIGIN", this.fadeNearOrigin ? "1" : "0",
				"STYLE", this.shadedClouds ? "1" : "0",
				"TRANSPARENCY", this.useTransparency ? "1" : "0",
				"FIXED_SECTION_SIZE", this.useFixedMeshDataSectionSize ? "1" : "0"
		);
		return ComputeShader.loadShader(this.meshShaderLoc, manager, LOCAL_SIZE, LOCAL_SIZE, LOCAL_SIZE, parameters);
	}
	
	protected void setupShader()
	{
		this.opaqueBufferSize = this.createBuffers(
				TOTAL_SIDES_NAME, 
				SIDES_PER_CHUNK_NAME, 
				SIDE_INFO_BUFFER_NAME,
				MAX_SIDE_INFO_BUFFER_SIZE * (this.useFixedMeshDataSectionSize ? 4 : 1)
		);
		
		if (this.useTransparency)
		{
			this.transparentBufferSize = this.createBuffers(
					TRANSPARENT_TOTAL_CUBES_NAME, 
					TRANSPARENT_CUBES_PER_CHUNK_NAME,
					TRANSPARENT_CUBE_INFO_BUFFER_NAME,
					MAX_TRANSPARENT_CUBE_INFO_BUFFER_SIZE * (this.useFixedMeshDataSectionSize ? 4 : 1)
			);
		}
		
		this.shader.forUniform("TotalLodLevels", (id, loc) -> {
			GL41.glProgramUniform1i(id, loc, this.lodConfig.getLods().length);
		});
		
		this.uploadFadeData();
	}
	
	private void uploadFadeData()
	{
		if (this.shader == null || !this.shader.isValid())
			return;
		
		this.shader.forUniform("TransparencyDistance", (id, loc) -> {
			GL41.glProgramUniform1i(id, loc, this.transparencyDistance);
		});
		this.shader.forUniform("FadeStart", (id, loc) -> {
			GL41.glProgramUniform1f(id, loc, this.fadeStart);
		});
		this.shader.forUniform("FadeEnd", (id, loc) -> {
			GL41.glProgramUniform1f(id, loc, this.fadeEnd);
		});
	}
	
	private int createBuffers(String totalCounterName, String countPerChunkName, String elementInfoBufferName, int maxSize)
	{
		if (!this.useFixedMeshDataSectionSize)
		{
			ShaderStorageBufferObject totalCountBuffer = this.shader.createAndBindSSBO(totalCounterName, GL15.GL_DYNAMIC_COPY);
			totalCountBuffer.allocateBuffer(4);
			totalCountBuffer.writeData(b -> {
				b.putInt(0, 0);
			}, 4, false);
		}
		
		int bufferSize = this.shader.createAndBindSSBO(elementInfoBufferName, GL15.GL_DYNAMIC_COPY).allocateBuffer(maxSize);
		
		int totalChunks = this.getLodConfig().getPreparedChunks().size();
		int countPerChunkBufferSize = totalChunks * 4;
		ShaderStorageBufferObject countPerChunkBuffer = this.shader.createAndBindSSBO(countPerChunkName, GL15.GL_DYNAMIC_COPY);
		countPerChunkBuffer.allocateBuffer(countPerChunkBufferSize);
		countPerChunkBuffer.writeData(b -> 
		{
			for (int i = 0; i < totalChunks; i++)
				b.putInt(0);
			b.rewind();
		}, countPerChunkBufferSize, false);
		
		return bufferSize;
	}
	
	protected void initExtra(ResourceManager manager) throws IOException {}
	
	/**
	 * Generates the entire cloud mesh at the origin at once
	 */
	public void generateMesh()
	{
		RenderSystem.assertOnRenderThread();
		
		if (this.shader == null || !this.shader.isValid())
			return;
		
		this.prepareMeshGen(0.0D, 0.0D, 0.0D, 0.0F, 0.0F, null, 1, 1.0F);
		
		if (!this.chunkGenTasks.isEmpty())
			this.doMeshGenning(this.chunkGenTasks.size());
		
		this.meshGenStatus = this.finalizeMeshGen();
		this.completedGenTasks.clear();
	}
	
	public void worldTick()
	{
		if (this.chunks != null)
			this.chunks.forEach(MeshChunk::tick);
	}
	
	/**
	 * Generates the cloud mesh on a per-frame basis
	 * 
	 * @param originX
	 * @param originY
	 * @param originZ
	 * @param frustum
	 */
	public void genTick(double originX, double originY, double originZ, @Nullable Frustum frustum, float partialTick)
	{
		RenderSystem.assertOnRenderThread();
		
		if (this.shader == null || !this.shader.isValid())
			return;

		float chunkSize = (float)SimpleCloudsConstants.CHUNK_SIZE;
		float meshGenOffsetX = (float)Mth.floor(originX / chunkSize) * chunkSize;
		float meshGenOffsetZ = (float)Mth.floor(originZ / chunkSize) * chunkSize;
		
		if (this.chunkGenTasks.isEmpty()) //If we have no chunk gen tasks
		{
			this.meshGenStatus = this.finalizeMeshGen(); //Split the combined mesh data from the GPU, and store them in the VBOs for each chunk that was generated
			this.completedGenTasks.clear(); //Clear the chunk gen tasks
			
			//Prepare the next batch of chunks to generate meshes for
			this.tasksPerTick = this.prepareMeshGen(originX, originY, originZ, meshGenOffsetX, meshGenOffsetZ, frustum, this.meshGenInterval, partialTick);
		}
		else
		{
			this.onOffGen();
		}
		
		//If there are mesh gen tasks, we do mesh genning
		if (!this.chunkGenTasks.isEmpty())
			this.doMeshGenning(this.tasksPerTick);
	}
	
	private static CloudMeshGenerator.MeshGenStatus fixedIterateAndCopyToChunkBuffer(int copyBufferId, int copyBufferSizeBytes, Collection<MeshChunk> chunks, Function<MeshChunk, Integer> byteOffsetPerChunk, Function<MeshChunk, Integer> chunkBufferId, Function<MeshChunk, Integer> bytesToCopyPerChunk, Function<MeshChunk, Integer> bufferSizeBytesPerChunk)
	{
		CloudMeshGenerator.MeshGenStatus result = CloudMeshGenerator.MeshGenStatus.NORMAL;
		
		GlStateManager._glBindBuffer(GL31.GL_COPY_READ_BUFFER, copyBufferId);
		
		for (MeshChunk chunk : chunks)
		{
			int bytesToCopy = bytesToCopyPerChunk.apply(chunk);
			if (bytesToCopy > 0)
			{
				int maxSize = bufferSizeBytesPerChunk.apply(chunk);
				if (bytesToCopy > maxSize) // Too many bytes to go in to the chunk mesh buffer
				{
					bytesToCopy = maxSize;
					result = CloudMeshGenerator.MeshGenStatus.CHUNK_OVERFLOW;
				}
				
				int byteOffset = byteOffsetPerChunk.apply(chunk);
				if (byteOffset + bytesToCopy > copyBufferSizeBytes) // TODO: Account for this overflow using mesh gen status
				{
					//TODO: Make sure this uses multiples of the size of a single element?
					bytesToCopy = copyBufferSizeBytes - byteOffset;
					if (bytesToCopy <= 0)
						continue;
				}
				
				GlStateManager._glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, chunkBufferId.apply(chunk));
				GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER, byteOffset, 0, bytesToCopy);
			}
		}
		
		return result;
	}
	
	private static CloudMeshGenerator.MeshGenStatus packedIterateAndCopyToChunkBuffer(int copyBufferId, int copyBufferSizeBytes, Collection<MeshChunk> chunks, Function<MeshChunk, Integer> chunkBufferId, Function<MeshChunk, Integer> bytesToCopyPerChunk, Function<MeshChunk, Integer> bufferSizeBytesPerChunk)
	{
		CloudMeshGenerator.MeshGenStatus result = CloudMeshGenerator.MeshGenStatus.NORMAL;
		
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
					result = CloudMeshGenerator.MeshGenStatus.CHUNK_OVERFLOW;
				}
				boolean stop = false;
				if (currentBytes + lastBytesOffset > copyBufferSizeBytes) //If the the byte offset will go over the size of the copy buffer, clamp
				{
					lastBytesOffset = copyBufferSizeBytes - currentBytes;
					if (lastBytesOffset <= 0) // If it becomes negative however, we don't want to attempt to copy data over
						return CloudMeshGenerator.MeshGenStatus.MESH_POOL_OVERFLOW;
					stop = true; // After copying this data over we will stop, since there is no more space in the copy buffer to read data from
				}
				
				GlStateManager._glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, chunkBufferId.apply(chunk));
				GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER, currentBytes, 0, lastBytesOffset);
				
				currentBytes += totalBytes;
				
				if (stop)
					return CloudMeshGenerator.MeshGenStatus.MESH_POOL_OVERFLOW;
			}
		}
		
		return result;
	}
	
	protected Pair<CloudMeshGenerator.MeshGenStatus, CloudMeshGenerator.MeshGenStatus> finalizeMeshGen()
	{
		if (this.shader == null || !this.shader.isValid() || this.chunks == null)
			return Pair.of(CloudMeshGenerator.MeshGenStatus.NOT_INITIALIZED, CloudMeshGenerator.MeshGenStatus.NOT_INITIALIZED);
		
		if (this.completedGenTasks.isEmpty())
			return Pair.of(CloudMeshGenerator.MeshGenStatus.NO_TASKS, CloudMeshGenerator.MeshGenStatus.NO_TASKS);
		
		RenderSystem.assertOnRenderThread();
		
		GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
			
		CloudMeshGenerator.MeshGenStatus opaqueResult = CloudMeshGenerator.MeshGenStatus.NORMAL;
		CloudMeshGenerator.MeshGenStatus transparentResult = CloudMeshGenerator.MeshGenStatus.NORMAL;
		
		opaqueResult = this.copyMeshData(
				TOTAL_SIDES_NAME, 
				SIDES_PER_CHUNK_NAME, 
				SIDE_INFO_BUFFER_NAME, 
				MeshChunk::getOpaqueBuffers,
				BYTES_PER_SIDE_INFO,
				this.opaqueBufferSize
		);
		
		if (this.useTransparency)
		{
			transparentResult = this.copyMeshData(
					TRANSPARENT_TOTAL_CUBES_NAME, 
					TRANSPARENT_CUBES_PER_CHUNK_NAME, 
					TRANSPARENT_CUBE_INFO_BUFFER_NAME, 
					c -> c.getTransparentBuffers().get(),
					BYTES_PER_CUBE_INFO,
					this.transparentBufferSize
			);
		}
		
		this.opaqueBufferBytesUsed = 0;
		this.transparentBufferBytesUsed = 0;
		for (MeshChunk chunk : this.chunks)
		{
			this.opaqueBufferBytesUsed += chunk.getOpaqueBuffers().getElementCount() * BYTES_PER_SIDE_INFO;
			chunk.getTransparentBuffers().ifPresent(bufferSet -> {
				this.transparentBufferBytesUsed += bufferSet.getElementCount() * BYTES_PER_CUBE_INFO;
			});
		}
		
		return Pair.of(opaqueResult, transparentResult);
	}
	
	private CloudMeshGenerator.MeshGenStatus copyMeshData(String totalCountBufferName, String countPerChunkBufferName, String elementBufferName, Function<MeshChunk, MeshChunk.BufferSet> bufferSetFunction, int bytesPerElement, int elementBufferSize)
	{
		CloudMeshGenerator.MeshGenStatus status = CloudMeshGenerator.MeshGenStatus.NORMAL;
		
		if (!this.useFixedMeshDataSectionSize)
		{
			//Get the total amount of sides and indices across all chunks and reset
			this.shader.getShaderStorageBuffer(totalCountBufferName).writeData(b -> {
				b.putInt(0, 0);
			}, 4, true); 
		}
		
		//Get the amount of total sides each chunk has and reset each counter
		this.shader.getShaderStorageBuffer(countPerChunkBufferName).readWriteData(buffer -> 
		{
			for (CloudMeshGenerator.ChunkGenTask gennedChunk : this.completedGenTasks)
			{
				MeshChunk.BufferSet bufferSet = bufferSetFunction.apply(gennedChunk.chunk());
				int index = gennedChunk.index() * 4;
				int count = buffer.getInt(index);
				bufferSet.setTotalElementCount(count);
				buffer.putInt(index, 0);
			}
		}, this.chunks.size() * 4);
		
		List<MeshChunk> completedChunks = this.completedGenTasks.stream().map(CloudMeshGenerator.ChunkGenTask::chunk).toList();
		
		int elementBufferId = this.shader.getShaderStorageBuffer(elementBufferName).getId();
		if (this.useFixedMeshDataSectionSize)
			status = fixedIterateAndCopyToChunkBuffer(elementBufferId, elementBufferSize, completedChunks, bufferSetFunction.andThen(b -> b.getElementOffset() * bytesPerElement), bufferSetFunction.andThen(MeshChunk.BufferSet::getBufferId), bufferSetFunction.andThen(c -> c.getElementCount() * bytesPerElement), bufferSetFunction.andThen(MeshChunk.BufferSet::getBufferSize));
		else
			status = packedIterateAndCopyToChunkBuffer(elementBufferId, elementBufferSize, completedChunks, bufferSetFunction.andThen(MeshChunk.BufferSet::getBufferId), bufferSetFunction.andThen(c -> c.getElementCount() * bytesPerElement), bufferSetFunction.andThen(MeshChunk.BufferSet::getBufferSize));
		
		GlStateManager._glBindBuffer(GL31.GL_COPY_READ_BUFFER, 0);
		GlStateManager._glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, 0);
		
		return status;
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
	protected int prepareMeshGen(double originX, double originY, double originZ, float meshGenOffsetX, float meshGenOffsetZ, @Nullable Frustum frustum, int genInterval, float partialTick)
	{
		this.shader.forUniform("Scroll", (id, loc) -> {
			GL41.glProgramUniform3f(id, loc, this.scrollX, this.scrollY, this.scrollZ);
		});
		this.shader.forUniform("Wiggle", (id, loc) -> {
			GL41.glProgramUniform1f(id, loc, (this.scrollX + this.scrollY + this.scrollZ) / 5.0F);
		});
		this.shader.forUniform("Origin", (id, loc) -> {
			GL41.glProgramUniform3f(id, loc, (float)originX, (float)originY, (float)originZ);
		});
		this.shader.forUniform("TestFacesFacingAway", (id, loc) -> {
			GL41.glProgramUniform1i(id, loc, this.testFacesFacingAway ? 1 : 0);
		});
		this.uploadFadeData();
		
		int chunkCount = 0;
		for (int i = 0; i < this.chunks.size(); i++)
		{
			if (this.queueChunkMeshGenTaskOrClear(this.chunks.get(i), i, meshGenOffsetX, meshGenOffsetZ, frustum))
				chunkCount++;
		}
		return Mth.ceil((float)chunkCount / (float)genInterval);
	}
	
	protected void onOffGen()
	{
		//We read these SSBOs here to avoid weird frame spikes when in fullscreen V-Sync, not sure why it happens
		if (!this.useFixedMeshDataSectionSize)
			this.shader.getShaderStorageBuffer(TOTAL_SIDES_NAME).readWriteData(b -> {}, 4);
		this.shader.getShaderStorageBuffer(SIDES_PER_CHUNK_NAME).readWriteData(buffer -> {}, this.chunks.size() * 4);
		if (this.useTransparency)
		{
			if (!this.useFixedMeshDataSectionSize)
				this.shader.getShaderStorageBuffer(TRANSPARENT_TOTAL_CUBES_NAME).readWriteData(b -> {}, 4);
			this.shader.getShaderStorageBuffer(TRANSPARENT_CUBES_PER_CHUNK_NAME).readWriteData(buffer -> {}, this.chunks.size() * 4);
		}
	}
	
	/**
	 * Queues a given chunk for mesh genning or clears it if empty
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
	protected boolean queueChunkMeshGenTaskOrClear(MeshChunk chunk, int chunkIndex, float meshGenOffsetX, float meshGenOffsetZ, @Nullable Frustum frustum)
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
				CloudMeshGenerator.ChunkGenSettings settings = this.determineChunkGenSettings(minX, minZ, maxX, maxZ);
				if (settings.skipChunk())
				{
					chunk.clearChunk();
					return false;
				}
				this.chunkGenTasks.add(new CloudMeshGenerator.ChunkGenTask(chunk, minX, (float)bounds.minY, minZ, maxX, (float)bounds.maxY, maxZ, chunkIndex, minX, 0.0F, minZ, settings.minimumHeight(), settings.maximumHeight()));
				return true;
			}
		}
		return false;
	}
	
	protected abstract CloudMeshGenerator.ChunkGenSettings determineChunkGenSettings(float minX, float minZ, float maxX, float maxZ);
	
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
		chunk.resetLastGenTime();
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
		int lowestY = task.startY();
		int height = Mth.ceil((float)(task.endY() - lowestY) / (float)lodScale);
		int localHeightInvocations = Mth.ceil((float)height / (float)LOCAL_SIZE);
		
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
			if (this.useFixedMeshDataSectionSize)
			{
				this.shader.forUniform("OpaqueMeshDataOffset", (id, loc) -> {
					GL41.glProgramUniform1i(id, loc, task.chunk().getOpaqueBuffers().getElementOffset());
				});
				
				task.chunk().getTransparentBuffers().ifPresent(bufferSet -> 
				{
					this.shader.forUniform("TransparentMeshDataOffset", (id, loc) -> {
						GL41.glProgramUniform1i(id, loc, bufferSet.getElementOffset());
					});
				});
			}
			
			this.shader.dispatch(WORK_SIZE, localHeightInvocations, WORK_SIZE, false);
			if (!this.useFixedMeshDataSectionSize)
				GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
		}
	}
	
	@Deprecated
	protected void clearChunk(CloudMeshGenerator.ChunkGenTask task)
	{
		Consumer<String> clear = countPerChunkBufferName -> 
		{
			//Clear count. This will cause the given chunk to not render in the render pass
			//TODO: Instead of modifying this make the copy func ignore this
			this.shader.getShaderStorageBuffer(countPerChunkBufferName).writeData(buffer -> {
				buffer.putInt(task.index() * 4, 0);
			}, task.index() * 4 + 4, false);
		};
		clear.accept(SIDES_PER_CHUNK_NAME);
		if (this.transparencyEnabled())
			clear.accept(TRANSPARENT_CUBES_PER_CHUNK_NAME);
	}
	
	public void forRenderableMeshChunks(@Nullable Frustum frustum, Function<MeshChunk, MeshChunk.BufferSet> bufferSetFunction, BiConsumer<MeshChunk, MeshChunk.BufferSet> function)
	{
		this.forRenderableMeshChunks(frustum, bufferSetFunction, function, false);
	}
	
	public void forRenderableMeshChunks(@Nullable Frustum frustum, Function<MeshChunk, MeshChunk.BufferSet> bufferSetFunction, BiConsumer<MeshChunk, MeshChunk.BufferSet> function, boolean updateFade)
	{
		for (MeshChunk chunk : this.chunks)
		{
			MeshChunk.BufferSet bufferSet = bufferSetFunction.apply(chunk);
			if (bufferSet.getElementCount() > 0)
			{
				if (updateFade && chunk.getTicksSinceLastGen() > TICKS_UNTIL_FADE_RESET)
				{
					chunk.resetAlpha();
					chunk.setFadeEnabled(false);
				}
				
				boolean render = true;
				if (frustum != null)
					render = ((MixinFrustumAccessor)frustum).simpleclouds$cubeInFrustum(chunk.getBoundsMinX(), chunk.getBoundsMinY(), chunk.getBoundsMinZ(), chunk.getBoundsMaxX(), chunk.getBoundsMaxY(), chunk.getBoundsMaxZ());
				
				if (render)
				{
					PreparedChunk chunkInfo = chunk.getChunkInfo();
					AABB bounds = chunkInfo.bounds();
					double nearestCornerX = Math.max(Math.max(bounds.minX, -bounds.maxX), 0.0D);
					double nearestCornerZ = Math.max(Math.max(bounds.minZ, -bounds.maxZ), 0.0D);
					double dist =  Math.sqrt(nearestCornerX * nearestCornerX + nearestCornerZ * nearestCornerZ);
					if (this.cullDistance <= 0.0F || this.cullDistance > dist)
					{
						if (updateFade)
							chunk.setFadeEnabled(true);
						function.accept(chunk, bufferSet);
					}
				}
			}
		}
	}
	
	public void fillReport(CrashReportCategory category)
	{
		category.setDetail("Shader Type", this.shaderType);
		category.setDetail("Shaded Clouds", this.shadedClouds);
		category.setDetail("Transparency Enabled", this.useTransparency);
		category.setDetail("Fade Near Origin", this.fadeNearOrigin);
		category.setDetail("Compute Shader", this.shader);
		category.setDetail("Level Of Details", 1 + this.lodConfig.getLods().length);
		category.setDetail("Generation Frame Interval", this.meshGenInterval);
		category.setDetail("Total Prepared Chunks", this.lodConfig.getPreparedChunks().size());
		category.setDetail("Tasks Per Frame", this.tasksPerTick);
		category.setDetail("Scroll", String.format("X: %s, Y: %s, Z: %s", this.scrollX, this.scrollY, this.scrollZ));
		category.setDetail("Total Mesh Chunks", this.chunks != null ? this.chunks.size() : "null");
		category.setDetail("Mesh Gen Status", this.meshGenStatus);
		category.setDetail("Test Occluded Faces", this.testFacesFacingAway);
	}
	
	@Override
	public String toString()
	{
		return String.format("%s[shader_name=%s]", this.getClass().getSimpleName(), this.meshShaderLoc);
	}
	
	protected static CloudMeshGenerator.ChunkGenSettings skip()
	{
		return new CloudMeshGenerator.ChunkGenSettings(true, 0, 0);
	}
	
	protected static CloudMeshGenerator.ChunkGenSettings heights(int min, int max)
	{
		return new CloudMeshGenerator.ChunkGenSettings(false, min, max);
	}
	
	public static CloudMeshGenerator.Builder builder()
	{
		return new CloudMeshGenerator.Builder();
	}
	
	protected static record ChunkGenSettings(boolean skipChunk, int minimumHeight, int maximumHeight) {}
	
	protected static record ChunkGenTask(MeshChunk chunk, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int index, float x, float y, float z, int startY, int endY) {}
	
	public static enum MeshGenStatus
	{
		NOT_INITIALIZED("Not initialized", true),
		NO_TASKS("No tasks", false),
		NORMAL("Normal", false),
		MESH_POOL_OVERFLOW("Mesh pool overflow", true),
		CHUNK_OVERFLOW("Chunk overflow", true);
		
		private String name;
		private boolean isErroneous;
		
		private MeshGenStatus(String name, boolean isErroneous)
		{
			this.name = name;
			this.isErroneous = isErroneous;
		}
		
		public String getName()
		{
			return this.name;
		}
		
		public boolean isErroneous()
		{
			return this.isErroneous;
		}
	}
	
	public static class Builder
	{
		private boolean fadeNearOrigin;
		private boolean shadedClouds = true;
		private LevelOfDetailConfig lodConfig = LevelOfDetailOptions.HIGH.getConfig();
		private int meshGenInterval = 5;
		private boolean useTransparency = true;
		private boolean fixedMeshDataSectionSize;
		private float fadeStart;
		private float fadeEnd;
		private boolean testFacesFacingAway = false;
		
		private Builder() {}
		
		public Builder fadeNearOrigin(boolean flag)
		{
			this.fadeNearOrigin = flag;
			return this;
		}
		
		public Builder shadedClouds(boolean flag)
		{
			this.shadedClouds = flag;
			return this;
		}
		
		public Builder meshGenInterval(int interval)
		{
			if (interval <= 0)
				throw new IllegalArgumentException("Mesh gen interval must be greater than 0");
			this.meshGenInterval = interval;
			return this;
		}
		
		public Builder lodConfig(LevelOfDetailConfig config)
		{
			this.lodConfig = config;
			return this;
		}
		
		public Builder useTransparency(boolean flag)
		{
			this.useTransparency = flag;
			return this;
		}
		
		public Builder fixedMeshDataSectionSize(boolean flag)
		{
			this.fixedMeshDataSectionSize = flag;
			return this;
		}
		
		public Builder fadeStart(float fadeStart)
		{
			this.fadeStart = fadeStart;
			return this;
		}
		
		public Builder fadeEnd(float fadeEnd)
		{
			this.fadeEnd = fadeEnd;
			return this;
		}
		
		public Builder testFacesFacingAway(boolean flag)
		{
			this.testFacesFacingAway = flag;
			return this;
		}
		
		private <T extends CloudMeshGenerator> T applyExtraSettings(T generator)
		{
			generator.setFadeDistances(this.fadeStart, this.fadeEnd);
			generator.setTestFacesFacingAway(this.testFacesFacingAway);
			return generator;
		}
		
		public MultiRegionCloudMeshGenerator createMultiRegion()
		{
			return this.applyExtraSettings(new MultiRegionCloudMeshGenerator(this.fadeNearOrigin, this.shadedClouds, this.lodConfig, this.meshGenInterval, this.useTransparency, this.fixedMeshDataSectionSize));
		}
		
		public SingleRegionCloudMeshGenerator createSingleRegion(CloudInfo type)
		{
			return this.applyExtraSettings(new SingleRegionCloudMeshGenerator(this.shadedClouds, this.lodConfig, this.meshGenInterval, this.useTransparency, this.fixedMeshDataSectionSize, type));
		}
	}
}
