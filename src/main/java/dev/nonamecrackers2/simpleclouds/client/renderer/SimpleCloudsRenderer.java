package dev.nonamecrackers2.simpleclouds.client.renderer;

import java.awt.Color;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL43;

import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.api.client.event.ModifyCloudRenderDistanceEvent;
import dev.nonamecrackers2.simpleclouds.api.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.client.cloud.ClientSideCloudTypeManager;
import dev.nonamecrackers2.simpleclouds.client.compat.SimpleCloudsCompatHelper;
import dev.nonamecrackers2.simpleclouds.client.event.impl.DetermineCloudRenderPipelineEvent;
import dev.nonamecrackers2.simpleclouds.client.framebuffer.CloudRenderTarget;
import dev.nonamecrackers2.simpleclouds.client.framebuffer.ShadowMapBuffer;
import dev.nonamecrackers2.simpleclouds.client.framebuffer.WeightedBlendingTarget;
import dev.nonamecrackers2.simpleclouds.client.mesh.RendererInitializeResult;
import dev.nonamecrackers2.simpleclouds.client.mesh.chunk.MeshChunk;
import dev.nonamecrackers2.simpleclouds.client.mesh.generator.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.mesh.generator.MultiRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.mesh.generator.SingleRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.mesh.lod.LevelOfDetailConfig;
import dev.nonamecrackers2.simpleclouds.client.mesh.lod.PreparedChunk;
import dev.nonamecrackers2.simpleclouds.client.renderer.lightning.LightningBolt;
import dev.nonamecrackers2.simpleclouds.client.renderer.pipeline.CloudsRenderPipeline;
import dev.nonamecrackers2.simpleclouds.client.renderer.settings.CloudsRendererSettings;
import dev.nonamecrackers2.simpleclouds.client.shader.SimpleCloudsShaders;
import dev.nonamecrackers2.simpleclouds.client.shader.SingleSSBOShaderInstance;
import dev.nonamecrackers2.simpleclouds.client.shader.buffer.BindingManager;
import dev.nonamecrackers2.simpleclouds.client.shader.buffer.ShaderStorageBufferObject;
import dev.nonamecrackers2.simpleclouds.client.world.ClientCloudManager;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudGetter;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.mixin.MixinPostChain;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.ImmediateWindowHandler;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.neoforge.common.NeoForge;
import nonamecrackers2.crackerslib.common.compat.CompatHelper;

public class SimpleCloudsRenderer implements ResourceManagerReloadListener
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/SimpleCloudsRenderer");
	private static final Vector3f DIFFUSE_LIGHT_0 = (new Vector3f(0.2F, 1.0F, -0.7F)).normalize();
	private static final Vector3f DIFFUSE_LIGHT_1 = (new Vector3f(-0.2F, 1.0F, 0.7F)).normalize();
	private static final ResourceLocation STORM_POST_PROCESSING_LOC = SimpleCloudsMod.id("shaders/post/storm_post.json");
	private static final ResourceLocation BLUR_POST_PROCESSING_LOC = ResourceLocation.withDefaultNamespace("shaders/post/blur.json");//SimpleCloudsMod.id("shaders/post/blur_post.json");
	private static final ResourceLocation SCREEN_SPACE_WORLD_FOG_LOC = SimpleCloudsMod.id("shaders/post/screen_space_world_fog.json");
	private static final ResourceLocation CLOUD_SHADOWS_LOC = SimpleCloudsMod.id("shaders/post/cloud_shadows.json");
	public static final ResourceLocation FINAL_COMPOSITE_LOC = SimpleCloudsMod.id("shaders/post/final_composite.json");
	public static final ResourceLocation FINAL_COMPOSITE_NO_TRANSPARENCY_LOC = SimpleCloudsMod.id("shaders/post/final_composite_no_transparency.json");
	private static final ResourceLocation DITHER_TEXTURE = SimpleCloudsMod.id("textures/shader/bayer_matrix.png");
	private static final ArtifactVersion REQUIRED_OPENGL_VERSION = new DefaultArtifactVersion("4.3");
	public static final int SHADOW_MAP_SIZE = 1024;
	public static final int SHADOW_MAP_SPAN = 10000;
	public static final int MAX_LIGHTNING_BOLTS = 16;
	public static final int BYTES_PER_LIGHTNING_BOLT = 16;
	public static final float CHUNK_FADE_IN_ALPHA_PER_TICK = 0.2F;
	public static final float DITHER_SCALE = 0.05F;
	private static @Nullable SimpleCloudsRenderer instance;
	private final CloudsRendererSettings settings;
	private final Minecraft mc;
	private final WorldEffects worldEffectsManager;
	private final AtmosphericCloudsRenderHandler atmoshpericClouds;
	private @Nullable ClientCloudManager cloudManager;
	private ArtifactVersion openGlVersion;
	private CloudMeshGenerator meshGenerator;
	private @Nullable CloudsRenderPipeline renderPipelineThisPass;
	private @Nullable RenderTarget cloudTarget;
	private @Nullable WeightedBlendingTarget cloudTransparencyTarget;
	private @Nullable RenderTarget stormFogTarget;
	private int stormFogResolutionDivisor = 4;
	private @Nullable RenderTarget blurTarget;
	private final List<PostChain> postChains = Lists.newArrayList();
	private @Nullable PostChain finalComposite;
	private @Nullable PostChain stormPostProcessing;
	private @Nullable PostChain blurPostProcessing;
	private @Nullable PostChain screenSpaceWorldFog;
	private @Nullable PostChain cloudShadows;
	private @Nullable ShaderStorageBufferObject lightningBoltPositions;
	private @Nullable ShadowMapBuffer stormFogShadowMap;
	private Optional<ShadowMapBuffer> shadowMap = Optional.empty();
	private @Nullable Frustum cullFrustum;
	private float fogStart;
	private float fogEnd;
	private @Nullable Matrix4f stormFogShadowMapMatrix;
	private @Nullable Matrix4f shadowMapMatrix;
	private boolean failedToCopyDepthBuffer;
	private boolean needsReload;
	private @Nullable RendererInitializeResult initialInitializationResult;
	
	private SimpleCloudsRenderer(CloudsRendererSettings settings, Minecraft mc)
	{
		this.settings = settings;
		this.mc = mc;
		this.worldEffectsManager = new WorldEffects(mc, this);
		this.atmoshpericClouds = new AtmosphericCloudsRenderHandler(mc);
	}
	
	public String getClientCloudManagerString()
	{
		return this.cloudManager != null ? this.cloudManager.toString() : "null";
	}
	
	public CloudMeshGenerator getMeshGenerator()
	{
		return this.meshGenerator;
	}
	
	public CloudsRenderPipeline getRenderPipeline()
	{
		return Objects.requireNonNull(this.renderPipelineThisPass, "Pipeline not determined");
	}
	
	public WorldEffects getWorldEffectsManager()
	{
		return this.worldEffectsManager;
	}
	
	public AtmosphericCloudsRenderHandler getAtmosphericCloudRenderer()
	{
		return this.atmoshpericClouds;
	}
	
	public CloudsRendererSettings getSettings()
	{
		return this.settings;
	}
	
	public @Nullable RendererInitializeResult getInitialInitializationResult()
	{
		return this.initialInitializationResult;
	}
	
	public ShadowMapBuffer getStormFogShadowMap()
	{
		return this.stormFogShadowMap;
	}
	
	public Optional<ShadowMapBuffer> getShadowMap()
	{
		return this.shadowMap;
	}
	
	public @Nullable Matrix4f getStormFogShadowMatrix()
	{
		return this.stormFogShadowMapMatrix;
	}
	
	public @Nullable Matrix4f getShadowMapMatrix()
	{
		return this.shadowMapMatrix;
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
	
	public WeightedBlendingTarget getCloudTransparencyTarget()
	{
		return this.cloudTransparencyTarget;
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
	
	public @Nullable Frustum getCullFrustum()
	{
		return this.cullFrustum;
	}
	
	public void onCloudManagerChange(ClientCloudManager manager)
	{
		this.cloudManager = manager;
		if (this.meshGenerator instanceof MultiRegionCloudMeshGenerator generator)
			generator.setCloudGetter(manager);
	}
	
	private void prepareMeshGenerator(float partialTicks)
	{
		if (this.meshGenerator instanceof SingleRegionCloudMeshGenerator generator)
			generator.setFadeDistances((float)SimpleCloudsConfig.CLIENT.singleModeFadeStartPercentage.get() / 100.0F, (float)SimpleCloudsConfig.CLIENT.singleModeFadeEndPercentage.get() / 100.0F);
		this.meshGenerator.setTransparencyRenderDistance((float)SimpleCloudsConfig.CLIENT.transparencyRenderDistancePercentage.get() / 100.0F);
		this.meshGenerator.setTestFacesFacingAway(SimpleCloudsConfig.CLIENT.testSidesThatAreOccluded.get());
		if (this.mc.level != null)
		{
			this.meshGenerator.setScroll(this.cloudManager.getScrollX(partialTicks), this.cloudManager.getScrollY(partialTicks), this.cloudManager.getScrollZ(partialTicks));
		}
	}
	
	public boolean needsReinitialization()
	{
		return this.settings.needsReinitialization(this.meshGenerator);
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
		
		this.initialInitializationResult = null;
		
		// --- Check OpenGL version ---
		
		ArtifactVersion openGlVersion = this.openGlVersion;
		if (openGlVersion == null)
			openGlVersion = new DefaultArtifactVersion(ImmediateWindowHandler.getGLVersion());
		if (openGlVersion.compareTo(REQUIRED_OPENGL_VERSION) < 0)
		{
			LOGGER.error("Simple Clouds renderer could not initialize. OpenGL version is {}, minimum required is {}", openGlVersion, REQUIRED_OPENGL_VERSION);
			this.initialInitializationResult = RendererInitializeResult.builder().errorOpenGL().build();
			this.openGlVersion = openGlVersion;
			return;
		}
		
		if (!SimpleCloudsShaders.areShadersInitialized())
		{
			LOGGER.error("Simple Clouds renderer could not initialize. Core shaders are not initialized.");
			this.initialInitializationResult = RendererInitializeResult.builder().coreShadersNotInitialized(SimpleCloudsShaders.getError()).build();
			saveAndPrintCrashReports(this.mc, this.initialInitializationResult);
			return;
		}
		
		RendererInitializeResult compatError = SimpleCloudsCompatHelper.findCompatErrors();
		if (compatError.getState() == RendererInitializeResult.State.ERROR)
		{
			LOGGER.error("Simple Clouds renderer could not initialize due to compat error(s): {}", compatError.getErrors().stream().map(e -> e.text().getString()).toList());
			this.initialInitializationResult = compatError;
			return;
		}
		
		StartupNotificationManager.addModMessage("Initializing Simple Clouds renderer");
		
		LOGGER.debug("OpenGL {}", openGlVersion);
		
		Instant started = Instant.now();
		
		LOGGER.debug("Beginning Simple Clouds renderer initialization");
		
		this.failedToCopyDepthBuffer = false;
		
		// --- Render Targets ---
		
		boolean highPrecisionDepth = SimpleCloudsMod.dhLoaded();
		
		RenderTarget main = SimpleCloudsCompatHelper.getMainRenderTarget();
		if (main == null)
		{
			this.initialInitializationResult = RendererInitializeResult.builder().errorUnknown(new NullPointerException("Main framebuffer is null"), "Simple Clouds Renderer").build();
			saveAndPrintCrashReports(this.mc, this.initialInitializationResult);
			return;
		}
		
		
		if (this.cloudTarget != null)
			this.cloudTarget.destroyBuffers();
		this.cloudTarget = new CloudRenderTarget(main.width, main.height, Minecraft.ON_OSX, highPrecisionDepth);
		this.cloudTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
		
		if (this.cloudTransparencyTarget != null)
			this.cloudTransparencyTarget.destroyBuffers();
		this.cloudTransparencyTarget = new WeightedBlendingTarget(main.width, main.height, Minecraft.ON_OSX, highPrecisionDepth);
		
		this.stormFogResolutionDivisor = SimpleCloudsCompatHelper.getStormFogResolutionDivisor();
		if (this.stormFogTarget != null)
			this.stormFogTarget.destroyBuffers();
		this.stormFogTarget = new TextureTarget(main.width / this.stormFogResolutionDivisor, main.height / this.stormFogResolutionDivisor, false, Minecraft.ON_OSX);
		this.stormFogTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
		this.stormFogTarget.setFilterMode(GL11.GL_LINEAR);
		
		if (this.blurTarget != null)
			this.blurTarget.destroyBuffers();
		this.blurTarget = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
		this.blurTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
		this.blurTarget.setFilterMode(GL11.GL_LINEAR);
		
		// --- Mesh Generator ---
		
		this.setupMeshGenerator(); // Create/setup the generator
		this.prepareMeshGenerator(0.0F); // Prepare it
		
		RendererInitializeResult result = this.meshGenerator.init(manager); // Initialize
		if (this.initialInitializationResult == null)
			this.initialInitializationResult = result;
		
		// --- Shadow Map ---
		
		if (this.stormFogShadowMap != null)
		{
			this.stormFogShadowMap.close();
			this.stormFogShadowMap = null;
		}
		
		this.shadowMap.ifPresent(buffer -> {
			buffer.close();
		});
		
		int span = this.meshGenerator.getLodConfig().getEffectiveChunkSpan() * SimpleCloudsConstants.CHUNK_SIZE * SimpleCloudsConstants.CLOUD_SCALE;
		this.stormFogShadowMap = new ShadowMapBuffer(span, span, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, 0.0F, 10000.0F, true, false);
		
		if (SimpleCloudsConfig.CLIENT.distantShadows.get() && SimpleCloudsMod.dhLoaded())
		{
			int distantShadowSpan = SimpleCloudsConfig.CLIENT.shadowDistance.get() * 2;
			distantShadowSpan = Math.min(distantShadowSpan, span);
			this.shadowMap = Optional.of(new ShadowMapBuffer(distantShadowSpan, distantShadowSpan, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, 0.0F, 10000.0F, false, true));
		}
		else
		{
			this.shadowMap = Optional.empty();
		}
		
		// --- Post Processing Shaders ---
		
		this.destroyPostChains();
		
		if (this.lightningBoltPositions != null)
		{
			BindingManager.freeSSBO(this.lightningBoltPositions);
			this.lightningBoltPositions = null;
		}
		
		this.lightningBoltPositions = BindingManager.createSSBO(GL15.GL_DYNAMIC_DRAW);
		this.lightningBoltPositions.allocateBuffer(MAX_LIGHTNING_BOLTS * BYTES_PER_LIGHTNING_BOLT);
		
		this.stormPostProcessing = this.createPostChain(manager, STORM_POST_PROCESSING_LOC, this.stormFogTarget, pass -> 
		{
			EffectInstance effect = pass.getEffect();
			effect.setSampler("ShadowMap", () -> this.stormFogShadowMap.getDepthTexId());
			effect.setSampler("ShadowMapColor", () -> this.stormFogShadowMap.getColorTexId());
			effect.setSampler("DepthSampler", () -> this.cloudTarget.getDepthTextureId());
			this.lightningBoltPositions.optionalBindToProgram("LightningBolts", effect.getId());
		});
		
		this.blurPostProcessing = this.createPostChain(manager, BLUR_POST_PROCESSING_LOC, this.blurTarget);
		
		this.screenSpaceWorldFog = this.createPostChain(manager, SCREEN_SPACE_WORLD_FOG_LOC, main, pass -> 
		{
			EffectInstance effect = pass.getEffect();
			effect.setSampler("StormFogSampler", () -> this.blurTarget.getColorTextureId());
			effect.setSampler("CloudDepthSampler", () -> this.cloudTarget.getDepthTextureId());
		});
		
		this.finalComposite = this.createPostChain(manager, this.settings.useTransparency() ? FINAL_COMPOSITE_LOC : FINAL_COMPOSITE_NO_TRANSPARENCY_LOC, main, pass -> 
		{
			EffectInstance effect = pass.getEffect();
			if (this.settings.useTransparency())
			{
				effect.setSampler("AccumTexture", () -> this.cloudTransparencyTarget.getColorTextureId());
				effect.setSampler("RevealageTexture", () -> this.cloudTransparencyTarget.getRevealageTextureId());
			}
			effect.setSampler("CloudsTexture", () -> this.cloudTarget.getColorTextureId());
		});
		
		if (this.shadowMap.isPresent())
		{
			ShadowMapBuffer map = this.shadowMap.get();
			this.cloudShadows = this.createPostChain(manager, CLOUD_SHADOWS_LOC, main, pass -> 
			{
				EffectInstance effect = pass.getEffect();
				effect.setSampler("ShadowMap", () -> this.shadowMap.get().getDepthTexId());
				effect.safeGetUniform("ShadowSpan").set((float)Math.min(map.getViewWidth(), map.getViewHeight()));
			});
		}
		
		this.atmoshpericClouds.init(manager);
		
		// --- Final debug ---
		
		long duration = Duration.between(started, Instant.now()).toMillis();
		LOGGER.info("Finished initialization, took {} ms", duration);
		
		LOGGER.debug("Total LODs: {}", this.meshGenerator.getLodConfig().getLods().length + 1);
		LOGGER.debug("Highest detail (primary) chunk span: {}", this.meshGenerator.getLodConfig().getPrimaryChunkSpan());
		LOGGER.debug("Effective chunk span with LODs (total viewable area): {}", this.meshGenerator.getLodConfig().getEffectiveChunkSpan());
		LOGGER.debug("Total span in blocks: {}", this.meshGenerator.getLodConfig().getEffectiveChunkSpan() * SimpleCloudsConstants.CHUNK_SIZE * SimpleCloudsConstants.CLOUD_SCALE);
		
		//Print crash reports if needed
		saveAndPrintCrashReports(this.mc, result);
	}
	
	private static void saveAndPrintCrashReports(Minecraft mc, RendererInitializeResult result)
	{
		switch (result.getState())
		{
		case ERROR:
		{
			List<CrashReport> reports = result.createCrashReports();
			LOGGER.error("---------CRASH REPORT BEGIN---------");
			for (CrashReport report : reports)
			{
				mc.fillReport(report);
				LOGGER.error("{}", report.getFriendlyReport(ReportType.CRASH));
			}
			LOGGER.error("---------CRASH REPORT END---------");
			result.saveCrashReports(mc.gameDirectory.toPath());
			break;
		}
		default:
		}
	}
	
	private void setupMeshGenerator()
	{
		if (this.settings.checkAndOrBeginInitialization(this.meshGenerator))
		{
			if (this.meshGenerator != null)
			{
				this.meshGenerator.close(); //Close the current generator
				this.meshGenerator = null;
			}
			
			CloudMode mode = this.settings.getCurrentCloudMode();
			boolean isAmbientMode = mode == CloudMode.AMBIENT;
			boolean useMultiRegion = isAmbientMode || mode == CloudMode.DEFAULT;
			boolean shadedClouds = this.settings.shadedClouds();
			boolean useFixedMeshDataSectionSize = this.settings.useFixedMeshDataSectionSize();
			boolean useTransparency = this.settings.useTransparency();
			LevelOfDetailConfig lod = this.settings.getCurrentLod().getConfig();
			
			var builder = CloudMeshGenerator.builder()
					.fadeNearOrigin(isAmbientMode)
					.shadedClouds(shadedClouds)
					.fixedMeshDataSectionSize(useFixedMeshDataSectionSize)
					.meshGenInterval(SimpleCloudsRenderer::calculateMeshGenInterval)
					.lodConfig(lod)
					.useTransparency(useTransparency);
			
			if (useMultiRegion) //Use the multi-region generator for DEFAULT or AMBIENT cloud mode
			{
				if (isAmbientMode)
				{
					builder.fadeStart(SimpleCloudsConstants.AMBIENT_MODE_FADE_START)
						.fadeEnd(SimpleCloudsConstants.AMBIENT_MODE_FADE_END);
				}
				this.meshGenerator = builder.createMultiRegion();
			}
			else if (mode == CloudMode.SINGLE)
			{
				float fadeStart = (float)SimpleCloudsConfig.CLIENT.singleModeFadeStartPercentage.get() / 100.0F;
				float fadeEnd = (float)SimpleCloudsConfig.CLIENT.singleModeFadeEndPercentage.get() / 100.0F;
				this.meshGenerator = builder.fadeStart(fadeStart).fadeEnd(fadeEnd).createSingleRegion(SimpleCloudsConstants.EMPTY);
			}
			else
			{
				throw new IllegalArgumentException("Not sure how to handle cloud mode " + mode);
			}
		}
		
		if (this.meshGenerator instanceof MultiRegionCloudMeshGenerator multiRegionGenerator)
		{
			multiRegionGenerator.setCloudGetter(this.cloudManager != null ? this.cloudManager : CloudGetter.EMPTY);
		}
		else if (this.meshGenerator instanceof SingleRegionCloudMeshGenerator singleRegionGenerator)
		{
			//Find the desired single mode cloud type, either from the client-side only context or
			//from the synced cloud types from the server
			CloudType type = this.settings.getSingleModeCloudType();
			if (!ClientCloudManager.isAvailableServerSide() && !ClientSideCloudTypeManager.isValidClientSideSingleModeCloudType(type))
				type = SimpleCloudsConstants.EMPTY;
			if (type == null)
				type = SimpleCloudsConstants.EMPTY;
			singleRegionGenerator.setCloudType(type);
		}
		else
		{
			throw new IllegalArgumentException("Not sure how to handle generator: " + this.meshGenerator);
		}
	}
	
	private void destroyPostChains()
	{
		this.postChains.forEach(PostChain::close);
		this.postChains.clear();
	}
	
	private @Nullable PostChain createPostChain(ResourceManager manager, ResourceLocation loc, RenderTarget target)
	{
		return this.createPostChain(manager, loc, target, effect -> {});
	}
	
	private @Nullable PostChain createPostChain(ResourceManager manager, ResourceLocation loc, RenderTarget target, Consumer<PostPass> passConsumer)
	{
		try
		{
			PostChain chain = new PostChain(this.mc.getTextureManager(), manager, target, loc);
			chain.resize(target.width, target.height);
			for (PostPass pass : ((MixinPostChain)chain).simpleclouds$getPostPasses())
				passConsumer.accept(pass);
			this.postChains.add(chain);
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
	
	public void onMainWindowResize(int width, int height)
	{
		this.atmoshpericClouds.onResize(width, height);
		
		RenderTarget main = SimpleCloudsCompatHelper.getMainRenderTarget();
		if (main == null)
			return;
		
		width = main.width;
		height = main.height;
		
		if (this.cloudTarget != null)
			this.cloudTarget.resize(width, height, Minecraft.ON_OSX);
		
		if (this.cloudTransparencyTarget != null)
			this.cloudTransparencyTarget.resize(width, height, Minecraft.ON_OSX);
		
		this.stormFogResolutionDivisor = SimpleCloudsCompatHelper.getStormFogResolutionDivisor();
		
		if (this.stormFogTarget != null)
		{
			this.stormFogTarget.resize(width / this.stormFogResolutionDivisor, height / this.stormFogResolutionDivisor, Minecraft.ON_OSX);
			this.stormFogTarget.setFilterMode(GL11.GL_LINEAR);
		}
		
		if (this.blurTarget != null)
		{
			this.blurTarget.resize(width, height, Minecraft.ON_OSX);
			this.blurTarget.setFilterMode(GL11.GL_LINEAR);
		}
		
		for (PostChain chain : this.postChains)
		{
			RenderTarget chainTarget = ((MixinPostChain)chain).simpleclouds$getScreenTarget();
			chain.resize(chainTarget.width, chainTarget.height);
		}
	}
		
	public void shutdown()
	{
		if (this.cloudTarget != null)
			this.cloudTarget.destroyBuffers();
		if (this.cloudTransparencyTarget != null)
			this.cloudTransparencyTarget.destroyBuffers();
		if (this.stormFogTarget != null)
			this.stormFogTarget.destroyBuffers();;
		if (this.blurTarget != null)
			this.blurTarget.destroyBuffers();
		
		this.cloudTarget = null;
		this.cloudTransparencyTarget = null;
		this.stormFogTarget = null;
		this.blurTarget = null;
		
		this.destroyPostChains();
		
		if (this.meshGenerator != null)
			this.meshGenerator.close();
		
		if (this.stormFogShadowMap != null)
		{
			this.stormFogShadowMap.close();
			this.stormFogShadowMap = null;
		}
		
		if (this.shadowMap.isPresent())
		{
			this.shadowMap.get().close();
			this.shadowMap = Optional.empty();
		}
		
		if (this.lightningBoltPositions != null)
		{
			BindingManager.freeSSBO(this.lightningBoltPositions);
			this.lightningBoltPositions = null;
		}
		
		this.atmoshpericClouds.close();
	}
	
	public void baseTick()
	{
		if (this.needsReload)
		{
			this.onResourceManagerReload(this.mc.getResourceManager());
			this.needsReload = false;
		}
	}
	
	public void tick()
	{
		this.worldEffectsManager.tick();
		
		if (this.cloudManager != null)
			this.atmoshpericClouds.setWindDirection(this.cloudManager.calculateWindDirection());
		this.atmoshpericClouds.tick();
		
		if (this.meshGenerator != null)
			this.meshGenerator.worldTick();
	}
	
	public static void renderCloudsOpaque(CloudMeshGenerator generator, PoseStack stack, Matrix4f projMat, float fogStart, float fogEnd, float partialTick, float r, float g, float b, @Nullable Frustum frustum)
	{
		renderCloudsOpaque(generator, stack, projMat, fogStart, fogEnd, partialTick, r, g, b, frustum, true);
	}
	
	public static void renderCloudsOpaque(CloudMeshGenerator generator, PoseStack stack, Matrix4f projMat, float fogStart, float fogEnd, float partialTick, float r, float g, float b, @Nullable Frustum frustum, boolean ditherFade)
	{
		RenderSystem.assertOnRenderThread();
		
		BufferUploader.reset();
		
		if (!generator.canRender())
			return;
		
		RenderSystem.disableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		
		SingleSSBOShaderInstance shader = SimpleCloudsShaders.getCloudsShader();
		RenderSystem.setShader(() -> shader);
		
		TextureManager manager = Minecraft.getInstance().getTextureManager();
		AbstractTexture ditherTexture = manager.getTexture(DITHER_TEXTURE);
		shader.setSampler("BayerMatrixSampler", ditherTexture);
		shader.safeGetUniform("DitherScale").set(DITHER_SCALE);
		
		SimpleCloudsRenderer.prepareShader(shader, stack.last().pose(), projMat, fogStart, fogEnd);
		shader.apply();
		
		generator.forRenderableMeshChunks(frustum, MeshChunk::getOpaqueBuffers, (chunk, opaqueBuffers) -> 
		{
			if (ditherFade)
			{
				RenderSystem.setShaderColor(r, g, b, chunk.getAlpha(partialTick));
				shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
				shader.COLOR_MODULATOR.upload();
			}
			GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, shader.getShaderStorageBinding(), opaqueBuffers.getBufferId());
			generator.getSideMesh().drawInstanced(opaqueBuffers.getElementCount());
		}, ditherFade);
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, shader.getShaderStorageBinding(), 0);
		
		shader.clear();
		
		GL30.glBindVertexArray(0);
		
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableCull();
	}
	
	public static void renderCloudsTransparency(CloudMeshGenerator generator, PoseStack stack, Matrix4f projMat, float fogStart, float fogEnd, float partialTick, float r, float g, float b, @Nullable Frustum frustum)
	{
		renderCloudsTransparency(generator, stack, projMat, fogStart, fogEnd, partialTick, r, g, b, frustum, true);
	}
	
	public static void renderCloudsTransparency(CloudMeshGenerator generator, PoseStack stack, Matrix4f projMat, float fogStart, float fogEnd, float partialTick, float r, float g, float b, @Nullable Frustum frustum, boolean ditherFade)
	{
		RenderSystem.assertOnRenderThread();
		
		BufferUploader.reset();
		
		if (!generator.canRender() || !generator.transparencyEnabled())
			return;
		
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(false);
		
		SingleSSBOShaderInstance shader = SimpleCloudsShaders.getCloudsTransparencyShader();
		RenderSystem.setShader(() -> shader);
		
		TextureManager manager = Minecraft.getInstance().getTextureManager();
		AbstractTexture ditherTexture = manager.getTexture(DITHER_TEXTURE);
		shader.setSampler("BayerMatrixSampler", ditherTexture);
		shader.safeGetUniform("DitherScale").set(DITHER_SCALE);
		
		SimpleCloudsRenderer.prepareShader(shader, stack.last().pose(), projMat, fogStart, fogEnd);
		
		shader.apply();
		
		GL30.glEnablei(GL11.GL_BLEND, 0);
		GL30.glEnablei(GL11.GL_BLEND, 1);
		GL40.glBlendEquationi(0, GL14.GL_FUNC_ADD);
		GL40.glBlendEquationi(1, GL14.GL_FUNC_ADD);
		GL40.glBlendFunci(0, GL11.GL_ONE, GL11.GL_ONE);
		GL40.glBlendFunci(1, GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR);
		
		generator.forRenderableMeshChunks(frustum, c -> c.getTransparentBuffers().get(), (chunk, transparentBuffers) -> 
		{
			if (ditherFade)
			{
				RenderSystem.setShaderColor(r, g, b, chunk.getAlpha(partialTick));
				shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
				shader.COLOR_MODULATOR.upload();
			}
			
			GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, shader.getShaderStorageBinding(), transparentBuffers.getBufferId());
			generator.getCubeMesh().drawInstanced(transparentBuffers.getElementCount());
		}, ditherFade);
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, shader.getShaderStorageBinding(), 0);
		
		shader.clear();
		
		GL30.glDisablei(GL11.GL_BLEND, 0);
		GL30.glDisablei(GL11.GL_BLEND, 1);
		GL40.glBlendFuncSeparatei(0, GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		GL40.glBlendFuncSeparatei(1, GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		
		GL30.glBindVertexArray(0);

		RenderSystem.depthMask(true);
		
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}
	
	private Matrix4f createShadowMapMatrix(ShadowMapBuffer shadowMap, double camX, double camY, double camZ, Consumer<Matrix4f> transformApplier)
	{
		Matrix4f mat = new Matrix4f().identity();
		float depthCenter = (shadowMap.getNear() + shadowMap.getFar()) * -0.5F;
		mat.translate(shadowMap.getViewWidth() / 2.0F, shadowMap.getViewHeight() / 2.0F, depthCenter);
		transformApplier.accept(mat);
		float chunkSizeUpscaled = (float)SimpleCloudsConstants.CHUNK_SIZE * (float)SimpleCloudsConstants.CLOUD_SCALE;
		float camOffsetX = ((float)Mth.floor(camX / chunkSizeUpscaled) * chunkSizeUpscaled);
		float camOffsetZ = ((float)Mth.floor(camZ / chunkSizeUpscaled) * chunkSizeUpscaled);
		mat.translate(-camOffsetX, -this.cloudManager.getCloudHeight(), -camOffsetZ);
		return mat;
	}
	
	private void renderShadowMap(ShadowMapBuffer shadowMap, Matrix4f mat, SingleSSBOShaderInstance shader, @Nullable Frustum frustum)
	{
		RenderSystem.assertOnRenderThread();
		
		PoseStack stack = new PoseStack();
		stack.mulPose(mat);
		
		this.translateClouds(stack, 0.0D, 0.0D, 0.0D);
		
		RenderSystem.setShader(() -> shader);
		prepareShader(shader, stack.last().pose(), shadowMap.getProjMatrix(), this.fogStart, this.fogEnd);
		shader.apply();
		
		shadowMap.bind();
		shadowMap.clear(Minecraft.ON_OSX);
		
		this.meshGenerator.forRenderableMeshChunks(frustum, MeshChunk::getOpaqueBuffers, (chunk, opaqueBuffers) -> 
		{
			GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, shader.getShaderStorageBinding(), opaqueBuffers.getBufferId());
			this.meshGenerator.getSideMesh().drawInstanced(opaqueBuffers.getElementCount());
		});
		GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, shader.getShaderStorageBinding(), 0);
		GL30.glBindVertexArray(0);
		
		shadowMap.unbind();
		
		shader.clear();
	}
	
	private float determineShadowMapAngle(float partialTick)
	{
		float timeOfDay = this.mc.level.getTimeOfDay(partialTick);
		return 45.0F * Mth.sin(2.0F * (float)Math.PI * timeOfDay);
	}
	
	private void renderShadowMaps(double camX, double camY, double camZ, float partialTick)
	{
		RenderSystem.assertOnRenderThread();
		
		BufferUploader.reset();
		
		RenderSystem.disableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableCull();
		
		this.stormFogShadowMapMatrix = this.createShadowMapMatrix(this.stormFogShadowMap, camX, camY, camZ, mat -> 
		{
			Vector2f direction = this.cloudManager.calculateWindDirection();
			float yaw = (float)Mth.atan2((double)direction.x, (double)direction.y);
			mat.rotate(Axis.XP.rotationDegrees(SimpleCloudsConfig.CLIENT.stormFogAngle.get().floatValue()));
			mat.rotate(Axis.YP.rotation(yaw));
		});
		this.renderShadowMap(this.stormFogShadowMap, this.stormFogShadowMapMatrix, SimpleCloudsShaders.getStormFogShadowMapShader(), this.cullFrustum);
		
		this.shadowMapMatrix = this.shadowMap.map(buffer -> 
		{
			Matrix4f mat = this.createShadowMapMatrix(buffer, camX, camY, camZ, m -> {
				m.rotate(Axis.XP.rotationDegrees(90.0F));
				m.rotate(Axis.ZN.rotationDegrees(this.determineShadowMapAngle(partialTick)));
			});
			this.renderShadowMap(buffer, mat, SimpleCloudsShaders.getCloudsShadowMapShader(), null);
			return mat;
		}).orElse(null);
		
		RenderSystem.enableCull();
		
		this.mc.getMainRenderTarget().bindWrite(true);
	}
	
	public static void renderCloudsDebug(CloudMeshGenerator generator, PoseStack stack, Matrix4f projMat, float partialTick, float fogStart, float fogEnd, @Nullable Frustum frustum, boolean chunkBoundaries, boolean noiseBoundaries)
	{
		RenderSystem.assertOnRenderThread();
		
		if (!generator.canRender())
			return;
		
		BufferUploader.reset();
		
		RenderSystem.disableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableCull();
		
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder builderOutline = tesselator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
		
		generator.forRenderableMeshChunks(frustum, MeshChunk::getOpaqueBuffers, (chunk, bufferSet) -> 
		{
			PreparedChunk preparedChunk = chunk.getChunkInfo();
			if (chunkBoundaries)
			{
				int color = Color.HSBtoRGB((float)preparedChunk.lodLevel() / ((float)generator.getLodConfig().getLods().length + 1), 1.0F, 1.0F);
				float r = (float)FastColor.ARGB32.red(color) / 255.0F;
				float g = (float)FastColor.ARGB32.green(color) / 255.0F;
				float b = (float)FastColor.ARGB32.blue(color) / 255.0F;
				LevelRenderer.renderLineBox(builderOutline, chunk.getBoundsMinX() + 1.0F, chunk.getBoundsMinY() + 1.0F, chunk.getBoundsMinZ() + 1.0F, chunk.getBoundsMaxX() - 1.0F, chunk.getBoundsMaxY() - 1.0F, chunk.getBoundsMaxZ() - 1.0F, r, g, b, 1.0F);
			}
			if (noiseBoundaries)
				LevelRenderer.renderLineBox(builderOutline, chunk.getBoundsMinX() + 1.0F, chunk.getMinHeight() + 1.0F, chunk.getBoundsMinZ() + 1.0F, chunk.getBoundsMaxX() - 1.0F, chunk.getMaxHeight() - 1.0F, chunk.getBoundsMaxZ() - 1.0F, 1.0F, 1.0F, 0.0F, 1.0F);
		});
		
		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		ShaderInstance shader = RenderSystem.getShader();
		SimpleCloudsRenderer.prepareShader(shader, stack.last().pose(), projMat, fogStart, fogEnd);
		shader.LINE_WIDTH.set(2.5F);
		shader.FOG_START.set(Float.MAX_VALUE);
		shader.apply();
		MeshData data = builderOutline.build();
		if (data != null)
			BufferUploader.draw(data);
		shader.clear();
		
		RenderSystem.enableCull();
		
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableBlend();
		
		BufferBuilder builderSolid = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		
		generator.forRenderableMeshChunks(frustum, MeshChunk::getOpaqueBuffers, (chunk, bufferSet) -> 
		{
			PreparedChunk preparedChunk = chunk.getChunkInfo();
			if (chunkBoundaries)
			{
				int color = Color.HSBtoRGB((float)preparedChunk.lodLevel() / ((float)generator.getLodConfig().getLods().length + 1), 1.0F, 1.0F);
				float r = (float)FastColor.ARGB32.red(color) / 255.0F;
				float g = (float)FastColor.ARGB32.green(color) / 255.0F;
				float b = (float)FastColor.ARGB32.blue(color) / 255.0F;
				renderChunkBox(builderSolid, chunk.getBoundsMinX() + 1.0F, chunk.getBoundsMinY() + 1.0F, chunk.getBoundsMinZ() + 1.0F, chunk.getBoundsMaxX() - 1.0F, chunk.getBoundsMaxY() - 1.0F, chunk.getBoundsMaxZ() - 1.0F, r, g, b, 0.4F);
			}
			if (noiseBoundaries)
				renderChunkBox(builderSolid, chunk.getBoundsMinX() + 1.0F, chunk.getMinHeight() + 1.0F, chunk.getBoundsMinZ() + 1.0F, chunk.getBoundsMaxX() - 1.0F, chunk.getMaxHeight() - 1.0F, chunk.getBoundsMaxZ() - 1.0F, 1.0F, 1.0F, 0.0F, 0.4F);
		});
		
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		shader = RenderSystem.getShader();
		SimpleCloudsRenderer.prepareShader(shader, stack.last().pose(), projMat, fogStart, fogEnd);
		shader.apply();
		data = builderSolid.build();
		if (data != null)
			BufferUploader.draw(data);
		shader.clear();
		
		RenderSystem.disableBlend();
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
		stack.translate(-camX, -camY + (double)this.cloudManager.getCloudHeight(), -camZ);
		stack.scale((float)SimpleCloudsConstants.CLOUD_SCALE, (float)SimpleCloudsConstants.CLOUD_SCALE, (float)SimpleCloudsConstants.CLOUD_SCALE);
	}
	
	public void renderWeather(LightTexture texture, float partialTick, double camX, double camY, double camZ)
	{
		if (SimpleCloudsCompatHelper.renderCustomRain())
			this.worldEffectsManager.renderRain(texture, partialTick, camX, camY, camZ);
		if (!SimpleCloudsMod.dhLoaded())
			this.worldEffectsManager.renderLightning(partialTick, camX, camY, camZ);
	}
	
	public void renderBeforeLevel(Matrix4f projMat, Matrix4f camMat, float partialTick, double camX, double camY, double camZ)
	{
		if (!SimpleCloudsCompatHelper.renderThisPass())
			return;
		
		CloudsRenderPipeline pipeline = CompatHelper.areShadersRunning() ? CloudsRenderPipeline.SHADER_SUPPORT : CloudsRenderPipeline.DEFAULT;
		DetermineCloudRenderPipelineEvent pipelineEvent = new DetermineCloudRenderPipelineEvent(pipeline);
		NeoForge.EVENT_BUS.post(pipelineEvent);
		this.renderPipelineThisPass = pipeline;
		if (pipelineEvent.getOverridenPipeline() != null)
			this.renderPipelineThisPass = pipelineEvent.getOverridenPipeline();
		
		float factor = this.worldEffectsManager.getDarkenFactor(partialTick);
		float renderDistance = (float)this.meshGenerator.getCloudAreaMaxRadius() * (float)SimpleCloudsConstants.CLOUD_SCALE * factor;
		if (renderDistance < 2867.0F)
			renderDistance = 2867.0F;
		ModifyCloudRenderDistanceEvent renderDistEvent = new ModifyCloudRenderDistanceEvent(renderDistance);
		NeoForge.EVENT_BUS.post(renderDistEvent);
		renderDistance = renderDistEvent.getRenderDistance();
		this.fogStart = renderDistance / 4.0F;
		this.fogEnd = renderDistance;
		
		Entity cameraEntity = this.mc.gameRenderer.getMainCamera().getEntity();
		if (cameraEntity instanceof LivingEntity living)
		{
			var map = living.getActiveEffectsMap();
			if (map.containsKey(MobEffects.BLINDNESS))
			{
				MobEffectInstance instance = map.get(MobEffects.BLINDNESS);
				float effectFactor = instance.isInfiniteDuration() ? 5.0F : Mth.lerp(Math.min(1.0F, (float)instance.getDuration() / 20.0F), renderDistance, 5.0F);
				this.fogStart = 0.0F;
				this.fogEnd = effectFactor * 0.8F;
			}
			else if (map.containsKey(MobEffects.DARKNESS))
			{
				MobEffectInstance instance = map.get(MobEffects.DARKNESS);
				float f = Mth.lerp(instance.getBlendFactor(living, partialTick), renderDistance, 15.0F);
				this.fogStart = 0.0F;
				this.fogEnd = f;
			}
		}
		
		this.meshGenerator.setCullDistance(this.fogEnd / (float)SimpleCloudsConstants.CLOUD_SCALE);
		
		this.mc.getProfiler().push("simple_clouds_prepare");
		
		this.cullFrustum = new Frustum(camMat, projMat);
		float scale = (float)SimpleCloudsConstants.CLOUD_SCALE;
		double originX = camX / scale;
		double originY = (camY - (double)this.cloudManager.getCloudHeight()) / scale;
		double originZ = camZ / scale;
		this.cullFrustum.prepare(originX, originY, originZ);
		
		ProfilerFiller p = this.mc.getProfiler();
		
		if (SimpleCloudsConfig.CLIENT.generateMesh.get() && SimpleCloudsCompatHelper.isPrimaryPass())
		{
			p.push("mesh_generation");
			this.prepareMeshGenerator(partialTick);
			this.meshGenerator.genTick(originX, originY, originZ, SimpleCloudsConfig.CLIENT.frustumCulling.get() ? this.cullFrustum : null, partialTick);
			p.pop();
		}
		
		if (SimpleCloudsConfig.CLIENT.renderClouds.get() && SimpleCloudsCompatHelper.isPrimaryPass())
		{
			p.push("shadow_map");
			this.renderShadowMaps(camX, camY, camZ, partialTick);
			this.getRenderPipeline().prepare(this.mc, this, camMat, projMat, partialTick, camX, camY, camZ, this.cullFrustum);
			p.pop();
		}
			
		this.mc.getProfiler().pop();
	}
	
	public void renderAfterSky(Matrix4f projMat, Matrix4f camMat, float partialTick, double camX, double camY, double camZ)
	{
		if (!SimpleCloudsCompatHelper.renderThisPass())
			return;
		
		this.mc.getProfiler().push("simple_clouds_after_sky");
		this.getRenderPipeline().afterSky(this.mc, this, camMat, projMat, partialTick, camX, camY, camZ, this.cullFrustum);
		this.mc.getProfiler().pop();
	}
	
	public void renderBeforeWeather(Matrix4f projMat, Matrix4f camMat, float partialTick, double camX, double camY, double camZ)
	{
		if (!SimpleCloudsCompatHelper.renderThisPass())
			return;
		
		this.mc.getProfiler().push("simple_clouds_before_weather");
		this.getRenderPipeline().beforeWeather(this.mc, this, camMat, projMat, partialTick, camX, camY, camZ, this.cullFrustum);
		this.mc.getProfiler().pop();
	}
	
	public void renderAfterLevel(Matrix4f projMat, Matrix4f camMat, float partialTick, double camX, double camY, double camZ)
	{
		if (!SimpleCloudsCompatHelper.renderThisPass())
			return;
		
		this.mc.getProfiler().push("simple_clouds");
		this.getRenderPipeline().afterLevel(this.mc, this, camMat, projMat, partialTick, camX, camY, camZ, this.cullFrustum);
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
	
	public void doFinalCompositePass(Matrix4f camMat, float partialTick, Matrix4f projMat)
	{
		if (this.finalComposite != null)
		{
			RenderSystem.disableDepthTest();
			RenderSystem.resetTextureMatrix();
			RenderSystem.depthMask(false);
			
			this.finalComposite.process(partialTick);
			
			RenderSystem.depthMask(true);
		}
	}
	
	public void doStormPostProcessing(Matrix4f camMat, float partialTick, Matrix4f projMat, double camX, double camY, double camZ, float r, float g, float b)
	{
		if (this.stormPostProcessing == null || this.stormFogShadowMapMatrix == null || this.stormFogShadowMapMatrix == null)
			return;
		
		RenderSystem.disableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.resetTextureMatrix();
		RenderSystem.depthMask(false);
		
		this.stormFogTarget.clear(Minecraft.ON_OSX);
		this.stormFogTarget.bindWrite(true);
		
		MutableInt size = new MutableInt();
		boolean flag = SimpleCloudsConfig.CLIENT.stormFogLightningFlashes.get();
		if (flag)
		{
			List<LightningBolt> lightningBolts = this.worldEffectsManager.getLightningBolts();
			size.setValue(Math.min(lightningBolts.size(), MAX_LIGHTNING_BOLTS));
			if (size.getValue() > 0)
			{
				this.lightningBoltPositions.writeData(buffer -> 
				{
					for (int i = 0; i < size.getValue(); i++)
					{
						LightningBolt bolt = lightningBolts.get(i);
						Vector3f pos = bolt.getPosition();
						buffer.putFloat(pos.x);
						buffer.putFloat(pos.y);
						buffer.putFloat(pos.z);
						buffer.putFloat(bolt.getFade(partialTick));
					}
					buffer.rewind();
				}, size.getValue() * BYTES_PER_LIGHTNING_BOLT, false);
			}
		}
		
		Matrix4f invertedProjMat = new Matrix4f(projMat).invert();
		Matrix4f invertedModelViewMat = new Matrix4f(camMat).invert();
		for (PostPass pass : ((MixinPostChain)this.stormPostProcessing).simpleclouds$getPostPasses())
		{
			EffectInstance effect = pass.getEffect();
			effect.safeGetUniform("InverseWorldProjMat").set(invertedProjMat);
			effect.safeGetUniform("InverseModelViewMat").set(invertedModelViewMat);
			effect.safeGetUniform("ShadowProjMat").set(this.stormFogShadowMap.getProjMatrix());
			effect.safeGetUniform("ShadowModelViewMat").set(this.stormFogShadowMapMatrix);
			effect.safeGetUniform("CameraPos").set((float)camX, (float)camY, (float)camZ);
			effect.safeGetUniform("FogStart").set(this.fogEnd / 2.0F);
			effect.safeGetUniform("FogEnd").set(this.fogEnd);
			effect.safeGetUniform("ColorModulator").set(r, g, b, 1.0F);
			float factor = this.worldEffectsManager.getDarkenFactor(partialTick);
			effect.safeGetUniform("CutoffDistance").set(1000.0F * factor);
			effect.safeGetUniform("TotalLightningBolts").set(size.getValue());
		}
		
		this.stormPostProcessing.process(partialTick);
		
		RenderSystem.depthMask(true);
	}
	
	public void doCloudShadowProcessing(PoseStack stack, float partialTick, Matrix4f projMat, double camX, double camY, double camZ, int depthBufferId)
	{
		if (this.cloudShadows == null || this.shadowMap.isEmpty() || this.shadowMapMatrix == null)
			return;
		
		RenderSystem.disableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.resetTextureMatrix();
		RenderSystem.depthMask(false);
		
		Matrix4f invertedProjMat = new Matrix4f(projMat).invert();
		Matrix4f invertedModelViewMat = new Matrix4f(stack.last().pose()).invert();
		float minimumRadius = this.mc.gameRenderer.getRenderDistance();
		for (PostPass pass : ((MixinPostChain)this.cloudShadows).simpleclouds$getPostPasses())
		{
			EffectInstance effect = pass.getEffect();
			effect.setSampler("DepthSampler", () -> depthBufferId);
			effect.safeGetUniform("InverseWorldProjMat").set(invertedProjMat);
			effect.safeGetUniform("InverseModelViewMat").set(invertedModelViewMat);
			effect.safeGetUniform("ShadowProjMat").set(this.shadowMap.get().getProjMatrix());
			effect.safeGetUniform("ShadowModelViewMat").set(this.shadowMapMatrix);
			effect.safeGetUniform("CameraPos").set((float)camX, (float)camY, (float)camZ);
			effect.safeGetUniform("MinimumRadius").set(minimumRadius);
		}
		
		this.cloudShadows.process(partialTick);
		
		RenderSystem.depthMask(true);
	}
	
	public static void prepareShader(ShaderInstance shader, Matrix4f modelView, Matrix4f projMat, float fogStart, float fogEnd)
	{
		for (int i = 0; i < 12; ++i)
		{
			int j = RenderSystem.getShaderTexture(i);
			shader.setSampler("Sampler" + i, j);
		}

		if (shader.MODEL_VIEW_MATRIX != null)
			shader.MODEL_VIEW_MATRIX.set(modelView);
		
		if (shader.PROJECTION_MATRIX != null)
			shader.PROJECTION_MATRIX.set(projMat);

		if (shader.COLOR_MODULATOR != null)
			shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());

		if (shader.GLINT_ALPHA != null)
			shader.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());

		if (shader.FOG_START != null)
			shader.FOG_START.set(fogStart);

		if (shader.FOG_END != null)
			shader.FOG_END.set(fogEnd);

		if (shader.FOG_COLOR != null)
			shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());

		if (shader.FOG_SHAPE != null)
			shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());

		if (shader.TEXTURE_MATRIX != null)
			shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());

		if (shader.GAME_TIME != null)
			shader.GAME_TIME.set(RenderSystem.getShaderGameTime());

		if (shader.SCREEN_SIZE != null)
		{
			Window window = Minecraft.getInstance().getWindow();
			shader.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
		}
		
		shader.safeGetUniform("UseNormals").set(SimpleCloudsConfig.CLIENT.cubeNormals.get() ? 1 : 0);
		
		RenderSystem.setShaderLights(DIFFUSE_LIGHT_0, DIFFUSE_LIGHT_1);
		RenderSystem.setupShaderLights(shader);
	}
	
	public void copyDepthFromCloudsToMain()
	{
		this._copyDepthSafe(this.mc.getMainRenderTarget(), this.cloudTarget);
	}
	
	public void copyDepthFromMainToClouds()
	{
		this._copyDepthSafe(this.cloudTarget, this.mc.getMainRenderTarget());
	}
	
	public void copyDepthFromCloudsToTransparency()
	{
		this._copyDepthSafe(this.cloudTransparencyTarget, this.cloudTarget);
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
	
	public void fillReport(CrashReport report)
	{
		CrashReportCategory category = report.addCategory("Simple Clouds Renderer");
		category.setDetail("Cloud Mode", this.settings.getCurrentCloudMode());
		category.setDetail("Cloud Target Available", this.cloudTarget != null);
		category.setDetail("Storm Fog Target Active", this.stormFogTarget != null);
		category.setDetail("Blur Target Active", this.blurTarget != null);
		category.setDetail("Transparency Target Active", this.cloudTransparencyTarget != null);
		category.setDetail("Post Chains", this.postChains.toString());
		category.setDetail("Lightning Bolt SSBO", this.lightningBoltPositions);
		category.setDetail("Clouds Shadow Map", this.stormFogShadowMap);
		category.setDetail("Storm Fog Shadow Map", this.stormFogShadowMap);
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
	
	public static void initialize(CloudsRendererSettings settings)
	{
		RenderSystem.assertOnRenderThread();
		if (instance != null)
			throw new IllegalStateException("Simple Clouds renderer is already initialized");
		instance = new SimpleCloudsRenderer(settings, Minecraft.getInstance());
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
	
	private static void renderChunkBox(VertexConsumer consumer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a)
	{
		//-X
		consumer.addVertex(minX, minY, maxZ).setColor(r, g, b, a);
		consumer.addVertex(minX, maxY, maxZ).setColor(r, g, b, a);
		consumer.addVertex(minX, maxY, minZ).setColor(r, g, b, a);
		consumer.addVertex(minX, minY, minZ).setColor(r, g, b, a);
		
		//+X
		consumer.addVertex(maxX, minY, minZ).setColor(r, g, b, a);
		consumer.addVertex(maxX, maxY, minZ).setColor(r, g, b, a);
		consumer.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);
		consumer.addVertex(maxX, minY, maxZ).setColor(r, g, b, a);
		
		//-Y
		consumer.addVertex(maxX, minY, minZ).setColor(r, g, b, a);
		consumer.addVertex(maxX, minY, maxZ).setColor(r, g, b, a);
		consumer.addVertex(minX, minY, maxZ).setColor(r, g, b, a);
		consumer.addVertex(minX, minY, minZ).setColor(r, g, b, a);
		
		//+Y
		consumer.addVertex(minX, maxY, minZ).setColor(r, g, b, a);
		consumer.addVertex(minX, maxY, maxZ).setColor(r, g, b, a);
		consumer.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);
		consumer.addVertex(maxX, maxY, minZ).setColor(r, g, b, a);
		
		//-Z
		consumer.addVertex(minX, minY, minZ).setColor(r, g, b, a);
		consumer.addVertex(minX, maxY, minZ).setColor(r, g, b, a);
		consumer.addVertex(maxX, maxY, minZ).setColor(r, g, b, a);
		consumer.addVertex(maxX, minY, minZ).setColor(r, g, b, a);
		
		//+Z
		consumer.addVertex(maxX, minY, maxZ).setColor(r, g, b, a);
		consumer.addVertex(maxX, maxY, maxZ).setColor(r, g, b, a);
		consumer.addVertex(minX, maxY, maxZ).setColor(r, g, b, a);
		consumer.addVertex(minX, minY, maxZ).setColor(r, g, b, a);
	}
	
	private static int calculateMeshGenInterval()
	{
		int fps = Minecraft.getInstance().getFps();
		switch (SimpleCloudsConfig.CLIENT.generationInterval.get())
		{
		case STATIC:
		{
			return SimpleCloudsConfig.CLIENT.framesToGenerateMesh.get();
		}
		case DYNAMIC:
		{
			return Math.max(Mth.ceil((130.0F - (float)fps) / 30.0F) + 5, 1);
		}
		case TARGET_FPS:
		{
			return Math.max(Mth.ceil((float)fps / SimpleCloudsConfig.CLIENT.targetMeshGenFps.get()), 1);
		}
		default:
			return 5;
		}
	}
}
