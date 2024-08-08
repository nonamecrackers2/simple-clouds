package dev.nonamecrackers2.simpleclouds.client.renderer;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WorldEffects
{
	public static final float EFFECTS_STRENGTH_MULTIPLER = 1.2F;
	private static final int RAINY_WATER_COLOR = 0xFF303030;
	private final Minecraft mc;
	private final SimpleCloudsRenderer renderer;
	private @Nullable CloudType typeAtCamera;
	private float storminessAtCamera;
	private float storminessSmoothed;
	private float storminessSmoothedO;
	
	protected WorldEffects(Minecraft mc, SimpleCloudsRenderer renderer)
	{
		this.mc = mc;
		this.renderer = renderer;
	}
	
	public void renderPost(float partialTick, double camX, double camY, double camZ, float scale)
	{
		Pair<CloudType, Float> result = CloudManager.get(this.mc.level).getCloudTypeAtPosition((float)camX, (float)camZ);//generator.getCloudTypeAtOrigin();
		CloudType type = result.getLeft();
		this.typeAtCamera = type;
		if (type.weatherType().causesDarkening() && (float)camY < type.stormStart() * scale + 128.0F)
		{
			float factor = Mth.clamp((1.0F - result.getRight()) * 3.0F, 0.0F, 1.0F);
			this.storminessAtCamera = type.storminess() * factor;
		}
		else
		{
			this.storminessAtCamera = 0.0F;
		}
	}
	
	public float getStorminessAtCamera()
	{
		return this.storminessAtCamera;
	}
	
	public void tick()
	{
		this.storminessSmoothedO = this.storminessSmoothed;
		this.storminessSmoothed += (this.storminessAtCamera - this.storminessSmoothed) / 25.0F;
	}
	
	public @Nullable CloudType getCloudTypeAtCamera()
	{
		return this.typeAtCamera;
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
	
	//While this works, the water color only changes when the chunk is rebuilt. TODO: Will need some sort of shader to modify the water color
	@Deprecated
	public int modifyWaterColor(int color)
	{
		return FastColor.ARGB32.lerp(this.getDarkenFactor(0.0F), RAINY_WATER_COLOR, color);
	}
	
	@SubscribeEvent
	public static void modifyFogColor(ViewportEvent.ComputeFogColor event)
	{
//		float factor = SimpleCloudsRenderer.getInstance().getWorldEffectsManager().getDarkenFactor((float)event.getPartialTick(), 1.5F);
//		event.setRed(event.getRed() * factor);
//		event.setGreen(event.getGreen() * factor);
//		event.setBlue(event.getBlue() * factor);
	}
	
	@SubscribeEvent
	public static void modifyFog(ViewportEvent.RenderFog event)
	{
//		float factor = SimpleCloudsRenderer.getInstance().getWorldEffectsManager().getDarkenFactor((float)event.getPartialTick(), 1.4F);
//		event.setNearPlaneDistance(event.getNearPlaneDistance() * factor);
//		event.setCanceled(true);
		if (event.getMode() == FogMode.FOG_TERRAIN)
			FogRenderer.setupNoFog();
	}
}
