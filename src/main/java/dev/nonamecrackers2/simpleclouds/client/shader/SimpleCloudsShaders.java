package dev.nonamecrackers2.simpleclouds.client.shader;

import java.io.IOException;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SimpleCloudsShaders
{
	public static final VertexFormatElement ELEMENT_BRIGHTNESS = new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.COLOR, 1);
	public static final VertexFormatElement ELEMENT_NORMAL_INDEX = new VertexFormatElement(0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.UV, 1);
	public static final VertexFormat POSITION_BRIGHTNESS_NORMAL_INDEX = new VertexFormat(ImmutableMap.<String, VertexFormatElement>builder().put("Position", DefaultVertexFormat.ELEMENT_POSITION).put("Brightness", ELEMENT_BRIGHTNESS).put("Index", ELEMENT_NORMAL_INDEX).build());
	private static ShaderInstance clouds;
	private static ShaderInstance cloudsShadowMap;
	private static ShaderInstance cloudRegionTex;
	
	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException
	{
		event.registerShader(new ShaderInstance(event.getResourceProvider(), SimpleCloudsMod.id("clouds"), POSITION_BRIGHTNESS_NORMAL_INDEX), s -> {
			clouds = s;
		});
		event.registerShader(new ShaderInstance(event.getResourceProvider(), SimpleCloudsMod.id("clouds_shadow_map"), POSITION_BRIGHTNESS_NORMAL_INDEX), s -> {
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
