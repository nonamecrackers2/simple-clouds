package dev.nonamecrackers2.simpleclouds.client.renderer;

import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector2f;
import org.joml.Vector3f;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.nonamecrackers2.simpleclouds.client.renderer.lightning.LightningBolt;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.init.SimpleCloudsSounds;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class WorldEffects
{
	public static final float EFFECTS_STRENGTH_MULTIPLER = 1.2F;
//	private static final int RAINY_WATER_COLOR = 0xFF303030;
	private final Minecraft mc;
	private final SimpleCloudsRenderer renderer;
	private @Nullable CloudType typeAtCamera;
	private float fadeAtCamera;
	private float storminessAtCamera;
	private float storminessSmoothed;
	private float storminessSmoothedO;
	private final List<LightningBolt> lightningBolts = Lists.newArrayList();
	
	protected WorldEffects(Minecraft mc, SimpleCloudsRenderer renderer)
	{
		this.mc = mc;
		this.renderer = renderer;
	}
	
	public void renderPost(PoseStack stack, float partialTick, double camX, double camY, double camZ, float scale)
	{
		CloudManager<ClientLevel> manager = CloudManager.get(this.mc.level);
		Pair<CloudType, Float> result = manager.getCloudTypeAtPosition((float)camX, (float)camZ);
		CloudType type = result.getLeft();
		this.typeAtCamera = type;
		this.fadeAtCamera = result.getRight();
		
		if (!manager.shouldUseVanillaWeather() && type.weatherType().causesDarkening())
		{
			float verticalFade = 1.0F - Mth.clamp(((float)camY - (type.stormStart() * SimpleCloudsConstants.CLOUD_SCALE + 128.0F)) / SimpleCloudsConstants.RAIN_VERTICAL_FADE, 0.0F, 1.0F);
			float factor = Mth.clamp((1.0F - result.getRight()) * 3.0F, 0.0F, 1.0F);
			this.storminessAtCamera = type.storminess() * factor * verticalFade;
		}
		else
		{
			this.storminessAtCamera = 0.0F;
		}
		
		if (!manager.shouldUseVanillaWeather())
		{
			float rainLevel = manager.getRainLevel((float)camX, (float)camY, (float)camZ);
			this.mc.level.setRainLevel(rainLevel);
		}
	}
	
	public void renderWeather(float partialTick, double camX, double camY, double camZ)
	{
		if (!this.lightningBolts.isEmpty())
		{
			PoseStack modelViewStack = RenderSystem.getModelViewStack();
			modelViewStack.pushPose();
			RenderSystem.applyModelViewMatrix();
			Tesselator tesselator = Tesselator.getInstance();
			BufferBuilder builder = tesselator.getBuilder();
			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			RenderSystem.setShader(GameRenderer::getRendertypeLightningShader);
			RenderSystem.depthMask(Minecraft.useShaderTransparency());
			RenderSystem.colorMask(true, true, true, true);
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
			PoseStack stack = new PoseStack();
			stack.pushPose();
			stack.translate(-camX, -camY, -camZ);
			for (LightningBolt bolt : this.lightningBolts)
			{
				if (bolt.getPosition().distance((float)camX, (float)camY, (float)camZ) <= SimpleCloudsConstants.CLOSE_THUNDER_CUTOFF && bolt.getFade(partialTick) > 0.5F)
					this.mc.level.setSkyFlashTime(2);
				float dist = bolt.getPosition().distance((float)camX, (float)camY, (float)camZ);
				bolt.render(stack, builder, partialTick, 1.0F, 1.0F, 1.0F, this.renderer.getFadeFactorForDistance(dist));
			}
			stack.popPose();
			tesselator.end();
			RenderSystem.disableBlend();
			RenderSystem.defaultBlendFunc();
			modelViewStack.popPose();
			RenderSystem.applyModelViewMatrix();
		}
	}
	
	public void spawnLightning(BlockPos pos, boolean onlySound, int seed, int depth, int branchCount, float maxBranchLength, float maxWidth, float minimumPitch, float maximumPitch)
	{
		Camera camera = this.mc.gameRenderer.getMainCamera();
		Vec3 cameraPos = camera.getPosition();
		Vector3f vec = new Vector3f((float)pos.getX() + 0.5F, (float)pos.getY() + 0.5F, (float)pos.getZ() + 0.5F);
		
		CloudManager<ClientLevel> manager = CloudManager.get(this.mc.level);
		if (manager.getCloudMode() == CloudMode.AMBIENT) //Prevent lightning from spawning where no clouds are using AMBIENT mode
		{
			float dist = Vector2f.distance(vec.x, vec.z, (float)cameraPos.x, (float)cameraPos.z);
			if (dist < SimpleCloudsConstants.AMBIENT_MODE_FADE_END)
				return;
		}
		
		SoundEvent sound = SimpleCloudsSounds.DISTANT_THUNDER.get();
		float dist = vec.distance((float)cameraPos.x, (float)cameraPos.y, (float)cameraPos.z);
		if (dist < SimpleCloudsConstants.CLOSE_THUNDER_CUTOFF)
			sound = SimpleCloudsSounds.CLOSE_THUNDER.get();
		float fade = 1.0F - Math.min(Math.max(dist - (float)SimpleCloudsConstants.THUNDER_PITCH_FULL_DIST, 0.0F) / ((float)SimpleCloudsConstants.THUNDER_PITCH_MINIMUM_DIST - (float)SimpleCloudsConstants.THUNDER_PITCH_FULL_DIST), 1.0F);
		RandomSource random = RandomSource.create((long)seed);
		SimpleSoundInstance instance = new SimpleSoundInstance(sound, SoundSource.WEATHER, 1.0F + random.nextFloat() * 4.0F, 0.5F + fade * 0.5F, random, (double)pos.getX() + 0.5D, (float)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D);
		int time = Mth.floor(dist / SimpleCloudsConstants.SOUND_METERS_PER_SECOND) * 20; 
		this.mc.getSoundManager().playDelayed(instance, time);
		if (!onlySound)
			this.lightningBolts.add(new LightningBolt(random, vec, depth, branchCount, maxBranchLength, maxWidth, minimumPitch, maximumPitch));
	}
	
	public void modifyLightMapTexture(float partialTick, int pixelX, int pixelY, Vector3f color)
	{
	}
	
	public float getStorminessAtCamera()
	{
		return this.storminessAtCamera;
	}
	
	public void tick()
	{
		var iterator = this.lightningBolts.iterator();
		while (iterator.hasNext())
		{
			LightningBolt bolt = iterator.next();
			if (bolt.isDead())
				iterator.remove();
			bolt.tick();
		}
		
		this.storminessSmoothedO = this.storminessSmoothed;
		this.storminessSmoothed += (this.storminessAtCamera - this.storminessSmoothed) / 25.0F;
	}
	
	public @Nullable CloudType getCloudTypeAtCamera()
	{
		return this.typeAtCamera;
	}
	
	public float getFadeRegionAtCamera()
	{
		return this.fadeAtCamera;
	}
	
	public float getStorminessSmoothed(float partialTick)
	{
		return Mth.lerp(partialTick, this.storminessSmoothedO, this.storminessSmoothed);
	}
	
	public float getDarkenFactor(float partialTick, float strength)
	{
		return Mth.clamp(1.0F - this.getStorminessSmoothed(partialTick) * strength, 0.1F, 1.0F);
	}
	
	public float getDarkenFactor(float partialTick)
	{
		return this.getDarkenFactor(partialTick, EFFECTS_STRENGTH_MULTIPLER);
	}
	
	public List<LightningBolt> getLightningBolts()
	{
		return this.lightningBolts;
	}
}
