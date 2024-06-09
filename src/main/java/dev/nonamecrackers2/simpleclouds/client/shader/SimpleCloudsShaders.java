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
	private static ShaderInstance stormReflection;
	
	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException
	{
		event.registerShader(new ShaderInstance(event.getResourceProvider(), SimpleCloudsMod.id("clouds"), DefaultVertexFormat.POSITION_COLOR_NORMAL), s -> {
			clouds = s;
		});
		event.registerShader(new ShaderInstance(event.getResourceProvider(), SimpleCloudsMod.id("storm_reflection"), DefaultVertexFormat.POSITION_COLOR_NORMAL), s -> {
			stormReflection = s;
		});
	}
	
	public static ShaderInstance getCloudsShader()
	{
		return Objects.requireNonNull(clouds, "Clouds shader not initialized yet");
	}
	
	public static ShaderInstance getStormReflectionShader()
	{
		return Objects.requireNonNull(stormReflection, "Storm reflection shader not initialized yet");
	}
}
