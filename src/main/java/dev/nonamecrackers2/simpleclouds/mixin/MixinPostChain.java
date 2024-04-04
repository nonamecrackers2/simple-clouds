package dev.nonamecrackers2.simpleclouds.mixin;

import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.pipeline.RenderTarget;

import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;

@Mixin(PostChain.class)
public interface MixinPostChain
{
	@Accessor("passes")
	List<PostPass> simpleclouds$getPostPasses();
	
	@Accessor("customRenderTargets")
	Map<String, RenderTarget> simpleclouds$getCustomRenderTargets();
}
