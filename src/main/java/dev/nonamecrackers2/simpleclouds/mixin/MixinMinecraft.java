package dev.nonamecrackers2.simpleclouds.mixin;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.nonamecrackers2.simpleclouds.client.compat.SimpleCloudsCompatHelper;
import dev.nonamecrackers2.simpleclouds.client.gui.SimpleCloudsErrorScreen;
import dev.nonamecrackers2.simpleclouds.client.gui.SimpleCloudsNoticeScreen;
import dev.nonamecrackers2.simpleclouds.client.mesh.RendererInitializeResult;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.shader.buffer.BindingManager;
import dev.nonamecrackers2.simpleclouds.client.world.ClientCloudManager;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManagerHolder;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.client.extensions.IForgeMinecraft;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft implements IForgeMinecraft
{
	@Inject(method = "fillReport", at = @At("HEAD"))
	public void simpleclouds$appendCrashReportDetails_fillReport(CrashReport report, CallbackInfoReturnable<CrashReport> ci)
	{
		SimpleCloudsRenderer.getOptionalInstance().ifPresent(renderer -> {
			renderer.fillReport(report);
		});
		BindingManager.fillReport(report);
	}

	@Inject(method = "setInitialScreen", at = @At("HEAD"), cancellable = true)
	public void simpleclouds$beforeMainTitleScreen_setInitialScreen(CallbackInfo ci)
	{
		SimpleCloudsNoticeScreen notice = SimpleCloudsCompatHelper.createNotice();
		if (notice != null)
		{
			this.pushGuiLayer(notice);
			ci.cancel();
		}
		
		var renderer = SimpleCloudsRenderer.getOptionalInstance().orElse(null);
		if (renderer != null)
		{
			RendererInitializeResult result = renderer.getInitialInitializationResult();
			if (result != null && result.getState() == RendererInitializeResult.State.ERROR)
			{
				this.pushGuiLayer(new SimpleCloudsErrorScreen(renderer.getInitialInitializationResult()));
				ci.cancel();
			}
		}
	}
	
	@Inject(method = "setLevel", at = @At("TAIL"))
	public void simpleclouds$onClientLevelChange_setLevel(@Nullable ClientLevel level, CallbackInfo ci)
	{
		if (level instanceof CloudManagerHolder)
		{
			SimpleCloudsRenderer.getOptionalInstance().ifPresent(renderer -> 
			{
				ClientCloudManager manager = (ClientCloudManager)CloudManager.get(level);
				renderer.onCloudManagerChange(manager);
			});
		}
	}

	@Shadow
	public abstract void setScreen(@Nullable Screen screen);
}
