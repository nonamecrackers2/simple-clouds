package dev.nonamecrackers2.simpleclouds.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.nonamecrackers2.simpleclouds.client.gui.SimpleCloudsErrorScreen;
import dev.nonamecrackers2.simpleclouds.client.mesh.GeneratorInitializeResult;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.ComputeShader;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft
{
	@Inject(method = "fillReport", at = @At("HEAD"))
	public void simpleclouds$appendCrashReportDetails_fillReport(CrashReport report, CallbackInfoReturnable<CrashReport> ci)
	{
		SimpleCloudsRenderer.getOptionalInstance().ifPresent(renderer -> {
			renderer.fillReport(report);
		});
		ComputeShader.fillReport(report);
	}

	@Inject(method = "setInitialScreen", at = @At("HEAD"), cancellable = true)
	public void simpleclouds$beforeMainTitleScreen_setInitialScreen(CallbackInfo ci)
	{
		var renderer = SimpleCloudsRenderer.getOptionalInstance().orElse(null);
		if (renderer != null)
		{
			GeneratorInitializeResult result = renderer.getInitialInitializationResult();
			if (result != null && result.getState() == GeneratorInitializeResult.State.ERROR)
			{
				this.setScreen(new SimpleCloudsErrorScreen(renderer.getInitialInitializationResult()));
				ci.cancel();
			}
		}
	}

	@Shadow
	public abstract void setScreen(@Nullable Screen screen);
}
