package dev.nonamecrackers2.simpleclouds.client.shader;

import java.io.IOException;
import java.util.Objects;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SimpleCloudsShaders
{
	private static ShaderInstance clouds;
	private static ShaderInstance cloudsShadowMap;
	private static ShaderInstance cloudRegionTex;
	
	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException
	{
		event.registerShader(new ShaderInstance(event.getResourceProvider(), SimpleCloudsMod.id("clouds"), DefaultVertexFormat.POSITION_COLOR_NORMAL), s -> {
			clouds = s;
		});
		event.registerShader(new ShaderInstance(event.getResourceProvider(), SimpleCloudsMod.id("clouds_shadow_map"), DefaultVertexFormat.POSITION_COLOR_NORMAL), s -> {
			cloudsShadowMap = s;
		});
		event.registerShader(new ShaderInstance(event.getResourceProvider(), SimpleCloudsMod.id("cloud_region_tex"), DefaultVertexFormat.POSITION_TEX), s -> {
			cloudRegionTex = s;
		});
	}
	
	public static ShaderInstance getCloudsShader()
	{
		return Objects.requireNonNull(clouds, "Clouds shader not initialized yet");
	}
	
	public static ShaderInstance getCloudsShadowMapShader()
	{
		return Objects.requireNonNull(cloudsShadowMap, "Clouds shadow map shader not initialized yet");
	}
	
	public static ShaderInstance getCloudRegionTexShader()
	{
		return Objects.requireNonNull(cloudRegionTex, "Cloud region tex shader not initialized yet");
	}
}
