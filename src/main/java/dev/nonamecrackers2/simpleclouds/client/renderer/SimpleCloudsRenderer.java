package dev.nonamecrackers2.simpleclouds.client.renderer;

import java.io.IOException;
import java.nio.IntBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
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
import org.lwjgl.opengl.GL30;

import com.google.common.collect.Maps;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.mesh.CloudStyle;
import dev.nonamecrackers2.simpleclouds.client.mesh.MultiRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.mesh.SingleRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.renderer.pipeline.CloudsRenderPipeline;
import dev.nonamecrackers2.simpleclouds.client.shader.SimpleCloudsShaders;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeDataManager;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.noise.StaticNoiseSettings;
import dev.nonamecrackers2.simpleclouds.mixin.MixinPostChain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import nonamecrackers2.crackerslib.common.compat.CompatHelper;

public class SimpleCloudsRenderer implements ResourceManagerReloadListener
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/SimpleCloudsRenderer");
	private static final Vector3f DIFFUSE_LIGHT_0 = (new Vector3f(0.2F, 1.0F, -0.7F)).normalize();
	private static final Vector3f DIFFUSE_LIGHT_1 = (new Vector3f(-0.2F, 1.0F, 0.7F)).normalize();
	private static final ResourceLocation CLOUD_POST_PROCESSING_LOC = SimpleCloudsMod.id("shaders/post/cloud_post.json");
//	private static final ResourceLocation WORLD_POST_PROCESSING_LOC = SimpleCloudsMod.id("shaders/post/world_post.json");
	private static final ResourceLocation STORM_POST_PROCESSING_LOC = SimpleCloudsMod.id("shaders/post/storm_post.json");
	private static final ResourceLocation BLUR_POST_PROCESSING_LOC = SimpleCloudsMod.id("shaders/post/blur_post.json");
	public static final CloudType FALLBACK = new CloudType(0.0F, 16.0F, 32.0F, StaticNoiseSettings.DEFAULT);
	public static final int CLOUD_SCALE = 8;
	private static final int SHADOW_MAP_SIZE = 1024;
	private static @Nullable SimpleCloudsRenderer instance;
	private final Minecraft mc;
	private CloudMeshGenerator meshGenerator;
	private final RandomSource random;
	private Matrix4f shadowMapProjMat;
	private @Nullable RenderTarget cloudTarget;
	private @Nullable RenderTarget stormFogTarget;
	private @Nullable RenderTarget blurTarget;
	private final Map<PostChain, Pair<Float, Float>> postChains = Maps.newHashMap();
	private @Nullable PostChain cloudsPostProcessing;
//	private @Nullable PostChain worldPostProcessing;
	private @Nullable PostChain stormPostProcessing;
	private @Nullable PostChain blurPostProcessing;
	private float scrollX;
	private float scrollY;
	private float scrollZ;
	private Vector3f scrollDirection = new Vector3f(1.0F, 0.0F, 0.0F);
	private boolean previewToggled;
	private Frustum cullFrustum;
	private int shadowMapBufferId = -1;
	private int shadowMapDepthTextureId = -1;
	private int shadowMapColorTextureId = -1;
	private float fogStart;
	private float fogEnd;
	private @Nullable PoseStack shadowMapStack;
	private boolean failedToCopyDepthBuffer;
	private @Nullable CloudMode cloudMode;
	private @Nullable CloudStyle cloudStyle;
	
	private SimpleCloudsRenderer(Minecraft mc)
	{
		this.mc = mc;
		this.random = RandomSource.create();
	}
	
	private void setupMeshGenerator()
	{
		this.meshGenerator.setMeshGenInterval(SimpleCloudsConfig.CLIENT.framesToGenerateMesh.get());
		this.meshGenerator.setTestFacesFacingAway(SimpleCloudsConfig.CLIENT.testSidesThatAreOccluded.get());
		this.meshGenerator.setLodConfig(SimpleCloudsConfig.CLIENT.levelOfDetail.get().getConfig());
		this.meshGenerator.setScroll(this.scrollX, this.scrollY, this.scrollZ);
	}
	
	@Override
	public void onResourceManagerReload(ResourceManager manager)
	{
		RenderSystem.assertOnRenderThreadOrInit();
		
		this.failedToCopyDepthBuffer = false;
		
		this.scrollDirection = new Vector3f(this.random.nextFloat() * 2.0F - 1.0F, this.random.nextFloat() * 2.0F - 1.0F, this.random.nextFloat() * 2.0F - 1.0F).normalize();
		
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
		CloudMode mode = SimpleCloudsConfig.CLIENT.cloudMode.get();
		CloudStyle style = SimpleCloudsConfig.CLIENT.cloudStyle.get();
		LOGGER.info("Beginning mesh generator initialization for cloud mode {} and cloud style {}", mode, style);
		if (this.cloudMode != mode || this.cloudStyle != style)
		{
			if (this.meshGenerator != null)
			{
				this.meshGenerator.close();
				this.meshGenerator = null;
			}
			if (mode == CloudMode.DEFAULT || mode == CloudMode.AMBIENT)
			{
				MultiRegionCloudMeshGenerator generator = new MultiRegionCloudMeshGenerator(new CloudType[] { FALLBACK }, SimpleCloudsConfig.CLIENT.levelOfDetail.get().getConfig(), SimpleCloudsConfig.CLIENT.framesToGenerateMesh.get(), style);
				var cloudTypes = CloudTypeDataManager.INSTANCE.getCloudTypes();
				if (cloudTypes.size() > MultiRegionCloudMeshGenerator.MAX_CLOUD_TYPES)
					LOGGER.warn("The amount of loaded cloud types exceeds the maximum of {}. Please be aware that not all cloud types loaded will be used.", MultiRegionCloudMeshGenerator.MAX_CLOUD_TYPES);
				else
					generator.setCloudTypes(cloudTypes.values().toArray(i -> new CloudType[i]));
				if (mode == CloudMode.AMBIENT)
					generator.setFadeNearOrigin(0.2F, 0.4F);
				this.meshGenerator = generator;
			}
			else if (mode == CloudMode.SINGLE)
			{
				float fadeStart = (float)SimpleCloudsConfig.CLIENT.singleModeFadeStartPercentage.get() / 100.0F;
				float fadeEnd = (float)SimpleCloudsConfig.CLIENT.singleModeFadeEndPercentage.get() / 100.0F;
				SingleRegionCloudMeshGenerator generator = new SingleRegionCloudMeshGenerator(FALLBACK, SimpleCloudsConfig.CLIENT.levelOfDetail.get().getConfig(), SimpleCloudsConfig.CLIENT.framesToGenerateMesh.get(), fadeStart, fadeEnd, style);
				String rawId = SimpleCloudsConfig.CLIENT.singleModeCloudType.get();
				ResourceLocation loc = ResourceLocation.tryParse(rawId);
				if (loc != null)
				{
					CloudType type = CloudTypeDataManager.INSTANCE.getCloudTypes().get(loc);
					if (type != null)
						generator.setCloudType(type);
				}
				this.meshGenerator = generator;
			}
			else
			{
				throw new IllegalArgumentException("Not sure how to handle cloud mode " + mode);
			}
			this.cloudMode = mode;
			this.cloudStyle = style;
		}
		this.setupMeshGenerator();
		this.meshGenerator.init(manager);
		long duration = Duration.between(started, Instant.now()).toMillis();
		LOGGER.info("Finished, took {} ms", duration);
		
		int span = this.meshGenerator.getLodConfig().getEffectiveChunkSpan() * 32 * CLOUD_SCALE;
		this.shadowMapProjMat = new Matrix4f().setOrtho(0.0F, span, span, 0.0F, 0.0F, 10000.0F);
		
		this.destroyPostChains();
		
		this.cloudsPostProcessing = this.createPostChain(manager, CLOUD_POST_PROCESSING_LOC, this.cloudTarget, 1.0F, 1.0F);
//		this.worldPostProcessing = this.createPostChain(manager, WORLD_POST_PROCESSING_LOC, this.mc.getMainRenderTarget(), 1.0F, 1.0F, effect -> {
//			effect.setSampler("ShadowMap", () -> this.shadowMapDepthTextureId);
//		});
		this.stormPostProcessing = this.createPostChain(manager, STORM_POST_PROCESSING_LOC, this.stormFogTarget, 0.25F, 0.25F, effect -> {
			effect.setSampler("ShadowMap", () -> this.shadowMapDepthTextureId);
			effect.setSampler("ShadowMapColor", () -> this.shadowMapColorTextureId);
		});
		this.blurPostProcessing = this.createPostChain(manager, BLUR_POST_PROCESSING_LOC, this.blurTarget, 1.0F, 1.0F);
		this.blurPostProcessing.getTempTarget("swap").setFilterMode(GL11.GL_LINEAR);
		
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
		if (this.blurPostProcessing != null)
			this.blurPostProcessing.getTempTarget("swap").setFilterMode(GL11.GL_LINEAR);
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
	}
	
	public void tick()
	{
		float speed = 0.01F * SimpleCloudsConfig.CLIENT.speedModifier.get().floatValue();
		this.scrollX += this.scrollDirection.x() * speed;
		this.scrollY += this.scrollDirection.y() * speed;
		this.scrollZ += this.scrollDirection.z() * speed;
		float renderDistance = (float)this.meshGenerator.getCloudAreaMaxRadius() * (float)CLOUD_SCALE;
		this.fogStart = renderDistance / 4.0F;
		this.fogEnd = renderDistance;
	}
	
	public void renderShadowMap(PoseStack stack, double camX, double camY, double camZ)
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
			
			int span = this.meshGenerator.getLodConfig().getEffectiveChunkSpan() * 32 * CLOUD_SCALE;
			stack.translate(span / 2.0D, span / 2.0D, -5000.0D);
			float yaw = (float)Mth.atan2((double)this.scrollDirection.x, (double)this.scrollDirection.z);
			stack.mulPose(Axis.XP.rotationDegrees(SimpleCloudsConfig.CLIENT.stormFogAngle.get().floatValue()));
			stack.mulPose(Axis.YP.rotation(yaw));
			float chunkSizeUpscaled = 32.0F * (float)CLOUD_SCALE;
			float camOffsetX = ((float)Mth.floor(camX / chunkSizeUpscaled) * chunkSizeUpscaled);
			float camOffsetZ = ((float)Mth.floor(camZ / chunkSizeUpscaled) * chunkSizeUpscaled);
			stack.translate(-camOffsetX, -(double)SimpleCloudsConfig.CLIENT.cloudHeight.get(), -camOffsetZ);
			stack.pushPose();
			translateClouds(stack, 0.0D, 0.0D, 0.0D);
			RenderSystem.setShader(SimpleCloudsShaders::getCloudsShadowMapShader);
			prepareShader(RenderSystem.getShader(), stack.last().pose(), this.shadowMapProjMat);
			RenderSystem.getShader().apply();
			RenderSystem.drawElements(GL11.GL_TRIANGLES, this.meshGenerator.getTotalIndices(), GL11.GL_UNSIGNED_INT);
			RenderSystem.getShader().clear();
			stack.popPose();
			
			GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
			
			this.mc.getMainRenderTarget().bindWrite(true);
			
			GL30.glBindVertexArray(0);
			RenderSystem.enableCull();
			
		}
		
		this.shadowMapStack = stack;
	}
	
	public static void translateClouds(PoseStack stack, double camX, double camY, double camZ)
	{
		stack.translate(-camX, -camY + (double)SimpleCloudsConfig.CLIENT.cloudHeight.get(), -camZ);
		stack.scale((float)CLOUD_SCALE, (float)CLOUD_SCALE, (float)CLOUD_SCALE);
	}
	
	public void renderBeforeLevel(PoseStack stack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ)
	{
		this.mc.getProfiler().push("simple_clouds_prepare");
		if (this.meshGenerator.getArrayObjectId() != -1)
		{
			this.cullFrustum = new Frustum(stack.last().pose(), projMat);
			
			if (SimpleCloudsConfig.CLIENT.generateMesh.get())
			{
				this.mc.getProfiler().push("mesh_generation");
				if (this.meshGenerator instanceof SingleRegionCloudMeshGenerator generator)
					generator.setFadeDistance((float)SimpleCloudsConfig.CLIENT.singleModeFadeStartPercentage.get() / 100.0F, (float)SimpleCloudsConfig.CLIENT.singleModeFadeEndPercentage.get() / 100.0F);
				this.setupMeshGenerator();
				this.meshGenerator.tick(camX, camY - (double)SimpleCloudsConfig.CLIENT.cloudHeight.get(), camZ, (float)CLOUD_SCALE, SimpleCloudsConfig.CLIENT.frustumCulling.get() ? this.cullFrustum : null);
				this.mc.getProfiler().pop();
			}
			
			if (SimpleCloudsConfig.CLIENT.renderClouds.get())
				getRenderPipeline().prepare(this.mc, this, stack, projMat, partialTick, camX, camY, camZ);
		}
		this.mc.getProfiler().pop();
	}
	
	public void renderAfterSky(PoseStack stack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ)
	{
		this.mc.getProfiler().push("simple_clouds_after_sky");
		if (this.meshGenerator.getArrayObjectId() != -1 && SimpleCloudsConfig.CLIENT.renderClouds.get())
			getRenderPipeline().afterSky(this.mc, this, stack, this.shadowMapStack, projMat, partialTick, camX, camY, camZ);
		this.mc.getProfiler().pop();
	}
	
	public void renderAfterLevel(PoseStack stack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ)
	{
		this.mc.getProfiler().push("simple_clouds");
		if (this.meshGenerator.getArrayObjectId() != -1 && SimpleCloudsConfig.CLIENT.renderClouds.get())
			getRenderPipeline().afterLevel(this.mc, this, stack, this.shadowMapStack, projMat, partialTick, camX, camY, camZ);
		this.mc.getProfiler().pop();
	}
	
//	public void renderInWorldPost(PoseStack stack, float partialTick, Matrix4f projMat, double camX, double camY, double camZ)
//	{
//		this.mc.getProfiler().push("simple_clouds_post");
//		if (this.meshGenerator.getArrayObjectId() != -1)
//		{
//		}
//		this.mc.getProfiler().pop();
//	}
//	
	public void doBlurPostProcessing(float partialTick)
	{
		if (this.blurPostProcessing != null)
		{
			RenderSystem.disableDepthTest();
			RenderSystem.resetTextureMatrix();
			RenderSystem.disableBlend();
			this.blurPostProcessing.process(partialTick);
		}
	}
//	
//	private void doWorldPostProcessing(PoseStack stack, PoseStack shadowMapStack, float partialTick, Matrix4f projMat, double camX, double camY, double camZ)
//	{
//		if (this.worldPostProcessing != null)
//		{
//			RenderSystem.disableDepthTest();
//			RenderSystem.resetTextureMatrix();
//			RenderSystem.disableBlend();
//			
//			Matrix4f invertedProjMat = new Matrix4f(projMat).invert();
//			Matrix4f invertedModelViewMat = new Matrix4f(stack.last().pose()).invert();
//			for (PostPass pass : ((MixinPostChain)this.worldPostProcessing).simpleclouds$getPostPasses())
//			{
//				EffectInstance effect = pass.getEffect();
//				effect.safeGetUniform("InverseWorldProjMat").set(invertedProjMat);
//				effect.safeGetUniform("InverseModelViewMat").set(invertedModelViewMat);
//				effect.safeGetUniform("ShadowProjMat").set(this.shadowMapProjMat);
//				effect.safeGetUniform("ShadowModelViewMat").set(shadowMapStack.last().pose());
//				effect.safeGetUniform("CameraPos").set((float)camX, (float)camY, (float)camZ);
//				effect.setSampler("ShadowMap", () -> this.shadowMapDepthTextureId);
//			}
//			
//			this.worldPostProcessing.process(partialTick);
//		}
//	}
	
	public void doCloudPostProcessing(PoseStack stack, float partialTick, Matrix4f projMat)
	{
		if (this.cloudsPostProcessing != null)
		{
			RenderSystem.disableBlend();
			RenderSystem.disableDepthTest();
			RenderSystem.resetTextureMatrix();
			
			Matrix4f invertedProjMat = new Matrix4f(projMat).invert();
			Matrix4f invertedModelViewMat = new Matrix4f(stack.last().pose()).invert();
			for (PostPass pass : ((MixinPostChain)this.cloudsPostProcessing).simpleclouds$getPostPasses())
			{
				EffectInstance effect = pass.getEffect();
				effect.safeGetUniform("InverseWorldProjMat").set(invertedProjMat);
				effect.safeGetUniform("InverseModelViewMat").set(invertedModelViewMat);
				effect.safeGetUniform("FogStart").set(this.fogStart);
				effect.safeGetUniform("FogEnd").set(this.fogEnd);
				float[] fogCol = RenderSystem.getShaderFogColor();
				effect.safeGetUniform("DefaultFogColor").set(fogCol[0], fogCol[1], fogCol[2]);
			}
			
			this.cloudsPostProcessing.process(partialTick);
		}
	}
	
	public void doStormPostProcessing(PoseStack stack, PoseStack shadowMapStack, float partialTick, Matrix4f projMat, double camX, double camY, double camZ, float r, float g, float b)
	{
		if (this.stormPostProcessing != null)
		{
			RenderSystem.disableBlend();
			RenderSystem.disableDepthTest();
			RenderSystem.resetTextureMatrix();
			
			this.stormFogTarget.clear(Minecraft.ON_OSX);
			this.stormFogTarget.bindWrite(true);
			
			Matrix4f invertedProjMat = new Matrix4f(projMat).invert();
			Matrix4f invertedModelViewMat = new Matrix4f(stack.last().pose()).invert();
			for (PostPass pass : ((MixinPostChain)this.stormPostProcessing).simpleclouds$getPostPasses())
			{
				EffectInstance effect = pass.getEffect();
				effect.safeGetUniform("InverseWorldProjMat").set(invertedProjMat);
				effect.safeGetUniform("InverseModelViewMat").set(invertedModelViewMat);
				effect.safeGetUniform("ShadowProjMat").set(this.shadowMapProjMat);
				effect.safeGetUniform("ShadowModelViewMat").set(shadowMapStack.last().pose());
				effect.safeGetUniform("CameraPos").set((float)camX, (float)camY, (float)camZ);
				effect.safeGetUniform("FogStart").set(this.fogEnd / 2.0F);
				effect.safeGetUniform("FogEnd").set(this.fogEnd);
				effect.safeGetUniform("ColorModulator").set(r, g, b, 1.0F);
			}
			
			this.stormPostProcessing.process(partialTick);
		}
	}
//	
//	private static void renderDebugRegionBoundingBoxes(MultiBufferSource.BufferSource buffers, double camX, double camY, double camZ, Frustum frustum)
//	{
//		float chunkSize = 32.0F * CLOUD_SCALE;
//		float camOffsetX = ((float)Mth.floor(camX / chunkSize) * chunkSize);
//		float camOffsetZ = ((float)Mth.floor(camZ / chunkSize) * chunkSize);
//		int radius = CloudMeshGenerator.PRIMARY_CHUNK_SPAN / 2;
//		VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
//		for (int x = -radius; x < radius; x++)
//		{
//			for (int y = 0; y < CloudMeshGenerator.VERTICAL_CHUNK_SPAN; y++)
//			{
//				for (int z = -radius; z < radius; z++)
//				{
//					float offsetX = (float)x * chunkSize;
//					float offsetY = (float)y * chunkSize;
//					float offsetZ = (float)z * chunkSize;
//					AABB box = new AABB(offsetX, offsetY, offsetZ, offsetX + chunkSize, offsetY + chunkSize, offsetZ + chunkSize).move(camOffsetX, 0.0F, camOffsetZ).move(-camX, -camY, -camZ);
//					LevelRenderer.renderLineBox(new PoseStack(), consumer, box, frustum.isVisible(box) ? 0.0F : 1.0F, 1.0F, 0.0F, 1.0F);
//				}
//			}
//		}
//		FogRenderer.setupNoFog();
//		buffers.endBatch();
//	}
	
	public static void prepareShader(ShaderInstance shader, Matrix4f modelView, Matrix4f projMat)
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

		if (shader.INVERSE_VIEW_ROTATION_MATRIX != null)
			shader.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());

		if (shader.COLOR_MODULATOR != null)
			shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());

		if (shader.GLINT_ALPHA != null)
			shader.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());

		if (shader.FOG_START != null)
			shader.FOG_START.set(RenderSystem.getShaderFogStart());

		if (shader.FOG_END != null)
			shader.FOG_END.set(RenderSystem.getShaderFogEnd());

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
		
		RenderSystem.setShaderLights(DIFFUSE_LIGHT_0, DIFFUSE_LIGHT_1);
		RenderSystem.setupShaderLights(shader);
	}
	
	public boolean previewToggled()
	{
		return this.previewToggled;
	}
	
	public void togglePreview(boolean flag)
	{
		this.previewToggled = flag;
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
	
	private void _copyDepthSafe(RenderTarget to, RenderTarget from)
	{
		RenderSystem.assertOnRenderThread();
		if (!this.failedToCopyDepthBuffer)
		{
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
	
	//TODO: Make it so you can't call this multiple times
	public static void initialize()
	{
		RenderSystem.assertOnRenderThread();
		instance = new SimpleCloudsRenderer(Minecraft.getInstance());
		LOGGER.debug("Clouds render initialized");
	}
	
	public static SimpleCloudsRenderer getInstance()
	{
		return Objects.requireNonNull(instance, "Renderer not initialized!");
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
}
