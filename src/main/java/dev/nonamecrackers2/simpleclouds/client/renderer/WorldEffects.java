package dev.nonamecrackers2.simpleclouds.client.renderer;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.nonamecrackers2.simpleclouds.client.renderer.lightning.LightningBolt;
import dev.nonamecrackers2.simpleclouds.client.renderer.rain.PrecipitationQuad;
import dev.nonamecrackers2.simpleclouds.client.sound.AdjustableAttenuationSoundInstance;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.init.SimpleCloudsSounds;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nonamecrackers2.crackerslib.common.compat.CompatHelper;

public class WorldEffects
{
	public static final float EFFECTS_STRENGTH_MULTIPLER = 1.2F;
	public static final int RAIN_SCAN_WIDTH = 32;
	public static final int RAIN_SCAN_HEIGHT = 8;
	public static final int RAIN_HEIGHT_OFFSET = 8;
	public static final int RAIN_SOUND_INTERVAL_MODIFIER = 100;
	public static final SimpleWeightedRandomList<Integer> LIGHTNING_COLORS = SimpleWeightedRandomList.<Integer>builder()
			.add(0xFFFFFFFF, 30) // White
			.add(0xFF8C80FF, 13) // Blue
			.add(0xFF8C80FF, 12) // Purple
			.add(0xFFF0FFB4, 10) // Yellow
			.add(0xFFFFB4BE, 5) // Red
			.build();
//	private static final int RAINY_WATER_COLOR = 0xFF303030;
	private final Minecraft mc;
	private final SimpleCloudsRenderer renderer;
	private @Nullable CloudType typeAtCamera;
	private float fadeAtCamera;
	private float storminessAtCamera;
	private float storminessSmoothed;
	private float storminessSmoothedO;
	private final List<LightningBolt> lightningBolts = Lists.newArrayList();
	private final Map<BlockPos, PrecipitationQuad> precipitationQuads = Maps.newHashMap();
	private final Map<Biome.Precipitation, List<PrecipitationQuad>> quadsByPrecipitation = Maps.newHashMap();
	private final RandomSource random = RandomSource.create();
	
	protected WorldEffects(Minecraft mc, SimpleCloudsRenderer renderer)
	{
		this.mc = mc;
		this.renderer = renderer;
	}
	
	public void renderPost(Matrix4f camMat, float partialTick, double camX, double camY, double camZ, float scale)
	{
		CloudManager<ClientLevel> manager = CloudManager.get(this.mc.level);
		Pair<CloudType, Float> result = manager.getCloudTypeAtPosition((float)camX, (float)camZ);
		CloudType type = result.getLeft();
		this.typeAtCamera = type;
		this.fadeAtCamera = result.getRight();
		
		if (!manager.shouldUseVanillaWeather() && type.weatherType().causesDarkening())
		{
			float verticalFade = 1.0F - Mth.clamp(((float)camY - (type.stormStart() * SimpleCloudsConstants.CLOUD_SCALE + manager.getCloudHeight())) / SimpleCloudsConstants.RAIN_VERTICAL_FADE, 0.0F, 1.0F);
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
	
	public void renderWeather(LightTexture texture, float partialTick, double camX, double camY, double camZ)
	{
		Tesselator tesselator = Tesselator.getInstance();
		RenderSystem.depthMask(Minecraft.useShaderTransparency() || CompatHelper.areShadersRunning());
		RenderSystem.colorMask(true, true, true, true);
		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		
		if (!this.lightningBolts.isEmpty())
		{
			float currentFogStart = RenderSystem.getShaderFogStart();
			RenderSystem.setShaderFogStart(Float.MAX_VALUE);
			RenderSystem.applyModelViewMatrix();
			BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			RenderSystem.setShader(GameRenderer::getRendertypeLightningShader);
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
			MeshData meshData = builder.build();
			if (meshData != null)
				BufferUploader.drawWithShader(meshData);
			RenderSystem.applyModelViewMatrix();
			RenderSystem.setShaderFogStart(currentFogStart);
		}
		
		if (!this.quadsByPrecipitation.isEmpty())
		{
			texture.turnOnLightLayer();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableCull();
			RenderSystem.setShader(GameRenderer::getParticleShader);
			for (var entry : this.quadsByPrecipitation.entrySet())
			{
				RenderSystem.setShaderTexture(0, PrecipitationQuad.TEXTURE_BY_PRECIPITATION.get(entry.getKey()));
				BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
				PoseStack stack = new PoseStack();
				stack.translate(-camX, -camY, -camZ);
				for (PrecipitationQuad quad : entry.getValue())
				{
					stack.pushPose();
					int packedLight = LevelRenderer.getLightColor(this.mc.level, quad.getBlockPos());
					quad.render(stack, builder, partialTick, packedLight, camX, camY, camZ);
					stack.popPose();
				}
				MeshData meshData = builder.build();
				if (meshData != null)
					BufferUploader.drawWithShader(meshData);
			}
			RenderSystem.enableCull();
		}
		
		RenderSystem.disableBlend();
		RenderSystem.defaultBlendFunc();
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
		int attenuation = SimpleCloudsConfig.CLIENT.thunderAttenuationDistance.get();
		float dist = vec.distance((float)cameraPos.x, (float)cameraPos.y, (float)cameraPos.z);
		if (dist < SimpleCloudsConstants.CLOSE_THUNDER_CUTOFF)
		{
			sound = SimpleCloudsSounds.CLOSE_THUNDER.get();
			attenuation = SimpleCloudsConstants.CLOSE_THUNDER_CUTOFF;
		}
		float fade = 1.0F - Math.min(Math.max(dist - (float)SimpleCloudsConstants.THUNDER_PITCH_FULL_DIST, 0.0F) / ((float)SimpleCloudsConstants.THUNDER_PITCH_MINIMUM_DIST - (float)SimpleCloudsConstants.THUNDER_PITCH_FULL_DIST), 1.0F);
		RandomSource random = RandomSource.create((long)seed);
		AdjustableAttenuationSoundInstance instance = new AdjustableAttenuationSoundInstance(sound, SoundSource.WEATHER, 1.0F + this.random.nextFloat() * 4.0F, 0.5F + fade * 0.5F, random, (double)pos.getX() + 0.5D, (float)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, attenuation);
		int time = Mth.floor(dist / SimpleCloudsConstants.SOUND_METERS_PER_SECOND) * 20; 
		this.mc.getSoundManager().playDelayed(instance, time);
		if (!onlySound)
		{
			int color = LIGHTNING_COLORS.getRandomValue(random).get();
			float r = 1.0F;
			float g = 1.0F;
			float b = 1.0F;
			if (SimpleCloudsConfig.CLIENT.lightningColorVariation.get())
			{
				r = (float)FastColor.ARGB32.red(color) / 255.0F;
				g = (float)FastColor.ARGB32.green(color) / 255.0F;
				b = (float)FastColor.ARGB32.blue(color) / 255.0F;
			}
			this.lightningBolts.add(new LightningBolt(random, vec, depth, branchCount, maxBranchLength, maxWidth, minimumPitch, maximumPitch, r, g, b));
		}
	}
//	
//	public void modifyLightMapTexture(float partialTick, int pixelX, int pixelY, Vector3f color)
//	{
//	}
	
	public float getStorminessAtCamera()
	{
		return this.storminessAtCamera;
	}
	
	public void tick()
	{
		var lightning = this.lightningBolts.iterator();
		while (lightning.hasNext())
		{
			LightningBolt bolt = lightning.next();
			if (bolt.isDead())
				lightning.remove();
			bolt.tick();
		}
		
		float rainIntensity = this.mc.level.getRainLevel(0.0F);
		BlockPos camPos = this.mc.gameRenderer.getMainCamera().getBlockPosition();
		float xRot = SimpleCloudsConfig.CLIENT.rainAngle.get().floatValue() * ((float)Math.PI / 180.0F);
		Vector3f direction = CloudManager.get(this.mc.level).getDirection();
		float yRot = (float)-Mth.atan2((double)direction.x, (double)direction.z);
		float xRotCos = Mth.cos(xRot - (float)Math.PI / 2.0F);
		int xOffset = Mth.floor(Mth.sin(-yRot) * xRotCos * ((float)RAIN_SCAN_WIDTH / 2.0F));
		int zOffset = Mth.floor(Mth.cos(-yRot) * xRotCos * ((float)RAIN_SCAN_WIDTH / 2.0F));
		int radius = Mth.floor((float)RAIN_SCAN_WIDTH / 2.0F * (Minecraft.useFancyGraphics() ? 1.0F : 0.5F));
		int minX = camPos.getX() - radius - xOffset;
		int minY = camPos.getY() + RAIN_HEIGHT_OFFSET;
		int minZ = camPos.getZ() - radius - zOffset;
		int maxX = camPos.getX() + radius - xOffset;
		int maxY = camPos.getY() + RAIN_SCAN_HEIGHT + RAIN_HEIGHT_OFFSET;
		int maxZ = camPos.getZ() + radius - zOffset;
		AABB box = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
		Biome biome = this.mc.level.getBiome(camPos).value();
		if (rainIntensity > 0.0F && biome.hasPrecipitation())
		{
			for (int x = minX; x < maxX; x++)
			{
				for (int z = minZ; z < maxZ; z++)
				{
					int height = this.mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
					for (int y = minY; y < maxY; y++)
					{
						if (height > y)
							continue;
						BlockPos pos = new BlockPos(x, y, z);
						Biome.Precipitation precipitation = biome.getPrecipitationAt(pos);
						RandomSource blockRandom = RandomSource.create(pos.asLong());
						if (!this.precipitationQuads.containsKey(pos))
						{
							if (blockRandom.nextInt(100) <= 2)
							{
								float widthModifier = precipitation == Biome.Precipitation.SNOW ? 4.0F : 2.0F;
								PrecipitationQuad quad = new PrecipitationQuad(precipitation, this.mc.level::clip, pos, xRot + this.random.nextFloat() * 0.1F, yRot + this.random.nextFloat() * 0.1F, 60 + this.random.nextInt(60), rainIntensity * widthModifier);
								this.precipitationQuads.put(pos, quad);
								this.quadsByPrecipitation.computeIfAbsent(precipitation, p -> Lists.newArrayList()).add(quad);
							}
						}
					}
				}
			}
		}
		
		var rain = this.precipitationQuads.entrySet().iterator();
		while (rain.hasNext())
		{
			var entry = rain.next();
			PrecipitationQuad quad = entry.getValue();
			BlockPos pos = entry.getKey();
			if (!box.contains(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) || quad.isDead())
			{
				rain.remove();
				this.quadsByPrecipitation.get(quad.getPrecipitation()).remove(quad);
			}
			else
			{
				quad.tick();
			}
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
