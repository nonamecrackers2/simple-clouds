package dev.nonamecrackers2.simpleclouds.client.renderer;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.shader.SimpleCloudsShaders;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.AtomicCounter;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.noise.ModifiableLayeredNoise;
import dev.nonamecrackers2.simpleclouds.common.noise.ModifiableNoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.NoiseSettings;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nonamecrackers2.crackerslib.common.compat.CompatHelper;

//TODO: Better far plane extension, use lowest GL class, etc
public class SimpleCloudsRenderer implements ResourceManagerReloadListener
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/SimpleCloudsRenderer");
	private static final Vector3f DIFFUSE_LIGHT_0 = (new Vector3f(0.2F, 1.0F, -0.7F)).normalize();
	private static final Vector3f DIFFUSE_LIGHT_1 = (new Vector3f(-0.2F, 1.0F, 0.7F)).normalize();
	private static final ResourceLocation POST_PROCESSING_LOC = SimpleCloudsMod.id("shaders/post/cloud_post.json");
	public static final int CLOUD_SCALE = 8;
//	private static boolean extendFarPlane;
//	private static float extendedFarPlaneAmount = -1.0F;
//	private static Matrix4f prevProjMat;
	private static @Nullable SimpleCloudsRenderer instance;
	private final Minecraft mc;
	private final CloudMeshGenerator meshGenerator;
	private final RandomSource random;
	private final ModifiableLayeredNoise previewNoiseSettings = new ModifiableLayeredNoise().addNoiseLayer(new ModifiableNoiseSettings());
	private @Nullable RenderTarget cloudTarget;
	private @Nullable PostChain cloudsPostProcessing;
	private int arrayObjectId = -1;
	private int totalIndices;
	private int totalSides;
	private float scrollX;
	private float scrollY;
	private float scrollZ;
	private Vector3f scrollDirection = new Vector3f(1.0F, 0.0F, 0.0F);
	private boolean previewToggled;
	private Frustum cullFrustum;
	
	private SimpleCloudsRenderer(Minecraft mc)
	{
		this.mc = mc;
		this.meshGenerator = new CloudMeshGenerator();
		this.setupMeshGenerator();
		this.random = RandomSource.create();
	}
	
	private void setupMeshGenerator()
	{
		//this.meshGenerator.setNoiseScale(30.0F, 10.0F, 30.0F);
		this.meshGenerator.setScroll(this.scrollX, this.scrollY, this.scrollZ);
	}
	
	@Override
	public void onResourceManagerReload(ResourceManager manager)
	{
		this.scrollDirection = new Vector3f(this.random.nextFloat() * 2.0F - 1.0F, this.random.nextFloat() * 2.0F - 1.0F, this.random.nextFloat() * 2.0F - 1.0F).normalize();
		
		if (this.cloudTarget != null)
			this.cloudTarget.destroyBuffers();
		this.cloudTarget = new TextureTarget(this.mc.getWindow().getWidth(), this.mc.getWindow().getHeight(), true, Minecraft.ON_OSX);
		this.cloudTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
		
		if (this.arrayObjectId >= 0)
		{
			RenderSystem.glDeleteVertexArrays(this.arrayObjectId);
			this.arrayObjectId = -1;
		}
		
		this.meshGenerator.init(manager);
		
		if (this.meshGenerator.getShader() != null)
		{
			this.arrayObjectId = GL30.glGenVertexArrays();
			this.rebindBuffers();
		}
		
		if (this.cloudsPostProcessing != null)
			this.cloudsPostProcessing.close();
		
		try
		{
			this.cloudsPostProcessing = new PostChain(this.mc.getTextureManager(), manager, this.cloudTarget, POST_PROCESSING_LOC);
			this.cloudsPostProcessing.resize(this.mc.getWindow().getWidth(), this.mc.getWindow().getHeight());
		}
		catch (JsonSyntaxException e)
		{
			LOGGER.warn("Failed to parse shader: {}", POST_PROCESSING_LOC, e);
		}
		catch (IOException e)
		{
			LOGGER.warn("Failed to load shader: {}", POST_PROCESSING_LOC, e);
		}
	}
	
	private void rebindBuffers()
	{
		if (this.arrayObjectId != -1)
		{
			this.totalSides = this.meshGenerator.getShader().<AtomicCounter>getBufferObject(0).get();
			this.totalIndices = this.totalSides * 6;
			int vertexBufferId = this.meshGenerator.getShader().getBufferObject(1).getId();
			int indexBufferId = this.meshGenerator.getShader().getBufferObject(2).getId();
			
			GL30.glBindVertexArray(this.arrayObjectId);
			
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBufferId);
			//Vertex position
			GL20.glEnableVertexAttribArray(0);
			GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 24, 0);
			//Vertex color
//			GL20.glEnableVertexAttribArray(1);
//			GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, true, 40, 12);
			//Vertex normal
			GL20.glEnableVertexAttribArray(2);
			GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, true, 24, 12);
			
			GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBufferId);
			
			GL30.glBindVertexArray(0);
		}
	}
	
	public CloudMeshGenerator getMeshGenerator()
	{
		return this.meshGenerator;
	}
	
	public void onResize(int width, int height)
	{
		if (this.cloudTarget != null)
			this.cloudTarget.resize(width, height, Minecraft.ON_OSX);
		if (this.cloudsPostProcessing != null)
			this.cloudsPostProcessing.resize(width, height);
	}
		
	public void shutdown()
	{
		if (this.cloudTarget != null)
			this.cloudTarget.destroyBuffers();
		this.cloudTarget = null;
		if (this.cloudsPostProcessing != null)
			this.cloudsPostProcessing.close();
		this.cloudsPostProcessing = null;
		this.meshGenerator.close();
	}
	
	public void tick()
	{
		float speed = 0.001F * SimpleCloudsConfig.CLIENT.speedModifier.get().floatValue();
		this.scrollX += this.scrollDirection.x() * speed;
		this.scrollY += this.scrollDirection.y() * speed;
		this.scrollZ += this.scrollDirection.z() * speed;
	}
	
	public void generateMesh(NoiseSettings settings, double camX, double camY, double camZ, @Nullable Frustum frustum)
	{
		this.setupMeshGenerator();
		this.meshGenerator.generateMesh(settings, SimpleCloudsConfig.CLIENT.movementSmoothing.get(), camX, camY, camZ, SimpleCloudsConfig.CLIENT.noiseThreshold.get().floatValue(), (float)CLOUD_SCALE, frustum);
		this.totalSides = this.meshGenerator.getShader().<AtomicCounter>getBufferObject(0).get();
		this.totalIndices = this.totalSides * 6;
	}
	
	public void render(PoseStack stack, Matrix4f projMat, float partialTick, float r, float g, float b)
	{
		if (this.arrayObjectId != -1)
		{
			BufferUploader.reset();
			
			RenderSystem.setShader(SimpleCloudsShaders::getCloudsShader);
			RenderSystem.disableBlend();
			RenderSystem.enableDepthTest();
			RenderSystem.setShaderColor(r, g, b, 1.0F);
			
			GL30.glBindVertexArray(this.arrayObjectId);
			
			ShaderInstance shader = RenderSystem.getShader();
			prepareShader(shader, stack.last().pose(), projMat);
			shader.apply();
			RenderSystem.drawElements(GL11.GL_TRIANGLES, this.totalIndices, GL11.GL_UNSIGNED_INT);
			shader.clear();
			
			GL30.glBindVertexArray(0);
			
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		}
	}
	
	public void renderInWorld(PoseStack stack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ)
	{
		if (this.arrayObjectId != -1)
		{
			this.cullFrustum = new Frustum(stack.last().pose(), projMat);
			this.cullFrustum.prepare(camX, camY, camZ);
			
			if (!this.mc.isPaused())
				this.generateMesh(this.previewToggled ? StaticNoiseSettings.DEFAULT : this.previewNoiseSettings, camX, camY, camZ, this.cullFrustum);
			
			this.cloudTarget.clear(Minecraft.ON_OSX);
			this.cloudTarget.copyDepthFrom(this.mc.getMainRenderTarget());
			this.cloudTarget.bindWrite(false);
			
			stack.pushPose();
			stack.translate(-camX, -camY, -camZ);
			stack.scale((float)CLOUD_SCALE, (float)CLOUD_SCALE, (float)CLOUD_SCALE);
			Vec3 cloudCol = this.mc.level.getCloudColor(partialTick);
			this.render(stack, projMat, partialTick, (float)cloudCol.x, (float)cloudCol.y, (float)cloudCol.z);
			stack.popPose();
			
			this.doPostProcessing(stack, partialTick, projMat);
			
			this.mc.getMainRenderTarget().bindWrite(false);
			
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
			this.cloudTarget.blitToScreen(this.mc.getWindow().getWidth(), this.mc.getWindow().getHeight(), false);
			RenderSystem.disableBlend();
	        RenderSystem.defaultBlendFunc();
		}
	}
	
	private void doPostProcessing(PoseStack stack, float partialTick, Matrix4f projMat)
	{
		if (this.cloudsPostProcessing != null)
		{
			RenderSystem.disableDepthTest();
			RenderSystem.resetTextureMatrix();
			
			for (PostPass pass : ((MixinPostChain)this.cloudsPostProcessing).simpleclouds$getPostPasses())
			{
				EffectInstance effect = pass.getEffect();
				effect.safeGetUniform("WorldProjMat").set(projMat);
				effect.safeGetUniform("ModelViewMat").set(stack.last().pose());
				float renderDistance = (float)CloudMeshGenerator.getCloudRenderDistance() * (float)CLOUD_SCALE;
				effect.safeGetUniform("FogStart").set(renderDistance - renderDistance / 4.0F);
				effect.safeGetUniform("FogEnd").set(renderDistance);
			}
			
			this.cloudsPostProcessing.process(partialTick);
		}
	}
	
	private static void prepareShader(ShaderInstance shader, Matrix4f modelView, Matrix4f projMat)
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
	
	public ModifiableLayeredNoise getPreviewNoiseSettings()
	{
		return this.previewNoiseSettings;
	}
	
	public boolean previewToggled()
	{
		return this.previewToggled;
	}
	
	public void togglePreview(boolean flag)
	{
		this.previewToggled = flag;
	}
	
	public int getTotalSides()
	{
		return this.totalSides;
	}
	
	public static boolean isEnabled()
	{
		return !CompatHelper.isShadersRunning();
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
}
