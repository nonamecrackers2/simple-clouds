package dev.nonamecrackers2.simpleclouds.client.renderer.pipeline;

import javax.annotation.Nullable;

import org.joml.Matrix4f;

import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import net.minecraft.client.Minecraft;

public interface CloudsRenderPipeline
{
	public static final CloudsRenderPipeline DEFAULT = new DefaultPipeline();
	public static final CloudsRenderPipeline SHADER_SUPPORT = new ShaderSupportPipeline();
	
	void prepare(Minecraft mc, SimpleCloudsRenderer renderer, Matrix4f camMat, Matrix4f projMat, float partialTick, double camX, double camY, double camZ);
	
	void afterSky(Minecraft mc, SimpleCloudsRenderer renderer, Matrix4f camMat, @Nullable Matrix4f shadowMapMat, Matrix4f projMat, float partialTick, double camX, double camY, double camZ);
	
	void beforeWeather(Minecraft mc, SimpleCloudsRenderer renderer, Matrix4f camMat, @Nullable Matrix4f shadowMapMat, Matrix4f projMat, float partialTick, double camX, double camY, double camZ);
	
	void afterLevel(Minecraft mc, SimpleCloudsRenderer renderer, Matrix4f camMat, @Nullable Matrix4f shadowMapMat, Matrix4f projMat, float partialTick, double camX, double camY, double camZ);
}
