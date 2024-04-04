package dev.nonamecrackers2.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.nonamecrackers2.simpleclouds.client.renderer.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public class MixinGameRenderer
{
	@Inject(method = "getDepthFar", at = @At("HEAD"), cancellable = true)
	public void simpleclouds$extendFarPlane_getDepthFar(CallbackInfoReturnable<Float> ci)
	{
//		if (SimpleCloudsRenderer.isExtendingFarPlane())
//		{
//			float amount = SimpleCloudsRenderer.getExtendedFarPlane();
//			if (amount < 0.0F)
//				throw new IllegalStateException("Extended far plane amount must be greater than zero!");
//			ci.setReturnValue(amount);
//		}
		ci.setReturnValue((float)CloudMeshGenerator.getCloudAreaMaxRadius() * (float)SimpleCloudsRenderer.CLOUD_SCALE);
	}
	
	@Inject(method = "close", at = @At("TAIL"))
	public void simpleclouds$shutdownRenderer_close(CallbackInfo ci)
	{
		SimpleCloudsRenderer.getInstance().shutdown();
	}
	
	@Inject(method = "resize", at = @At("TAIL"))
	public void simpleclouds$resizeRenderer_resize(int width, int height, CallbackInfo ci)
	{
		SimpleCloudsRenderer.getInstance().onResize(width, height);
	}
}
