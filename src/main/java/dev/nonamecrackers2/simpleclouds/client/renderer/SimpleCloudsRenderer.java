package dev.nonamecrackers2.simpleclouds.client.renderer;

import java.io.IOException;
import java.nio.IntBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import com.google.common.collect.Maps;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.cloud.ClientSideCloudTypeManager;
import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.mesh.CloudStyle;
import dev.nonamecrackers2.simpleclouds.client.mesh.GeneratorInitializeResult;
import dev.nonamecrackers2.simpleclouds.client.mesh.SingleRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.mesh.multiregion.MultiRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.renderer.lightning.LightningBolt;
import dev.nonamecrackers2.simpleclouds.client.renderer.pipeline.CloudsRenderPipeline;
import dev.nonamecrackers2.simpleclouds.client.shader.SimpleCloudsShaders;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.ShaderStorageBufferObject;
import dev.nonamecrackers2.simpleclouds.client.world.ClientCloudManager;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.RegionType;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.init.RegionTypes;
import dev.nonamecrackers2.simpleclouds.common.registry.SimpleCloudsRegistries;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import dev.nonamecrackers2.simpleclouds.mixin.MixinPostChain;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nonamecrackers2.crackerslib.common.compat.CompatHelper;

public class SimpleCloudsRenderer implements ResourceManagerReloadListener
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/SimpleCloudsRenderer");
	private static final Vector3f DIFFUSE_LIGHT_0 = (new Vector3f(0.2F, 1.0F, -0.7F)).normalize();
	private static final Vector3f DIFFUSE_LIGHT_1 = (new Vector3f(-0.2F, 1.0F, 0.7F)).normalize();
	private static final ResourceLocation CLOUD_POST_PROCESSING_LOC = SimpleCloudsMod.id("shaders/post/cloud_post.json");
	private static final ResourceLocation STORM_POST_PROCESSING_LOC = SimpleCloudsMod.id("shaders/post/storm_post.json");
	private static final ResourceLocation BLUR_POST_PROCESSING_LOC = ResourceLocation.withDefaultNamespace("shaders/post/blur.json");//SimpleCloudsMod.id("shaders/post/blur_post.json");
	private static final ResourceLocation SCREEN_SPACE_WORLD_FOG_LOC = SimpleCloudsMod.id("shaders/post/screen_space_world_fog.json");
	public static final int SHADOW_MAP_SIZE = 1024;
	public static final int MAX_LIGHTNING_BOLTS = 16;
	public static final int BYTES_PER_LIGHTNING_BOLT = 16;
	private static @Nullable SimpleCloudsRenderer instance;
	private final Minecraft mc;
	private final WorldEffects worldEffectsManager;
	private CloudMeshGenerator meshGenerator;
	private Matrix4f shadowMapProjMat;
	private @Nullable RenderTarget cloudTarget;
	private @Nullable RenderTarget stormFogTarget;
	private @Nullable RenderTarget blurTarget;
	private final Map<PostChain, Pair<Float, Float>> postChains = Maps.newHashMap();
	private @Nullable PostChain cloudsPostProcessing;
	private @Nullable PostChain stormPostProcessing;
	private @Nullable PostChain blurPostProcessing;
	private @Nullable PostChain screenSpaceWorldFog;
	private @Nullable ShaderStorageBufferObject lightningBoltPositions;
	private Frustum cullFrustum;
	private int shadowMapBufferId = -1;
	private int shadowMapDepthTextureId = -1;
	private int shadowMapColorTextureId = -1;
	private float fogStart;
	private float fogEnd;
	private @Nullable Matrix4f shadowMapViewMat;
	private boolean failedToCopyDepthBuffer;
	private @Nullable CloudMode cloudMode;
	private @Nullable CloudStyle cloudStyle;
	private @Nullable RegionType regionGenerator;
	private boolean needsReload;
	private @Nullable GeneratorInitializeResult initialInitializationResult;
	
	private SimpleCloudsRenderer(Minecraft mc)
	{
		this.mc = mc;
		this.worldEffectsManager = new WorldEffects(mc, this);
	}
	
	private void setupMeshGenerator(float partialTicks)
	{
		this.meshGenerator.setMeshGenInterval(SimpleCloudsConfig.CLIENT.framesToGenerateMesh.get());
		this.meshGenerator.setTestFacesFacingAway(SimpleCloudsConfig.CLIENT.testSidesThatAreOccluded.get());
		this.meshGenerator.setLodConfig(SimpleCloudsConfig.CLIENT.levelOfDetail.get().getConfig());
		if (this.mc.level != null)
		{
			CloudManager<ClientLevel> manager = CloudManager.get(this.mc.level);
			this.meshGenerator.setScroll(manager.getScrollX(partialTicks), manager.getScrollY(partialTicks), manager.getScrollZ(partialTicks));
		}
	}
	
	private CloudMode determineCloudMode()
	{
		if (this.mc.level != null)
			return CloudManager.get(this.mc.level).getCloudMode();
		else
			return CloudMode.DEFAULT;
	}
	
	private String determineSingleModeCloudTypeRawId()
	{
		if (this.mc.level != null)
			return CloudManager.get(this.mc.level).getSingleModeCloudTypeRawId();
		else
			return "simpleclouds:itty_bitty";
	}
	
	private RegionType determineRegionGenerator()
	{
		if (this.mc.level != null)
			return CloudManager.get(this.mc.level).getRegionGenerator();
		else
			return RegionTypes.VORONOI_DIAGRAM.get();
	}
	
	/**
	 * The current cloud mode that is set up with the renderer
	 * <p><p>
	 * May return {@code NULL} if the renderer has not been initialized yet
	 * 
	 * @return {@link CloudMode}
	 */
	public @Nullable CloudMode getCloudMode()
	{
		return this.cloudMode;
	}
	
	/**
	 * The current cloud style that is set up with the renderer
	 * <p><p>
	 * May return {@code NULL} if the renderer has not been initialized yet
	 * 
	 * @return {@link CloudStyle}
	 */
	public @Nullable CloudStyle getCloudStyle()
	{
		return this.cloudStyle;
	}
	
	/**
	 * The current region generator that is set up with the renderer
	 * <p><p>
	 * May return {@code NULL} if the renderer has not been initialized yet
	 * 
	 * @return {@link RegionType}
	 */
	public @Nullable RegionType getRegionGenerator()
	{
		return this.regionGenerator;
	}
	
	public void requestReload()
	{
		LOGGER.debug("Requesting reload...");
		this.needsReload = true;
	}
	
	@Override
	public void onResourceManagerReload(ResourceManager manager)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		
		this.failedToCopyDepthBuffer = false;
		
		if (this.cloudTarget != null)
			this.cloudTarget.destroyBuffers();
		this.cloudTarget = new TextureTarget(this.mc.getWindow().getWidth(), this.mc.getWindow().getHeight(), true, Minecraft.ON_OSX);
		this.cloudTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
		
		if (this.stormFogTarget != null)
			this.stormFogTarget.destroyBuffers();
		this.stormFogTarget = new TextureTarget(this.mc.getWindow().getWidth() / 4, this.mc.getWindow().getHeight() / 4, false, Minecraft.ON_OSX);
		this.stormFogTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
		this.stormFogTarget.setFilterMode(GL11.GL_LINEAR);
		
		if (this.blurTarget != null)
			this.blurTarget.destroyBuffers();
		this.blurTarget = new TextureTarget(this.mc.getWindow().getWidth(), this.mc.getWindow().getHeight(), false, Minecraft.ON_OSX);
		this.blurTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
		this.blurTarget.setFilterMode(GL11.GL_LINEAR);
		
		Instant started = Instant.now();
		CloudMode mode = this.determineCloudMode(); //Determine the cloud mode we should use
		CloudStyle style = SimpleCloudsConfig.CLIENT.cloudStyle.get();
		LOGGER.info("Beginning mesh generator initialization for cloud mode {} and cloud style {}", mode, style);
		if (this.cloudMode != mode || this.cloudStyle != style) //If the cloud mode and cloud style is different then what was previously initialized, recreate the mesh generators
		{
			if (this.meshGenerator != null)
			{
				this.meshGenerator.close(); //Close the current generator
				this.meshGenerator = null;
			}
			
			if (mode == CloudMode.DEFAULT || mode == CloudMode.AMBIENT) //Use the multi-region generator for DEFAULT or AMBIENT cloud mode
			{
				//Create the generator but use a fallback cloud types array
				MultiRegionCloudMeshGenerator generator = new MultiRegionCloudMeshGenerator(new CloudType[] { SimpleCloudsConstants.FALLBACK }, SimpleCloudsConfig.CLIENT.levelOfDetail.get().getConfig(), RegionTypes.VORONOI_DIAGRAM.get(), SimpleCloudsConfig.CLIENT.framesToGenerateMesh.get(), style);
				if (mode == CloudMode.AMBIENT) //Enable the fade near origin when using AMBIENT
					generator.setFadeNearOrigin(SimpleCloudsConstants.AMBIENT_MODE_FADE_START / SimpleCloudsConstants.CLOUD_SCALE, SimpleCloudsConstants.AMBIENT_MODE_FADE_END / SimpleCloudsConstants.CLOUD_SCALE);
				this.meshGenerator = generator;
			}
			else if (mode == CloudMode.SINGLE)
			{
				float fadeStart = (float)SimpleCloudsConfig.CLIENT.singleModeFadeStartPercentage.get() / 100.0F;
				float fadeEnd = (float)SimpleCloudsConfig.CLIENT.singleModeFadeEndPercentage.get() / 100.0F;
				//Create the generator but use a fallback single mode cloud type
				this.meshGenerator = new SingleRegionCloudMeshGenerator(SimpleCloudsConstants.FALLBACK, SimpleCloudsConfig.CLIENT.levelOfDetail.get().getConfig(), SimpleCloudsConfig.CLIENT.framesToGenerateMesh.get(), fadeStart, fadeEnd, style);
			}
			else
			{
				throw new IllegalArgumentException("Not sure how to handle cloud mode " + mode);
			}
			this.cloudMode = mode; //Set the current cloud mode the renderer is using
			this.cloudStyle = style;
		}
		
		//Figure out what region generator we should use
		//This renderer can be initialized without having the player logged into a world,
		//meaning that the client-side CloudManager may not exist. This method picks a
		//region generator regardless of whether we are in game or not, to make sure
		//one is always available. This is similiar with other methods, such as with
		//determineCloudMode, etc.
		RegionType generator = this.determineRegionGenerator();
		this.regionGenerator = generator;
		
		if (this.meshGenerator instanceof MultiRegionCloudMeshGenerator multiRegionGenerator)
		{
			CloudType[] cloudTypes = ClientSideCloudTypeManager.getInstance().getIndexedCloudTypes();
			if (cloudTypes.length > MultiRegionCloudMeshGenerator.MAX_CLOUD_TYPES)
				LOGGER.warn("The amount of loaded cloud types exceeds the maximum of {}. Please be aware that not all cloud types loaded will be used.", MultiRegionCloudMeshGenerator.MAX_CLOUD_TYPES);
			else
				multiRegionGenerator.setCloudTypes(cloudTypes); //Set the cloud types using the synced, or client-side loaded ones, from ClientSideCloudTypeManager
			multiRegionGenerator.setRegionGenerator(generator); //Set the region generator
		}
		else if (this.meshGenerator instanceof SingleRegionCloudMeshGenerator singleRegionGenerator)
		{
			//Find the desired single mode cloud type, either from the client-side only context or
			//from the synced cloud types from the server
			CloudType type = ClientSideCloudTypeManager.getInstance().getCloudTypeFromRawId(this.determineSingleModeCloudTypeRawId()).orElse(null);
			if (!ClientCloudManager.isAvailableServerSide() && !ClientSideCloudTypeManager.isValidClientSideSingleModeCloudType(type))
				type = SimpleCloudsConstants.FALLBACK;
			if (type == null)
				type = SimpleCloudsConstants.FALLBACK;
			singleRegionGenerator.setCloudType(type);
		}
		
		this.setupMeshGenerator(0.0F); //Setup the mesh generator
		
		GeneratorInitializeResult result = this.meshGenerator.init(manager); //Initialize
		if (this.initialInitializationResult == null)
			this.initialInitializationResult = result;
		
		long duration = Duration.between(started, Instant.now()).toMillis();
		LOGGER.info("Finished, took {} ms", duration);
		
		int span = this.meshGenerator.getLodConfig().getEffectiveChunkSpan() * SimpleCloudsConstants.CHUNK_SIZE * SimpleCloudsConstants.CLOUD_SCALE;
		this.shadowMapProjMat = new Matrix4f().setOrtho(0.0F, span, span, 0.0F, 0.0F, 10000.0F);
		
		this.destroyPostChains();
		
		this.cloudsPostProcessing = this.createPostChain(manager, CLOUD_POST_PROCESSING_LOC, this.cloudTarget, 1.0F, 1.0F);
		
		if (this.lightningBoltPositions != null)
		{
			this.lightningBoltPositions.closeAndClearBinding();
			this.lightningBoltPositions = null;
		}
		
		this.lightningBoltPositions = ShaderStorageBufferObject.create(GL15.GL_DYNAMIC_DRAW);
		this.lightningBoltPositions.allocateBuffer(MAX_LIGHTNING_BOLTS * BYTES_PER_LIGHTNING_BOLT);
		
		this.stormPostProcessing = this.createPostChain(manager, STORM_POST_PROCESSING_LOC, this.stormFogTarget, 0.25F, 0.25F, effect -> {
			effect.setSampler("ShadowMap", () -> this.shadowMapDepthTextureId);
			effect.setSampler("ShadowMapColor", () -> this.shadowMapColorTextureId);
			this.lightningBoltPositions.optionalBindToProgram("LightningBolts", effect.getId());
		});
		
		this.blurPostProcessing = this.createPostChain(manager, BLUR_POST_PROCESSING_LOC, this.blurTarget, 1.0F, 1.0F);
		
		this.screenSpaceWorldFog = this.createPostChain(manager, SCREEN_SPACE_WORLD_FOG_LOC, this.mc.getMainRenderTarget(), 1.0F, 1.0F, effect -> {
			effect.setSampler("StormFogSampler", () -> this.blurTarget.getColorTextureId());
			effect.setSampler("CloudDepthSampler", () -> this.cloudTarget.getDepthTextureId());
		});
		
		
		if (this.shadowMapDepthTextureId != -1)
		{
			TextureUtil.releaseTextureId(this.shadowMapDepthTextureId);
			this.shadowMapBufferId = -1;
		}
		
		if (this.shadowMapColorTextureId != -1)
		{
			TextureUtil.releaseTextureId(this.shadowMapColorTextureId);
			this.shadowMapColorTextureId = -1;
		}
		
		if (this.shadowMapBufferId != -1)
		{
			GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
			GlStateManager._glDeleteFramebuffers(this.shadowMapBufferId);
			this.shadowMapBufferId = -1;
		}
		
		GlStateManager._enableDepthTest();
		this.shadowMapBufferId = GlStateManager.glGenFramebuffers();
		this.shadowMapDepthTextureId = TextureUtil.generateTextureId();
		this.shadowMapColorTextureId = TextureUtil.generateTextureId();
		GlStateManager._bindTexture(this.shadowMapDepthTextureId);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, 0);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (IntBuffer)null);
		GlStateManager._bindTexture(this.shadowMapColorTextureId);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, 0);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB8, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, 0, GL11.GL_RGB, GL11.GL_FLOAT, (IntBuffer)null);
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.shadowMapBufferId);
		GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, this.shadowMapDepthTextureId, 0);
		GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, this.shadowMapColorTextureId, 0);
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
		checkFrameBufferStatus();
		GlStateManager._bindTexture(0);
		
		LOGGER.debug("Total LODs: {}", this.meshGenerator.getLodConfig().getLods().length + 1);
		LOGGER.debug("Highest detail (primary) chunk span: {}", this.meshGenerator.getLodConfig().getPrimaryChunkSpan());
		LOGGER.debug("Effective chunk span with LODs (total viewable area): {}", this.meshGenerator.getLodConfig().getEffectiveChunkSpan());
		
		switch (result.getState()) //Print crash reports if needed
		{
		case ERROR:
		{
			List<CrashReport> reports = result.createCrashReports();
			LOGGER.error("---------CRASH REPORT BEGIN---------");
			for (CrashReport report : reports)
			{
				this.mc.fillReport(report);
				LOGGER.error("{}", report.getFriendlyReport(ReportType.CRASH));
			}
			LOGGER.error("---------CRASH REPORT END---------");
			result.saveCrashReports(this.mc.gameDirectory.toPath());
			break;
		}
		default:
		}
	}
	
	private void destroyPostChains()
	{
		var iterator = this.postChains.keySet().iterator();
		while (iterator.hasNext())
		{
			iterator.next().close();
			iterator.remove();
		}
	}
	
	private @Nullable PostChain createPostChain(ResourceManager manager, ResourceLocation loc, RenderTarget target, float widthFactor, float heightFactor)
	{
		return this.createPostChain(manager, loc, target, heightFactor, heightFactor, effect -> {});
	}
	
	private @Nullable PostChain createPostChain(ResourceManager manager, ResourceLocation loc, RenderTarget target, float widthFactor, float heightFactor, Consumer<EffectInstance> passConsumer)
	{
		try
		{
			PostChain chain = new PostChain(this.mc.getTextureManager(), manager, target, loc);
			chain.resize((int)((float)this.mc.getWindow().getWidth() * widthFactor), (int)((float)this.mc.getWindow().getHeight() * heightFactor));
			for (PostPass pass : ((MixinPostChain)chain).simpleclouds$getPostPasses())
				passConsumer.accept(pass.getEffect());
			this.postChains.put(chain, Pair.of(widthFactor, heightFactor));
			return chain;
		}
		catch (JsonSyntaxException e)
		{
			LOGGER.warn("Failed to parse post shader: {}", loc, e);
		}
		catch (IOException e)
		{
			LOGGER.warn("Failed to load post shader: {}", loc, e);
		}
		
		return null;
	}
	
	public CloudMeshGenerator getMeshGenerator()
	{
		return this.meshGenerator;
	}
	
	public void onResize(int width, int height)
	{
		if (this.cloudTarget != null)
			this.cloudTarget.resize(width, height, Minecraft.ON_OSX);
		if (this.stormFogTarget != null)
		{
			this.stormFogTarget.resize(width / 4, height / 4, Minecraft.ON_OSX);
			this.stormFogTarget.setFilterMode(GL11.GL_LINEAR);
		}
		if (this.blurTarget != null)
		{
			this.blurTarget.resize(width, height, Minecraft.ON_OSX);
			this.blurTarget.setFilterMode(GL11.GL_LINEAR);
		}
		for (var entry : this.postChains.entrySet())
		{
			PostChain chain = entry.getKey();
			chain.resize((int)((float)this.mc.getWindow().getWidth() * entry.getValue().getLeft()), (int)((float)this.mc.getWindow().getHeight() * entry.getValue().getRight()));
		}
	}
		
	public void shutdown()
	{
		if (this.cloudTarget != null)
			this.cloudTarget.destroyBuffers();
		if (this.stormFogTarget != null)
			this.stormFogTarget.destroyBuffers();;
		if (this.blurTarget != null)
			this.blurTarget.destroyBuffers();
		this.cloudTarget = null;
		this.stormFogTarget = null;
		this.blurTarget = null;
		this.destroyPostChains();
		this.meshGenerator.close();
		
		if (this.shadowMapDepthTextureId != -1)
		{
			TextureUtil.releaseTextureId(this.shadowMapDepthTextureId);
			this.shadowMapBufferId = -1;
		}
		
		if (this.shadowMapBufferId != -1)
		{
			GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
			GlStateManager._glDeleteFramebuffers(this.shadowMapBufferId);
			this.shadowMapBufferId = -1;
		}
		
		if (this.lightningBoltPositions != null)
		{
			this.lightningBoltPositions.closeAndClearBinding();
			this.lightningBoltPositions = null;
		}	
	}
	
	public void tick()
	{
		if (this.needsReload)
		{
			this.onResourceManagerReload(this.mc.getResourceManager());
			this.needsReload = false;
		}
		
		this.worldEffectsManager.tick();
	}
	
	public @Nullable Matrix4f renderShadowMap(double camX, double camY, double camZ)
	{
		RenderSystem.assertOnRenderThread();
		if (this.meshGenerator.getArrayObjectId() != -1 && this.meshGenerator.getTotalIndices() > 0)
		{
			BufferUploader.reset();
			
			RenderSystem.disableBlend();
			RenderSystem.enableDepthTest();
			RenderSystem.disableCull();
			
			GL30.glBindVertexArray(this.meshGenerator.getArrayObjectId());
			
			GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.shadowMapBufferId);
			GlStateManager._viewport(0, 0, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
			GlStateManager._clearColor(0.0F, 0.0F, 0.0F, 0.0F);
			GlStateManager._clearDepth(1.0D);
			RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
			
			int span = this.meshGenerator.getLodConfig().getEffectiveChunkSpan() * SimpleCloudsConstants.CHUNK_SIZE * SimpleCloudsConstants.CLOUD_SCALE;
			PoseStack stack = new PoseStack();
			stack.setIdentity();
			stack.translate(span / 2.0D, span / 2.0D, -5000.0D);
			Vector3f direction = CloudManager.get(this.mc.level).getDirection();
			float yaw = (float)Mth.atan2((double)direction.x, (double)direction.z);
			stack.mulPose(Axis.XP.rotationDegrees(SimpleCloudsConfig.CLIENT.stormFogAngle.get().floatValue()));
			stack.mulPose(Axis.YP.rotation(yaw));
			float chunkSizeUpscaled = (float)SimpleCloudsConstants.CHUNK_SIZE * (float)SimpleCloudsConstants.CLOUD_SCALE;
			float camOffsetX = ((float)Mth.floor(camX / chunkSizeUpscaled) * chunkSizeUpscaled);
			float camOffsetZ = ((float)Mth.floor(camZ / chunkSizeUpscaled) * chunkSizeUpscaled);
			stack.translate(-camOffsetX, -(double)CloudManager.get(this.mc.level).getCloudHeight(), -camOffsetZ);
			stack.pushPose();
			this.translateClouds(stack, 0.0D, 0.0D, 0.0D);
			RenderSystem.setShader(SimpleCloudsShaders::getCloudsShadowMapShader);
			setupCloudShaderLights();
			RenderSystem.getShader().setDefaultUniforms(VertexFormat.Mode.QUADS, stack.last().pose(), this.shadowMapProjMat, this.mc.getWindow());
			RenderSystem.getShader().apply();
			RenderSystem.drawElements(GL11.GL_TRIANGLES, this.meshGenerator.getTotalIndices(), GL11.GL_UNSIGNED_INT);
			RenderSystem.getShader().clear();
			stack.popPose();
			
			GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
			
			this.mc.getMainRenderTarget().bindWrite(true);
			
			GL30.glBindVertexArray(0);
			RenderSystem.enableCull();
			
			this.shadowMapViewMat = stack.last().pose();
		}
		
		return this.shadowMapViewMat;
	}
	
	public float[] getCloudColor(float partialTick)
	{
		Vec3 cloudCol = this.mc.level.getCloudColor(partialTick);
		float factor = this.worldEffectsManager.getDarkenFactor(partialTick, 0.8F);
		float skyFlashFactor = Math.max(0.0F, ((float)this.mc.level.getSkyFlashTime() - partialTick) * SimpleCloudsConstants.LIGHTNING_FLASH_STRENGTH);
		factor += skyFlashFactor;
		float r = Mth.clamp((float)cloudCol.x * factor, 0.0F, 1.0F);
		float g = Mth.clamp((float)cloudCol.y * factor, 0.0F, 1.0F);
		float b = Mth.clamp((float)cloudCol.z * factor, 0.0F, 1.0F);
		return new float[] { r, g, b };
	}
	
	public void translateClouds(PoseStack stack, double camX, double camY, double camZ)
	{
		stack.translate(-camX, -camY + (double)CloudManager.get(this.mc.level).getCloudHeight(), -camZ);
		stack.scale((float)SimpleCloudsConstants.CLOUD_SCALE, (float)SimpleCloudsConstants.CLOUD_SCALE, (float)SimpleCloudsConstants.CLOUD_SCALE);
	}
	
	public void renderBeforeLevel(Matrix4f projMat, Matrix4f camMat, float partialTick, double camX, double camY, double camZ)
	{
		float factor = this.worldEffectsManager.getDarkenFactor(partialTick);
		float renderDistance = (float)this.meshGenerator.getCloudAreaMaxRadius() * (float)SimpleCloudsConstants.CLOUD_SCALE * factor;
		this.fogStart = renderDistance / 4.0F;
		this.fogEnd = renderDistance;
		this.meshGenerator.setCullDistance(this.fogEnd);
		
		this.mc.getProfiler().push("simple_clouds_prepare");
		if (this.meshGenerator.getArrayObjectId() != -1)
		{
			this.cullFrustum = new Frustum(camMat, projMat);
			
			if (SimpleCloudsConfig.CLIENT.generateMesh.get())
			{
				this.mc.getProfiler().push("mesh_generation");
				if (this.meshGenerator instanceof SingleRegionCloudMeshGenerator generator)
					generator.setFadeDistance((float)SimpleCloudsConfig.CLIENT.singleModeFadeStartPercentage.get() / 100.0F, (float)SimpleCloudsConfig.CLIENT.singleModeFadeEndPercentage.get() / 100.0F);
				this.setupMeshGenerator(partialTick);
				this.meshGenerator.tick(camX, camY - (double)CloudManager.get(this.mc.level).getCloudHeight(), camZ, (float)SimpleCloudsConstants.CLOUD_SCALE, SimpleCloudsConfig.CLIENT.frustumCulling.get() ? this.cullFrustum : null);
				this.mc.getProfiler().pop();
			}
			
			if (SimpleCloudsConfig.CLIENT.renderClouds.get())
				getRenderPipeline().prepare(this.mc, this, camMat, projMat, partialTick, camX, camY, camZ);
		}
		this.mc.getProfiler().pop();
	}
	
	public void renderAfterSky(Matrix4f projMat, Matrix4f camMat, float partialTick, double camX, double camY, double camZ)
	{
		this.mc.getProfiler().push("simple_clouds_after_sky");
		if (this.meshGenerator.getArrayObjectId() != -1 && SimpleCloudsConfig.CLIENT.renderClouds.get())
			getRenderPipeline().afterSky(this.mc, this, camMat, this.shadowMapViewMat, projMat, partialTick, camX, camY, camZ);
		this.mc.getProfiler().pop();
	}
	
	public void renderBeforeWeather(Matrix4f projMat, Matrix4f camMat, float partialTick, double camX, double camY, double camZ)
	{
		this.mc.getProfiler().push("simple_clouds_before_weather");
		if (this.meshGenerator.getArrayObjectId() != -1 && SimpleCloudsConfig.CLIENT.renderClouds.get())
			getRenderPipeline().beforeWeather(this.mc, this, camMat, this.shadowMapViewMat, projMat, partialTick, camX, camY, camZ);
		this.mc.getProfiler().pop();
	}
	
	public void renderAfterLevel(Matrix4f projMat, Matrix4f camMat, float partialTick, double camX, double camY, double camZ)
	{
		this.mc.getProfiler().push("simple_clouds");
		if (this.meshGenerator.getArrayObjectId() != -1 && SimpleCloudsConfig.CLIENT.renderClouds.get())
			getRenderPipeline().afterLevel(this.mc, this, camMat, this.shadowMapViewMat, projMat, partialTick, camX, camY, camZ);
		this.mc.getProfiler().pop();
		
		this.mc.getProfiler().push("world_effects");
		this.worldEffectsManager.renderPost(camMat, partialTick, camX, camY, camZ, (float)SimpleCloudsConstants.CLOUD_SCALE);
		this.mc.getProfiler().pop();
	}
	
	public void doBlurPostProcessing(float partialTick)
	{
		if (this.blurPostProcessing != null)
		{
			RenderSystem.disableDepthTest();
			RenderSystem.resetTextureMatrix();
			RenderSystem.disableBlend();
			RenderSystem.depthMask(false);
			this.blurPostProcessing.process(partialTick);
			RenderSystem.depthMask(true);
		}
	}
	
	public void doScreenSpaceWorldFog(Matrix4f camMat, Matrix4f projMat, float partialTick)
	{
		if (this.screenSpaceWorldFog != null)
		{
			RenderSystem.disableBlend();
			RenderSystem.disableDepthTest();
			RenderSystem.resetTextureMatrix();
			RenderSystem.depthMask(false);
			
			Matrix4f invertedProjMat = new Matrix4f(projMat).invert();
			Matrix4f invertedModelViewMat = new Matrix4f(camMat).invert();
			for (PostPass pass : ((MixinPostChain)this.screenSpaceWorldFog).simpleclouds$getPostPasses())
			{
				EffectInstance effect = pass.getEffect();
				effect.safeGetUniform("InverseWorldProjMat").set(invertedProjMat);
				effect.safeGetUniform("InverseModelViewMat").set(invertedModelViewMat);
				effect.safeGetUniform("FogStart").set(RenderSystem.getShaderFogStart());
				effect.safeGetUniform("FogEnd").set(RenderSystem.getShaderFogEnd());
				float[] fogCol = RenderSystem.getShaderFogColor();
				effect.safeGetUniform("FogColor").set(fogCol[0], fogCol[1], fogCol[2]);
				effect.safeGetUniform("FogShape").set(RenderSystem.getShaderFogShape().getIndex());
			}
			
			this.screenSpaceWorldFog.process(partialTick);
			
			RenderSystem.depthMask(true);
		}
	}
	
	public void doCloudPostProcessing(Matrix4f camMat, float partialTick, Matrix4f projMat)
	{
		if (this.cloudsPostProcessing != null)
		{
			RenderSystem.disableBlend();
			RenderSystem.disableDepthTest();
			RenderSystem.resetTextureMatrix();
			RenderSystem.depthMask(false);
			
			Matrix4f invertedProjMat = new Matrix4f(projMat).invert();
			Matrix4f invertedModelViewMat = new Matrix4f(camMat).invert();
			for (PostPass pass : ((MixinPostChain)this.cloudsPostProcessing).simpleclouds$getPostPasses())
			{
				EffectInstance effect = pass.getEffect();
				effect.safeGetUniform("InverseWorldProjMat").set(invertedProjMat);
				effect.safeGetUniform("InverseModelViewMat").set(invertedModelViewMat);
				effect.safeGetUniform("FogStart").set(this.fogStart);
				effect.safeGetUniform("FogEnd").set(this.fogEnd);
			}
			
			this.cloudsPostProcessing.process(partialTick);
			
			RenderSystem.depthMask(true);
		}
	}
	
	public void doStormPostProcessing(Matrix4f camMat, float partialTick, Matrix4f projMat, double camX, double camY, double camZ, float r, float g, float b)
	{
		if (this.stormPostProcessing != null && this.shadowMapViewMat != null)
		{
			RenderSystem.disableBlend();
			RenderSystem.disableDepthTest();
			RenderSystem.resetTextureMatrix();
			RenderSystem.depthMask(false);
			
			this.stormFogTarget.clear(Minecraft.ON_OSX);
			this.stormFogTarget.bindWrite(true);
			
			List<LightningBolt> lightningBolts = this.worldEffectsManager.getLightningBolts();
			int size = Math.min(lightningBolts.size(), MAX_LIGHTNING_BOLTS);
			if (size > 0)
			{
				this.lightningBoltPositions.writeData(buffer -> 
				{
					for (int i = 0; i < size; i++)
					{
						LightningBolt bolt = lightningBolts.get(i);
						Vector3f pos = bolt.getPosition();
						buffer.putFloat(pos.x);
						buffer.putFloat(pos.y);
						buffer.putFloat(pos.z);
						buffer.putFloat(bolt.getFade(partialTick));
					}
					buffer.rewind();
				}, size * BYTES_PER_LIGHTNING_BOLT);
			}
			
			Matrix4f invertedProjMat = new Matrix4f(projMat).invert();
			Matrix4f invertedModelViewMat = new Matrix4f(camMat).invert();
			for (PostPass pass : ((MixinPostChain)this.stormPostProcessing).simpleclouds$getPostPasses())
			{
				EffectInstance effect = pass.getEffect();
				effect.safeGetUniform("InverseWorldProjMat").set(invertedProjMat);
				effect.safeGetUniform("InverseModelViewMat").set(invertedModelViewMat);
				effect.safeGetUniform("ShadowProjMat").set(this.shadowMapProjMat);
				effect.safeGetUniform("ShadowModelViewMat").set(this.shadowMapViewMat);
				effect.safeGetUniform("CameraPos").set((float)camX, (float)camY, (float)camZ);
				effect.safeGetUniform("FogStart").set(this.fogEnd / 2.0F);
				effect.safeGetUniform("FogEnd").set(this.fogEnd);
				effect.safeGetUniform("ColorModulator").set(r, g, b, 1.0F);
				float factor = this.worldEffectsManager.getDarkenFactor(partialTick);
				effect.safeGetUniform("CutoffDistance").set(1000.0F * factor);
				effect.safeGetUniform("TotalLightningBolts").set(size);
			}
			
			this.stormPostProcessing.process(partialTick);
			
			RenderSystem.depthMask(true);
		}
	}
	
	public static void setupCloudShaderLights()
	{
		RenderSystem.setShaderLights(DIFFUSE_LIGHT_0, DIFFUSE_LIGHT_1);
	}
	
	public int getShadowMapTextureId()
	{
		return this.shadowMapColorTextureId;
	}
	
	public RenderTarget getBlurTarget()
	{
		return this.blurTarget;
	}
	
	public RenderTarget getStormFogTarget()
	{
		return this.stormFogTarget;
	}
	
	public RenderTarget getCloudTarget()
	{
		return this.cloudTarget;
	}
	
	public void copyDepthFromCloudsToMain()
	{
		this._copyDepthSafe(this.mc.getMainRenderTarget(), this.cloudTarget);
	}
	
	public void copyDepthFromMainToClouds()
	{
		this._copyDepthSafe(this.cloudTarget, this.mc.getMainRenderTarget());
	}
	
	public WorldEffects getWorldEffectsManager()
	{
		return this.worldEffectsManager;
	}
	
	public float getFogStart()
	{
		return this.fogStart;
	}
	
	public float getFogEnd()
	{
		return this.fogEnd;
	}
	
	public float getFadeFactorForDistance(float distance)
	{
		return 1.0F - Math.min(Math.max(distance - this.fogStart, 0.0F) / (this.fogEnd - this.fogStart), 1.0F);
	}
	
	public void fillReport(CrashReport report)
	{
		CrashReportCategory category = report.addCategory("Simple Clouds Renderer");
		category.setDetail("Cloud Mode", this.cloudMode);
		category.setDetail("Cloud Style", this.cloudStyle);
		category.setDetail("Region Generator", () -> {
			ResourceLocation key = SimpleCloudsRegistries.REGION_TYPES.getKey(this.regionGenerator);
			if (key == null)
				return "UNKNOWN";
			else
				return key.toString();
		});
		category.setDetail("Cloud Target Available", this.cloudTarget != null);
		category.setDetail("Storm Fog Target Active", this.stormFogTarget != null);
		category.setDetail("Blur Target Active", this.blurTarget != null);
		category.setDetail("Post Chains", this.postChains.toString());
		category.setDetail("Lightning Bolt SSBO", this.lightningBoltPositions);
		category.setDetail("Shadow Map Buffer ID", this.shadowMapBufferId);
		category.setDetail("Shadow Map Depth Texture ID", this.shadowMapDepthTextureId);
		category.setDetail("Shadow Map Color Texture ID", this.shadowMapColorTextureId);
		category.setDetail("Failed to copy depth buffer", this.failedToCopyDepthBuffer);
		category.setDetail("Needs Reload", this.needsReload);
		CrashReportCategory meshGenCategory = report.addCategory("Cloud Mesh Generator");
		if (this.meshGenerator != null)
		{
			meshGenCategory.setDetail("Type", this.meshGenerator.toString());
			this.meshGenerator.fillReport(meshGenCategory);
		}
		else
		{
			meshGenCategory.setDetail("Type", "Mesh generator is not initialized");
		}
	}
	
	public @Nullable GeneratorInitializeResult getInitialInitializationResult()
	{
		return this.initialInitializationResult;
	}
	
	private void _copyDepthSafe(RenderTarget to, RenderTarget from)
	{
		RenderSystem.assertOnRenderThread();
		GlStateManager._getError(); //Clear old error
		if (!this.failedToCopyDepthBuffer)
		{
			to.bindWrite(false);
			to.copyDepthFrom(from);
			if (GlStateManager._getError() != GL11.GL_INVALID_OPERATION)
				return;
			boolean enabledStencil = false;
			if (to.isStencilEnabled() && !from.isStencilEnabled())
			{
				from.enableStencil();
				enabledStencil = true;
			}
			else if (from.isStencilEnabled() && !to.isStencilEnabled())
			{
				to.enableStencil();
				enabledStencil = true;
			}
			if (enabledStencil)
			{
				to.copyDepthFrom(from);
				if (GlStateManager._getError() == GL11.GL_INVALID_OPERATION)
				{
					LOGGER.error("Unable to copy depth between the main and clouds frame buffers, even after enabling stencil. Please note that the clouds may not render properly.");
					this.failedToCopyDepthBuffer = true;
				}
				else
				{
					LOGGER.info("NOTE: Please ignore the above OpenGL error. Simple Clouds had to toggle stencil in order to copy the depth buffer between the main and clouds frame buffers.");
				}
			}
			else
			{
				LOGGER.error("Unable to copy depth between the main and clouds frame buffers. Please note that the clouds may not render properly.");
				this.failedToCopyDepthBuffer = true;
			}
		}
	}
	
	public static CloudsRenderPipeline getRenderPipeline()
	{
		return CompatHelper.areShadersRunning() ? CloudsRenderPipeline.SHADER_SUPPORT : CloudsRenderPipeline.DEFAULT;
	}
	
	public static void initialize()
	{
		RenderSystem.assertOnRenderThread();
		if (instance != null)
			throw new IllegalStateException("Simple Clouds renderer is already initialized");
		instance = new SimpleCloudsRenderer(Minecraft.getInstance());
		LOGGER.debug("Clouds render initialized");
	}
	
	public static SimpleCloudsRenderer getInstance()
	{
		return Objects.requireNonNull(instance, "Renderer not initialized!");
	}
	
	public static Optional<SimpleCloudsRenderer> getOptionalInstance()
	{
		return Optional.ofNullable(instance);
	}
	
	private static void checkFrameBufferStatus()
	{
		RenderSystem.assertOnRenderThreadOrInit();
		int i = GlStateManager.glCheckFramebufferStatus(36160);
		if (i != 36053)
		{
			if (i == 36054)
				throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
			else if (i == 36055)
				throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
			else if (i == 36059)
				throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
			else if (i == 36060)
				throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
			else if (i == 36061)
				throw new RuntimeException("GL_FRAMEBUFFER_UNSUPPORTED");
			else if (i == 1285)
				throw new RuntimeException("GL_OUT_OF_MEMORY");
			else
				throw new RuntimeException("glCheckFramebufferStatus returned unknown status:" + i);
		}
	}
	
	public static boolean canRenderInDimension(@Nullable ClientLevel level)
	{
		if (level == null)
			return false;
		
		List<? extends String> whitelist;
		boolean useAsBlacklist;
		if (ClientCloudManager.isAvailableServerSide() && SimpleCloudsConfig.SERVER_SPEC.isLoaded())
		{
			whitelist = SimpleCloudsConfig.SERVER.dimensionWhitelist.get();
			useAsBlacklist = SimpleCloudsConfig.SERVER.whitelistAsBlacklist.get();
		}
		else
		{
			whitelist = SimpleCloudsConfig.CLIENT.dimensionWhitelist.get();
			useAsBlacklist = SimpleCloudsConfig.CLIENT.whitelistAsBlacklist.get();
		}
		
		boolean flag = whitelist.stream().anyMatch(val -> {
			return level.dimension().location().toString().equals(val);
		});
		
		return useAsBlacklist ? !flag : flag;
	}
}
