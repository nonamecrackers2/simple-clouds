package dev.nonamecrackers2.simpleclouds.mixin;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;

@Mixin(LightTexture.class)
public class MixinLightTexture
{
	@Inject(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;effects()Lnet/minecraft/client/renderer/DimensionSpecialEffects;", shift = At.Shift.AFTER, ordinal = 1), locals = LocalCapture.CAPTURE_FAILHARD)
	public void simpleclouds$modifyLightTextureColor_updateLightTexture(float partialTick, CallbackInfo ci, ClientLevel clientLevel, float f, float f1, float f2, float f3, float f4, float f6, float f5, Vector3f vector3f, float f7, Vector3f vector3f1, int i, int j, float f8, float f9, float f10, float f11, boolean flag)
	{
		//SimpleCloudsRenderer.getInstance().getWorldEffectsManager().modifyLightMapTexture(partialTick, j, i, vector3f1);
	}
}
