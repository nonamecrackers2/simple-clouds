package dev.nonamecrackers2.simpleclouds.client.gui;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.nonamecrackers2.simpleclouds.client.renderer.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.common.noise.StaticNoiseSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import nonamecrackers2.crackerslib.client.gui.Screen3D;

public class CloudPreviewerScreen extends Screen3D
{
	public CloudPreviewerScreen()
	{
		super(Component.translatable("gui.simpleclouds.cloud_previewer.title"), 0.5F, 1000.0F);
	}
	
	@Override
	public void render(GuiGraphics stack, int pMouseX, int pMouseY, float pPartialTick)
	{
		this.renderBackground(stack);
		super.render(stack, pMouseX, pMouseY, pPartialTick);
	}
	
	@Override
	protected void render3D(PoseStack stack, MultiBufferSource buffers, int mouseX, int mouseY, float partialTick)
	{
		BufferUploader.reset();
		
		SimpleCloudsRenderer renderer = SimpleCloudsRenderer.getInstance();
		renderer.generateMesh(StaticNoiseSettings.DEFAULT, 0.0D, 0.0D, 0.0D);
		renderer.render(stack, RenderSystem.getProjectionMatrix(), partialTick, 1.0F, 1.0F, 1.0F);
		
		float radius = CloudMeshGenerator.getCloudRenderDistance();
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder builder = tesselator.getBuilder();
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f pose = stack.last().pose();
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		float r = 0.3F, g = 0.3F, b = 0.3F, a = 0.5F;
		builder.vertex(pose, -radius, 0.0F, -radius).color(r, g, b, a).endVertex();
		builder.vertex(pose, radius, 0.0F, -radius).color(r, g, b, a).endVertex();
		builder.vertex(pose, radius, 0.0F, radius).color(r, g, b, a).endVertex();
		builder.vertex(pose, -radius, 0.0F, radius).color(r, g, b, a).endVertex();
		tesselator.end();
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}
}
