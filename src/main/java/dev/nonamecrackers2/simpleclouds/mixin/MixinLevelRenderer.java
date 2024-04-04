package dev.nonamecrackers2.simpleclouds.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import net.minecraft.client.renderer.LevelRenderer;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer
{
	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	public void simpleclouds$overrideCloudRendering_renderClouds(PoseStack stack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ, CallbackInfo ci)
	{
		//SimpleCloudsRenderer.extendFarPlane((float)CloudMeshGenerator.getCloudAreaMaxRadius() * (float)SimpleCloudsRenderer.CLOUD_SCALE, partialTick);
		if (SimpleCloudsRenderer.isEnabled())
			SimpleCloudsRenderer.getInstance().render(stack, RenderSystem.getProjectionMatrix(), partialTick, camX, camY, camZ);
		//SimpleCloudsRenderer.resetFarPlane();
		ci.cancel();
	}
	
	@Inject(method = "tick", at = @At("HEAD"))
	public void simpleclouds$tickCloudRenderer_tick(CallbackInfo ci)
	{
		SimpleCloudsRenderer.getInstance().tick();
	}
}
