package dev.nonamecrackers2.simpleclouds.mixin.skyaesthetics;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;

@Mixin(targets = "fr.tathan.sky_aesthetics.client.skies.renderer.SkyRenderer")
public class MixinSkyRenderer
{
	@Inject(method = "render", at = @At("TAIL"))
	public void simpleclouds$renderAfterCustomSky_render(ClientLevel level, PoseStack stack, Matrix4f projMat, float partialTick, Camera camera, Runnable fogCallback, CallbackInfo ci)
	{
		if (SimpleCloudsRenderer.canRenderInDimension(level))
			SimpleCloudsRenderer.getInstance().renderAfterSky(projMat, stack.last().pose(), partialTick, camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
	}
}
