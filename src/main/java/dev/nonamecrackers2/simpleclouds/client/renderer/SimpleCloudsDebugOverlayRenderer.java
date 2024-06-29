package dev.nonamecrackers2.simpleclouds.client.renderer;

import org.joml.Matrix4f;
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

import dev.nonamecrackers2.simpleclouds.client.mesh.MultiRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.shader.SimpleCloudsShaders;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class SimpleCloudsDebugOverlayRenderer
{
	public static void render(ForgeGui gui, GuiGraphics stack, float partialTicks, float width, float height)
	{
		if (gui.getMinecraft().options.renderDebug)
		{
			if (SimpleCloudsRenderer.getInstance().getMeshGenerator() instanceof MultiRegionCloudMeshGenerator meshGenerator)
			{
				int id = meshGenerator.getCloudRegionTextureId();
				if (id != -1)
				{
					RenderSystem.setShader(SimpleCloudsShaders::getCloudRegionTexShader);
					Matrix4f matrix4f = stack.pose().last().pose();
					BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
					bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
					bufferbuilder.vertex(matrix4f, width - 200.0F, height - 100.0F, -100.0F).uv(0.0F, 0.0F).endVertex();
					bufferbuilder.vertex(matrix4f, width - 200.0F, height, -100.0F).uv(0.0F, 1.0F).endVertex();
					bufferbuilder.vertex(matrix4f, width - 100.0F, height, -100.0F).uv(1.0F, 1.0F).endVertex();
					bufferbuilder.vertex(matrix4f, width - 100.0F, height - 100.0F, -100.0F).uv(1.0F, 0.0F).endVertex();
					ShaderInstance shader = RenderSystem.getShader();
					shader.safeGetUniform("LodLevel").set(3);
					shader.safeGetUniform("TotalCloudTypes").set(meshGenerator.getTotalCloudTypes());
					ProgramManager.glUseProgram(shader.getId());
					int loc = Uniform.glGetUniformLocation(shader.getId(), "TexRegionSampler");
					Uniform.uploadInteger(loc, 0);
					RenderSystem.activeTexture('\u84c0' + 0);
					GL11.glBindTexture(GL12.GL_TEXTURE_3D, id);
					BufferUploader.drawWithShader(bufferbuilder.end());
				}
			}
			RenderSystem.setShaderTexture(0, SimpleCloudsRenderer.getInstance().getShadowMapTextureId());
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			Matrix4f matrix4f = stack.pose().last().pose();
			BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
			bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
			bufferbuilder.vertex(matrix4f, width - 100.0F, height - 100.0F, -100.0F).uv(0.0F, 0.0F).endVertex();
			bufferbuilder.vertex(matrix4f, width - 100.0F, height, -100.0F).uv(0.0F, 1.0F).endVertex();
			bufferbuilder.vertex(matrix4f, width, height, -100.0F).uv(1.0F, 1.0F).endVertex();
			bufferbuilder.vertex(matrix4f, width, height - 100.0F, -100.0F).uv(1.0F, 0.0F).endVertex();
			BufferUploader.drawWithShader(bufferbuilder.end());
		}
	}
}
