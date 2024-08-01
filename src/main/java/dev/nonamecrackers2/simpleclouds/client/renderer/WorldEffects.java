package dev.nonamecrackers2.simpleclouds.client.renderer;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector2d;
import org.joml.Vector3d;

import dev.nonamecrackers2.simpleclouds.client.cloud.ClientSideCloudTypeManager;
import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudInfo;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WorldEffects
{
	public static final float EFFECTS_STRENGTH_MULTIPLER = 1.2F;
	private final Minecraft mc;
	private final SimpleCloudsRenderer renderer;
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
		CloudMeshGenerator generator = this.renderer.getMeshGenerator();
		CloudInfo info = generator.getCloudTypeAtOrigin();
		if (info != null && info.weatherType().causesDarkening() && (float)camY < info.stormStart() * scale + 128.0F)
		{
			float factor = Mth.clamp((1.0F - generator.getCloudFadeAtOrigin()) * 3.0F, 0.0F, 1.0F);
			this.storminessAtCamera = info.storminess() * factor;
		}
		else
		{
			this.storminessAtCamera = 0.0F;
		}
		
//		CloudManager manager = CloudManager.get(this.mc.level);
//		float chunkSizeUpscaled = 32.0F * scale;
//		float camOffsetX = ((float)Mth.floor(camX / chunkSizeUpscaled) * 32.0F);
//		float camOffsetZ = ((float)Mth.floor(camZ / chunkSizeUpscaled) * 32.0F);
//		var infoTest = CPUBasedCloudRegionTest.getCloudTypeAt(new Vector2d().add(manager.getScrollX(), manager.getScrollZ()).add(camOffsetX, camOffsetZ).div(2000.0F), ClientSideCloudTypeManager.getInstance().getIndexed());
//		System.out.println("=====================");
//		System.out.println("Predicted:");
//		System.out.println("	" + ClientSideCloudTypeManager.getInstance().getIndexed()[infoTest.getLeft()]);
//		System.out.println("	" + infoTest.getRight());
//		float fade = generator.getCloudFadeAtOrigin();
//		System.out.println("Actual:");
//		System.out.println("	" + info);
//		System.out.println("	" + fade);
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
