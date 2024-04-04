package dev.nonamecrackers2.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;

@Mixin(GameRenderer.class)
public interface MixinGameRendererAccessor
{
	@Invoker("getFov")
	public double simpleclouds$getFov(Camera camera, float partialTicks, boolean useConfiguredFov);
}
