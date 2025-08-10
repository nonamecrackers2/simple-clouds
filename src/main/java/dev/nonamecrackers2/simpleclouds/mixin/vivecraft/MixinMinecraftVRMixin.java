package dev.nonamecrackers2.simpleclouds.mixin.vivecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import net.minecraft.client.Minecraft;

//A mixin mixing into a method added by a mixin... yikes
@Pseudo
@Mixin(value = Minecraft.class, priority = 999)
public class MixinMinecraftVRMixin
{
	@Inject(method = "vivecraft$switchVRState", at = @At("TAIL"))
	public void simpleclouds$onVRStateSwitched_vivecrafft$switchVRSate(boolean vrActive, CallbackInfo ci)
	{
		SimpleCloudsRenderer.getOptionalInstance().ifPresent(renderer -> {
			renderer.onResourceManagerReload(Minecraft.getInstance().getResourceManager());
		});
	}
}
