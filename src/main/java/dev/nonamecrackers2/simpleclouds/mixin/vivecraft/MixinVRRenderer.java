package dev.nonamecrackers2.simpleclouds.mixin.vivecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import net.minecraft.client.Minecraft;

// Reload the Simple Clouds renderer when Vivecraft framebuffers initialize
@Pseudo
@Mixin(targets = "org.vivecraft.client_vr.provider.VRRenderer", remap = false)
public class MixinVRRenderer
{
	@Inject(method = "setupRenderConfiguration", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;onResourceManagerReload(Lnet/minecraft/server/packs/resources/ResourceManager;)V"))
	public void simpleclouds$reloadRenderer_setupRenderConfiguration(CallbackInfo ci)
	{
		SimpleCloudsRenderer.getOptionalInstance().ifPresent(renderer -> {
			renderer.onResourceManagerReload(Minecraft.getInstance().getResourceManager());
		});
	}
}
