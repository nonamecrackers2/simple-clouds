package dev.nonamecrackers2.simpleclouds.mixin;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.Window;

import dev.nonamecrackers2.simpleclouds.client.compat.SimpleCloudsCompatHelper;
import dev.nonamecrackers2.simpleclouds.client.gui.CloudPreviewerScreen;
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
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.extensions.IMinecraftExtension;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft implements IMinecraftExtension
{
	@Shadow
	public @Nullable Screen screen;
	@Shadow @Final
	private Window window;
	
	@Inject(method = "fillReport", at = @At("HEAD"))
	public void simpleclouds$appendCrashReportDetails_fillReport(CrashReport report, CallbackInfoReturnable<CrashReport> ci)
	{
		SimpleCloudsRenderer.getOptionalInstance().ifPresent(renderer -> {
			renderer.fillReport(report);
		});
		BindingManager.fillReport(report);
	}

	@Inject(method = "addInitialScreens", at = @At("HEAD"))
	private void simpleclouds$addErrorScreens_addInitialScreens(List<Function<Runnable, Screen>> screenFactory, CallbackInfo ci)
	{
		var renderer = SimpleCloudsRenderer.getOptionalInstance().orElse(null);
		if (renderer != null)
		{
			RendererInitializeResult result = renderer.getInitialInitializationResult();
			if (result != null && result.getState() == RendererInitializeResult.State.ERROR)
				screenFactory.add(onClose -> new SimpleCloudsErrorScreen(renderer.getInitialInitializationResult(), onClose));
		}
	}
	
	@Inject(method = "addInitialScreens", at = @At("TAIL"))
	private void simpleclouds$addInfoScreens_addInitialScreens(List<Function<Runnable, Screen>> screenFactory, CallbackInfo ci)
	{
		SimpleCloudsNoticeScreen notice = SimpleCloudsCompatHelper.createNotice();
		if (notice != null)
		{
			screenFactory.add(onClose -> {
				notice.setOnClose(onClose);
				return notice;
			});
		}
	}
	
	@Inject(method = "setLevel", at = @At("TAIL"))
	public void simpleclouds$onClientLevelChange_setLevel(@Nullable ClientLevel level, ReceivingLevelScreen.Reason reason, CallbackInfo ci)
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
	
	@Inject(method = "getFramerateLimit", at = @At("TAIL"), cancellable = true)
	public void simpleclouds$overrideFramerateLimit_getFramerateLimit(CallbackInfoReturnable<Integer> ci)
	{
		if (this.screen instanceof CloudPreviewerScreen)
			ci.setReturnValue(this.window.getFramerateLimit());
	}

	@Shadow
	public abstract void setScreen(@Nullable Screen screen);
}
