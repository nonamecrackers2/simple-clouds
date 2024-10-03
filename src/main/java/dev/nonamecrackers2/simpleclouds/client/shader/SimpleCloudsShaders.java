package dev.nonamecrackers2.simpleclouds.client.shader;

import java.io.IOException;
import java.util.Objects;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.mixin.MixinVertexFormatElement;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

public class SimpleCloudsShaders
{
	public static final VertexFormatElement BRIGHTNESS;
	public static final VertexFormatElement NORMAL_INDEX;
	public static final VertexFormat POSITION_BRIGHTNESS_NORMAL_INDEX;
	private static ShaderInstance clouds;
	private static ShaderInstance cloudsShadowMap;
	private static ShaderInstance cloudRegionTex;
	
	static
	{
		BRIGHTNESS = attemptToRegisterUsingNextId(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.COLOR, 1);
		NORMAL_INDEX = attemptToRegisterUsingNextId(0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.UV, 1);
		POSITION_BRIGHTNESS_NORMAL_INDEX = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("Brightness", BRIGHTNESS).add("Index", NORMAL_INDEX).build();
	}
	
	private static VertexFormatElement attemptToRegisterUsingNextId(int index, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int count)
	{
		int currentId = -1;
		VertexFormatElement[] byId = MixinVertexFormatElement.simpleclouds$getByID();
		for (int i = 0; i < byId.length; i++)
		{
			if (byId[i] == null)
				currentId = i;
		}
		if (currentId == -1)
			throw new IllegalStateException("No available ID was found for crucial shader attributes. Please report this to the Simple Clouds devs.");
		return VertexFormatElement.register(currentId, index, type, usage, count);
	}
	
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
