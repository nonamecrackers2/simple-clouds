package dev.nonamecrackers2.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.blaze3d.pipeline.RenderTarget;

import dev.nonamecrackers2.simpleclouds.client.accessor.PostPassAccessor;
import net.minecraft.client.renderer.PostPass;

@Mixin(PostPass.class)
public abstract class MixinPostPass implements PostPassAccessor
{
	@Unique
	private boolean outClearDisabled;
	
	@Override
	public void disableOutClear()
	{
		this.outClearDisabled = true;
	}
	
	@Redirect(method = "process", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;clear(Z)V"))
	public void simpleclouds$disableOutClear_process(RenderTarget target, boolean clearErrors)
	{
		if (!this.outClearDisabled)
			target.clear(clearErrors);
	}
}
