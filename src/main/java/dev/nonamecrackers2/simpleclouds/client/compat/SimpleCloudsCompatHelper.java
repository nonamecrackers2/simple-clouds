package dev.nonamecrackers2.simpleclouds.client.compat;

import javax.annotation.Nullable;

import com.mojang.blaze3d.pipeline.RenderTarget;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.gui.SimpleCloudsNoticeScreen;
import dev.nonamecrackers2.simpleclouds.client.mesh.RendererInitializeResult;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.vivecraft.SimpleCloudsReloadVivecraftCompatWrapper;
import dev.nonamecrackers2.simpleclouds.client.vivecraft.SimpleCloudsVivecraftCompatHandler;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import nonamecrackers2.crackerslib.common.compat.CompatHelper;

public class SimpleCloudsCompatHelper
{
	public static ResourceManagerReloadListener getRendererReloadListener(SimpleCloudsRenderer renderer)
	{
		if (CompatHelper.isVivecraftLoaded())
			return new SimpleCloudsReloadVivecraftCompatWrapper(renderer);
		else
			return renderer;
	}
	
	public static @Nullable RenderTarget getMainRenderTarget()
	{
		if (CompatHelper.isVivecraftLoaded())
			return SimpleCloudsVivecraftCompatHandler.getMainFrameBuffer();
		else
			return Minecraft.getInstance().getMainRenderTarget();
	}
	
	public static boolean renderThisPass()
	{
		if (CompatHelper.isVivecraftLoaded())
			return SimpleCloudsVivecraftCompatHandler.renderThisPass();
		else
			return true;
	}
	
	public static boolean isPrimaryPass()
	{
		if (CompatHelper.isVivecraftLoaded())
			return SimpleCloudsVivecraftCompatHandler.isPrimaryPass();
		else
			return true;
	}
	
	public static int getStormFogResolutionDivisor()
	{
		if (CompatHelper.isVivecraftLoaded())
			return SimpleCloudsVivecraftCompatHandler.getStormFogResolutionDivisor();
		else
			return 4;
	}
	
	public static @Nullable SimpleCloudsNoticeScreen createNotice()
	{
		if (CompatHelper.isVivecraftLoaded() && SimpleCloudsConfig.CLIENT.showVivecraftNotice.get())
		{
			SimpleCloudsConfig.CLIENT.showVivecraftNotice.set(false);
			return new SimpleCloudsNoticeScreen(Component.translatable("gui.simpleclouds.notice.vivecraft"));
		}
		return null;
	}
	
	public static @Nullable RendererInitializeResult findCompatErrors()
	{
		RendererInitializeResult.Builder result = RendererInitializeResult.builder();
		if (CompatHelper.isOculusLoaded() && SimpleCloudsMod.dhLoaded())
			result.addError(null, "Simple Clouds Notice", Component.translatable("gui.simpleclouds.error.compat.dh_oculus"));
		return result.build();
	}
}
