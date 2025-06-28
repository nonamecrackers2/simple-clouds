package dev.nonamecrackers2.simpleclouds.client.renderer;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix2f;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.compat.SimpleCloudsCompatHelper;
import dev.nonamecrackers2.simpleclouds.mixin.MixinPostChain;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.Tags;

public class AtmosphericCloudsRenderHandler
{
	private static final ResourceLocation SHADER_LOC = SimpleCloudsMod.id("shaders/post/atmospheric_clouds.json");
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/AtmosphericCloudsRenderHandler");
	private static final float SHIFT_MOVEMENT_SPEED = 0.005F;
	private static final float TRANSITION_SPEED = 0.001F;
	private static final int BIOME_CHECK_INTERVAL = 400;
	private static final AtmosphericCloudsRenderHandler.Formation DEFAULT = new AtmosphericCloudsRenderHandler.Formation(b -> b.value().getBaseTemperature() < 2.0F, 0.6F, 1.0F, 1.0F);
			// In order of precedence
	private static final ImmutableList<AtmosphericCloudsRenderHandler.Formation> FORMATIONS = Util.make(() -> 
	{
		ImmutableList.Builder<AtmosphericCloudsRenderHandler.Formation> builder = ImmutableList.builder();
		builder.add(new AtmosphericCloudsRenderHandler.Formation(b -> b.is(Tags.Biomes.IS_COLD_OVERWORLD) || b.is(Tags.Biomes.IS_DRY_OVERWORLD) || b.is(BiomeTags.IS_SAVANNA), 0.3F, 1.0F, 30.0F)); // Cirrostratus-like clouds
		builder.add(new AtmosphericCloudsRenderHandler.Formation(b -> b.is(Tags.Biomes.IS_HOT_OVERWORLD), 0.8F, 10.0F, 10.0F)); // Cirrocumulus-like clouds
		builder.add(new AtmosphericCloudsRenderHandler.Formation(b -> b.is(Tags.Biomes.IS_PLAINS) || b.is(BiomeTags.IS_FOREST), 1.0F, 2.0F, 10.0F)); // Cirrus-like clouds
		builder.add(DEFAULT); // Very sparse clouds, appears everywhere else
		return builder.build();
	});

	private final Minecraft mc;
	private @Nullable PostChain postProcessingShader;
	private Vector2f windDirection = new Vector2f(1.0F, 0.0F);
	private float shiftMovement;
	private float shiftMovementO;
	private float transition;
	private float transitionO;
	private int tickCount;
	private AtmosphericCloudsRenderHandler.Formation formation = DEFAULT;
	private AtmosphericCloudsRenderHandler.Formation nextFormation;
	
	public AtmosphericCloudsRenderHandler(Minecraft mc)
	{
		this.mc = mc;
	}
	
	public void setWindDirection(Vector2f direction)
	{
		this.windDirection = new Vector2f(direction);
	}
	
	public void tick()
	{
		this.tickCount++;
		
		this.shiftMovementO = this.shiftMovement;
		this.shiftMovement += SHIFT_MOVEMENT_SPEED;
		
		this.transitionO = this.transition;
		if (this.nextFormation != null)
		{
			this.transition += TRANSITION_SPEED;
			if (this.transition > 1.0F)
			{
				this.formation = this.nextFormation;
				this.nextFormation = null;
				this.transition = 0.0F;
				this.transitionO = 0.0F;
			}
		}
		
		if (this.mc.level != null && this.tickCount % BIOME_CHECK_INTERVAL == 0)
		{
			for (AtmosphericCloudsRenderHandler.Formation formation : FORMATIONS)
			{
				if (formation.rendersIn().test(this.mc.level.getBiome(this.mc.gameRenderer.getMainCamera().getBlockPosition())))
				{
					if (this.formation != formation && this.nextFormation != formation && this.transition == 0.0F)
						this.nextFormation = formation;
					break;
				}
			}
		}
	}
	
	public void render(PoseStack stack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ, float r, float g, float b)
	{
		if (this.postProcessingShader != null)
		{
			RenderSystem.disableDepthTest();
			RenderSystem.resetTextureMatrix();
			RenderSystem.disableBlend();
			RenderSystem.depthMask(false);
			
			Matrix4f invertedProjMat = new Matrix4f(projMat).invert();
			Matrix4f invertedModelViewMat = new Matrix4f(stack.last().pose()).invert();
			float shiftMovement = Mth.lerp(partialTick, this.shiftMovementO, this.shiftMovement);
			float yaw = (float)Mth.atan2((double)this.windDirection.x, (double)this.windDirection.y);
			float transition = Mth.lerp(partialTick, this.transitionO, this.transition);

			float alpha = 1.0F;
			Entity cameraEntity = this.mc.gameRenderer.getMainCamera().getEntity();
			if (cameraEntity instanceof LivingEntity living)
			{
				var map = living.getActiveEffectsMap();
				if (map.containsKey(MobEffects.BLINDNESS))
				{
					MobEffectInstance instance = map.get(MobEffects.BLINDNESS);
					alpha = instance.isInfiniteDuration() ? 0.0F : 1.0F - Mth.clamp((float)instance.getDuration() / 20.0F, 0.0F, 1.0F);
				}
				else if (map.containsKey(MobEffects.DARKNESS))
				{
					MobEffectInstance instance = map.get(MobEffects.DARKNESS);
					if (instance.getFactorData().isPresent())
						alpha = 1.0F - Mth.clamp(instance.getFactorData().get().getFactor(living, partialTick), 0.0F, 1.0F);
				}
			}
			
			List<PostPass> passes = ((MixinPostChain)this.postProcessingShader).simpleclouds$getPostPasses();
			updatePass(passes.get(0), invertedProjMat, invertedModelViewMat, camX, camY, camZ, yaw, shiftMovement, 1.0F - transition, this.formation, r, g, b, alpha);
			updatePass(passes.get(1), invertedProjMat, invertedModelViewMat, camX, camY, camZ, yaw, shiftMovement, transition, this.nextFormation != null ? this.nextFormation : DEFAULT, r, g, b, alpha);
			
			this.postProcessingShader.process(partialTick);
			
			RenderSystem.depthMask(true);
		}
	}
	
	private static void updatePass(PostPass pass, Matrix4f invertedProjMat, Matrix4f invertedModelViewMat, double camX, double camY, double camZ, float yaw, float shiftMovement, float densityMult, AtmosphericCloudsRenderHandler.Formation formation, float r, float g, float b, float a)
	{
		EffectInstance effect = pass.getEffect();
		effect.safeGetUniform("InverseWorldProjMat").set(invertedProjMat);
		effect.safeGetUniform("InverseModelViewMat").set(invertedModelViewMat);
//		effect.safeGetUniform("CameraPos").set((float)camX, (float)camY, (float)camZ);
		Matrix2f transform = new Matrix2f().identity();
		transform.scale(formation.scaleX, formation.scaleZ);
		transform.rotateLocal(yaw);
		effect.safeGetUniform("Transform").setMat2x2(transform.m00, transform.m01, transform.m10, transform.m11);
		effect.safeGetUniform("ShiftMovement").set(shiftMovement);
		effect.safeGetUniform("CloudDensity").set(formation.density * densityMult);
		effect.safeGetUniform("CloudColor").set(r, g, b, a);
	}
	
	public void init(ResourceManager manager)
	{
		this.close();
		
		try
		{
			RenderTarget main = SimpleCloudsCompatHelper.getMainRenderTarget();
			if (main == null)
			{
				LOGGER.warn("Main framebufer is null");
				return;
			}
			this.postProcessingShader = new PostChain(this.mc.getTextureManager(), manager, main, SHADER_LOC);
			if (((MixinPostChain)this.postProcessingShader).simpleclouds$getPostPasses().size() != 2)
				throw new IllegalArgumentException("Expected two post passes in shader");
			this.postProcessingShader.resize(main.width, main.height);
		}
		catch (JsonSyntaxException e)
		{
			LOGGER.warn("Failed to parse post shader: {}", SHADER_LOC, e);
			this.close();
		}
		catch (IOException | IllegalArgumentException e)
		{
			LOGGER.warn("Failed to load post shader: {}", SHADER_LOC, e);
			this.close();
		}
	}
	
	public void onResize(int width, int height)
	{
		RenderTarget main = SimpleCloudsCompatHelper.getMainRenderTarget();
		if (main == null)
			return;
		
		width = main.width;
		height = main.height;
		
		if (this.postProcessingShader != null)
			this.postProcessingShader.resize(width, height);
	}
	
	public void close()
	{
		if (this.postProcessingShader != null)
		{
			this.postProcessingShader.close();
			this.postProcessingShader = null;
		}
		
		this.formation = DEFAULT;
		this.nextFormation = null;
		this.transition = 0.0F;
		this.transitionO = 0.0F;
		this.tickCount = 0;
		this.shiftMovement = 0.0F;
		this.shiftMovementO = 0.0F;
		this.windDirection = new Vector2f(1.0F, 0.0F);
	}
	
	static record Formation(Predicate<Holder<Biome>> rendersIn, float density, float scaleX, float scaleZ) {}
}
