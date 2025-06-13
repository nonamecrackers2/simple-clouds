package dev.nonamecrackers2.simpleclouds.client.framebuffer.compat;

import java.util.function.Supplier;

import com.mojang.blaze3d.pipeline.RenderTarget;

import dev.nonamecrackers2.simpleclouds.client.vivecraft.SimpleCloudsVivecraftCompatHandler;
import net.minecraft.client.Minecraft;
import nonamecrackers2.crackerslib.common.compat.CompatHelper;

public class MainRenderTargetHelper
{
	public static RenderTarget getMainTarget(boolean createWrappedGetter)
	{
		Supplier<RenderTarget> getter = () -> 
		{
			if (CompatHelper.isVrActive())
				return SimpleCloudsVivecraftCompatHandler.getMainFrameBuffer();
			else
				return Minecraft.getInstance().getMainRenderTarget();
		};
		if (createWrappedGetter)
			return WrappedRenderTarget.wrapped(getter);
		else
			return getter.get();
	}
	
	public static boolean useWrapped()
	{
		return CompatHelper.isVivecraftLoaded();
	}
}
