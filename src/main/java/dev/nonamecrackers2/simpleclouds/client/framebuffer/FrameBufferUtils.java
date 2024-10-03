package dev.nonamecrackers2.simpleclouds.client.framebuffer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;

public class FrameBufferUtils
{
	public static void blitTargetPreservingAlpha(RenderTarget target, int width, int height)
	{
		RenderSystem.assertOnRenderThread();
		GlStateManager._colorMask(true, true, true, true);
		GlStateManager._disableDepthTest();
		GlStateManager._depthMask(false);
		GlStateManager._viewport(0, 0, width, height);
		RenderSystem.disableBlend();

		Minecraft minecraft = Minecraft.getInstance();
		ShaderInstance shaderinstance = minecraft.gameRenderer.blitShader;
		shaderinstance.setSampler("DiffuseSampler", target.getColorTextureId());
		shaderinstance.apply();
		BufferBuilder bufferbuilder = RenderSystem.renderThreadTesselator().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLIT_SCREEN);
        bufferbuilder.addVertex(0.0F, 0.0F, 0.0F);
        bufferbuilder.addVertex(1.0F, 0.0F, 0.0F);
        bufferbuilder.addVertex(1.0F, 1.0F, 0.0F);
        bufferbuilder.addVertex(0.0F, 1.0F, 0.0F);
        BufferUploader.draw(bufferbuilder.buildOrThrow());
        shaderinstance.clear();
        GlStateManager._depthMask(true);
		GlStateManager._depthMask(true);
	}
}
