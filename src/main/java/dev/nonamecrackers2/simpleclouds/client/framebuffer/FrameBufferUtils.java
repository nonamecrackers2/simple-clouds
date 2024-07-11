package dev.nonamecrackers2.simpleclouds.client.framebuffer;

import org.joml.Matrix4f;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;

public class FrameBufferUtils
{
	public static void blitTargetPreservingAlpha(RenderTarget target, int width, int height)
	{
		RenderSystem.assertOnRenderThread();
		GlStateManager._disableDepthTest();
		GlStateManager._depthMask(false);
		GlStateManager._viewport(0, 0, width, height);
		RenderSystem.disableBlend();

		Minecraft minecraft = Minecraft.getInstance();
		ShaderInstance shaderinstance = minecraft.gameRenderer.blitShader;
		shaderinstance.setSampler("DiffuseSampler", target.getColorTextureId());
		Matrix4f matrix4f = (new Matrix4f()).setOrtho(0.0F, (float)width, (float)height, 0.0F, 1000.0F, 3000.0F);
		RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
		if (shaderinstance.MODEL_VIEW_MATRIX != null)
			shaderinstance.MODEL_VIEW_MATRIX.set((new Matrix4f()).translation(0.0F, 0.0F, -2000.0F));

		if (shaderinstance.PROJECTION_MATRIX != null)
			shaderinstance.PROJECTION_MATRIX.set(matrix4f);

		shaderinstance.apply();
		float f = (float)width;
		float f1 = (float)height;
		float f2 = (float)target.viewWidth / (float)target.width;
		float f3 = (float)target.viewHeight / (float)target.height;
		Tesselator tesselator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferbuilder = tesselator.getBuilder();
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		bufferbuilder.vertex(0.0D, (double) f1, 0.0D).uv(0.0F, 0.0F).color(255, 255, 255, 255).endVertex();
		bufferbuilder.vertex((double) f, (double) f1, 0.0D).uv(f2, 0.0F).color(255, 255, 255, 255).endVertex();
		bufferbuilder.vertex((double) f, 0.0D, 0.0D).uv(f2, f3).color(255, 255, 255, 255).endVertex();
		bufferbuilder.vertex(0.0D, 0.0D, 0.0D).uv(0.0F, f3).color(255, 255, 255, 255).endVertex();
		BufferUploader.draw(bufferbuilder.end());
		shaderinstance.clear();
		GlStateManager._depthMask(true);
	}
}
