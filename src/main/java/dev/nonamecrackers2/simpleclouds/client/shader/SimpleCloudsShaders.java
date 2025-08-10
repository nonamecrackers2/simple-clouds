package dev.nonamecrackers2.simpleclouds.client.shader;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

public class SimpleCloudsShaders
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/SimpleCloudsShaders");
	private static SingleSSBOShaderInstance clouds;
	private static SingleSSBOShaderInstance cloudsTransparency;
	private static SingleSSBOShaderInstance stormFogShadowMap;
	private static SingleSSBOShaderInstance cloudsShadowMap;
	private static ShaderInstance cloudRegionTex;
	private static boolean shadersInitialized;
	private static @Nullable Throwable error;
	
	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event)
	{
		try
		{
			event.registerShader(new SingleSSBOShaderInstance(event.getResourceProvider(), SimpleCloudsMod.id("clouds"), DefaultVertexFormat.POSITION, "SideInfoBuffer"), s -> {
				clouds = (SingleSSBOShaderInstance)s;
			});
			event.registerShader(new SingleSSBOShaderInstance(event.getResourceProvider(), SimpleCloudsMod.id("clouds_transparency"), DefaultVertexFormat.POSITION, "TransparentCubeInfoBuffer"), s -> {
				cloudsTransparency = (SingleSSBOShaderInstance)s;
			});
			event.registerShader(new SingleSSBOShaderInstance(event.getResourceProvider(), SimpleCloudsMod.id("storm_fog_shadow_map"), DefaultVertexFormat.POSITION, "SideInfoBuffer"), s -> {
				stormFogShadowMap = (SingleSSBOShaderInstance)s;
			});
			event.registerShader(new SingleSSBOShaderInstance(event.getResourceProvider(), SimpleCloudsMod.id("clouds_shadow_map"), DefaultVertexFormat.POSITION, "SideInfoBuffer"), s -> {
				cloudsShadowMap = (SingleSSBOShaderInstance)s;
			});
			event.registerShader(new ShaderInstance(event.getResourceProvider(), SimpleCloudsMod.id("cloud_region_tex"), DefaultVertexFormat.POSITION_TEX), s -> {
				cloudRegionTex = s;
			});
			shadersInitialized = true;
			error = null;
		}
		catch (IOException e)
		{
			LOGGER.error("Failed to set up shaders: ", e);
			shadersInitialized = false;
			error = e;
		}
	}
	
	public static boolean areShadersInitialized()
	{
		return shadersInitialized;
	}
	
	public static @Nullable Throwable getError()
	{
		return error;
	}
	
	public static SingleSSBOShaderInstance getCloudsShader()
	{
		return Objects.requireNonNull(clouds, "Clouds shader not initialized yet");
	}
	
	public static SingleSSBOShaderInstance getCloudsTransparencyShader()
	{
		return Objects.requireNonNull(cloudsTransparency, "Clouds transparency shader not initialized yet");
	}
	
	public static SingleSSBOShaderInstance getStormFogShadowMapShader()
	{
		return Objects.requireNonNull(stormFogShadowMap, "Storm fog shadow map shader not initialized yet");
	}
	
	public static SingleSSBOShaderInstance getCloudsShadowMapShader()
	{
		return Objects.requireNonNull(cloudsShadowMap, "Clouds shadow map shader not initialized yet");
	}
	
	public static ShaderInstance getCloudRegionTexShader()
	{
		return Objects.requireNonNull(cloudRegionTex, "Cloud region tex shader not initialized yet");
	}
}
