package dev.nonamecrackers2.simpleclouds.mixin.vivecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.blaze3d.pipeline.RenderTarget;

@Pseudo
@Mixin(value = RenderTarget.class, priority = 1001)
public class MixinRenderTarget
{
	@Redirect(method = "modify$zfb000$vivecraft$noViewportChangeOnClear", at = @At(value = "INVOKE", target = "Lorg/vivecraft/client_xr/render_pass/RenderPassType;isWorldOnly()Z", remap = false))
	public boolean simpleclouds$disableNoViewportChangeOnClear(boolean flag)
	{
		return false;
	}
}
