package dev.nonamecrackers2.simpleclouds.client.world;

import net.minecraft.client.renderer.RenderType;

public enum FogRenderMode
{
	VANILLA,
	SCREEN_SPACE,
	OFF;
	
	public static boolean shouldUseTranslucency(RenderType type)
	{
		return type == RenderType.solid() || type == RenderType.cutout() || type == RenderType.cutoutMipped();
	}
}
