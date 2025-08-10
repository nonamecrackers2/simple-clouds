package dev.nonamecrackers2.simpleclouds.client.renderer.pipeline;

import org.joml.Matrix4f;

import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;

public interface CloudsRenderPipeline
{
	public static final CloudsRenderPipeline DEFAULT = new DefaultPipeline();
	public static final CloudsRenderPipeline SHADER_SUPPORT = new ShaderSupportPipeline();
	
	void prepare(Minecraft mc, SimpleCloudsRenderer renderer, Matrix4f camMat, Matrix4f projMat, float partialTick, double camX, double camY, double camZ, Frustum frustum);
	
	void afterSky(Minecraft mc, SimpleCloudsRenderer renderer, Matrix4f camMat, Matrix4f projMat, float partialTick, double camX, double camY, double camZ, Frustum frustum);
	
	void beforeWeather(Minecraft mc, SimpleCloudsRenderer renderer, Matrix4f camMat, Matrix4f projMat, float partialTick, double camX, double camY, double camZ, Frustum frustum);
	
	void afterLevel(Minecraft mc, SimpleCloudsRenderer renderer, Matrix4f camMat, Matrix4f projMat, float partialTick, double camX, double camY, double camZ, Frustum frustum);
	
	default void beforeDistantHorizonsApplyShader(Minecraft mc, SimpleCloudsRenderer renderer, Matrix4f dhModelViewMat, Matrix4f dhProjMat, float partialTick, double camX, double camY, double camZ, Frustum frustum, int dhFrameBufferId) {}
	
	default void afterDistantHorizonsRender(Minecraft mc, SimpleCloudsRenderer renderer, Matrix4f dhModelViewMat, Matrix4f dhProjMat, float partialTick, double camX, double camY, double camZ, Frustum frustum, int dhFrameBufferId) {}
}
