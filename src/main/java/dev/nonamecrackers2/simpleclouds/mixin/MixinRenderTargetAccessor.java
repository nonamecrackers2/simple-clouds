package dev.nonamecrackers2.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.pipeline.RenderTarget;

@Mixin(RenderTarget.class)
public interface MixinRenderTargetAccessor
{
	@Accessor("frameBufferId")
	int simpleclouds$getFrameBufferId();
}
