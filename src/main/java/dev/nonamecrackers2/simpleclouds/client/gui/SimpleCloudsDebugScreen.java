package dev.nonamecrackers2.simpleclouds.client.gui;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.nonamecrackers2.simpleclouds.client.cloud.ClientSideCloudTypeManager;
import dev.nonamecrackers2.simpleclouds.client.mesh.multiregion.MultiRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.renderer.CPUBasedCloudRegionTest;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.shader.SimpleCloudsShaders;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public class SimpleCloudsDebugScreen extends Screen
{
	private static final CPUBasedCloudRegionTest CLOUD_REGION_TEST = new CPUBasedCloudRegionTest();
	private boolean compareOverlayed;

	public SimpleCloudsDebugScreen()
	{
		super(Component.translatable("gui.simpleclouds.debug.title"));
	}

	@Override
	protected void init()
	{
		super.init();

		if (this.minecraft.level != null && SimpleCloudsRenderer.getInstance().getMeshGenerator() instanceof MultiRegionCloudMeshGenerator generator)
		{
			CloudType[] types = ClientSideCloudTypeManager.getInstance().getIndexed();
			CloudManager manager = CloudManager.get(this.minecraft.level);
			Vec3 cameraPos = this.minecraft.gameRenderer.getMainCamera().getPosition();
			CLOUD_REGION_TEST.init(types, generator.getCloudRegionTextureGenerator().getTextureSize(), 8.0F, manager.getScrollX(), manager.getScrollZ(), cameraPos.x, cameraPos.z, SimpleCloudsRenderer.CLOUD_SCALE);
		}
	}
	
	private void renderCPUTexture(GuiGraphics stack, float size, float padding, float x, float y, boolean withText, float alpha)
	{
		if (CLOUD_REGION_TEST.getTextureId() != -1)
		{
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
			float minX = x;
			float minY = y;
			float maxX = minX + size;
			float maxY = minY + size;
			if (withText)
				stack.drawString(this.font, "CPU", (int)minX, (int)minY - this.font.lineHeight - 5, 0xFFFFFFFF);
			RenderSystem.setShaderTexture(0, CLOUD_REGION_TEST.getTextureId());
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			Matrix4f matrix4f = stack.pose().last().pose();
			BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
			bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
			bufferbuilder.vertex(matrix4f, minX, minY, 0.0F).uv(0.0F, 0.0F).endVertex();
			bufferbuilder.vertex(matrix4f, minX, maxY, 0.0F).uv(0.0F, 1.0F).endVertex();
			bufferbuilder.vertex(matrix4f, maxX, maxY, 0.0F).uv(1.0F, 1.0F).endVertex();
			bufferbuilder.vertex(matrix4f, maxX, minY, 0.0F).uv(1.0F, 0.0F).endVertex();
			BufferUploader.drawWithShader(bufferbuilder.end());
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.disableBlend();
		}
	}
	
	private void renderGPUTexture(GuiGraphics stack, float size, float padding, float x, float y, boolean withText)
	{
		float minX = x;
		float minY = y;
		float maxX = minX + size;
		float maxY = minY + size;
		if (withText)
			stack.drawString(this.font, "GPU", (int)minX, (int)minY - this.font.lineHeight - 5, 0xFFFFFFFF);
		if (SimpleCloudsRenderer.getInstance().getMeshGenerator() instanceof MultiRegionCloudMeshGenerator meshGenerator)
		{
			int id = meshGenerator.getCloudRegionTextureId();
			if (id != -1)
			{
				RenderSystem.setShader(SimpleCloudsShaders::getCloudRegionTexShader);
				Matrix4f matrix4f = stack.pose().last().pose();
				BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
				bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
				bufferbuilder.vertex(matrix4f, minX, minY, 0.0F).uv(0.0F, 0.0F).endVertex();
				bufferbuilder.vertex(matrix4f, minX, maxY, 0.0F).uv(0.0F, 1.0F).endVertex();
				bufferbuilder.vertex(matrix4f, maxX, maxY, 0.0F).uv(1.0F, 1.0F).endVertex();
				bufferbuilder.vertex(matrix4f, maxX, minY, 0.0F).uv(1.0F, 0.0F).endVertex();
				ShaderInstance shader = RenderSystem.getShader();
				shader.safeGetUniform("LodLevel").set(meshGenerator.getLodConfig().getLods().length);
				shader.safeGetUniform("TotalCloudTypes").set(meshGenerator.getTotalCloudTypes());
				ProgramManager.glUseProgram(shader.getId());
				int loc = Uniform.glGetUniformLocation(shader.getId(), "TexRegionSampler");
				Uniform.uploadInteger(loc, 0);
				RenderSystem.activeTexture('\u84c0' + 0);
				GL11.glBindTexture(GL12.GL_TEXTURE_3D, id);
				BufferUploader.drawWithShader(bufferbuilder.end());
			}
		}
	}

	@Override
	public void render(GuiGraphics stack, int mouseX, int mouseY, float partialTick)
	{
		CLOUD_REGION_TEST.regenerate();
		this.renderBackground(stack);
		super.render(stack, mouseX, mouseY, partialTick);
		float size = 100.0F;
		float padding = 20.0F;
		float y = this.height / 2.0F - size / 2.0F;
		if (this.compareOverlayed)
		{
			this.renderGPUTexture(stack, size, padding, this.width / 2.0F - size / 2.0F, y, false);
			this.renderCPUTexture(stack, size, padding, this.width / 2.0F - size / 2.0F, y, false, 0.5F);
		}
		else
		{
			this.renderCPUTexture(stack, size, padding, this.width / 2.0F - size - padding / 2.0F, y, true, 1.0F);
			this.renderGPUTexture(stack, size, padding, this.width / 2.0F + padding / 2.0F, y, true);
		}
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;
		if (keyCode == GLFW.GLFW_KEY_S)
		{
			this.compareOverlayed = !this.compareOverlayed;
			return true;
		}
		return false;
	}
}
