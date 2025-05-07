package dev.nonamecrackers2.simpleclouds.client.renderer;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.nonamecrackers2.simpleclouds.client.framebuffer.ShadowMapBuffer;
import dev.nonamecrackers2.simpleclouds.client.mesh.multiregion.MultiRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.shader.SimpleCloudsShaders;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class SimpleCloudsDebugOverlayRenderer
{
	public static void render(ForgeGui gui, GuiGraphics stack, float partialTicks, float width, float height)
	{
		if (SimpleCloudsRenderer.canRenderInDimension(gui.getMinecraft().level) && gui.getMinecraft().options.renderDebug)
		{
			float displaySize = 50.0F;
			if (SimpleCloudsRenderer.getInstance().getMeshGenerator() instanceof MultiRegionCloudMeshGenerator meshGenerator)
			{
				int id = meshGenerator.getCloudRegionTextureId();
				if (id != -1)
				{
					RenderSystem.setShader(SimpleCloudsShaders::getCloudRegionTexShader);
					Matrix4f matrix4f = stack.pose().last().pose();
					BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
					bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
					bufferbuilder.vertex(matrix4f, displaySize, height - displaySize, -100.0F).uv(0.0F, 0.0F).endVertex();
					bufferbuilder.vertex(matrix4f, displaySize, height, -100.0F).uv(0.0F, 1.0F).endVertex();
					bufferbuilder.vertex(matrix4f, displaySize * 2.0F, height, -100.0F).uv(1.0F, 1.0F).endVertex();
					bufferbuilder.vertex(matrix4f, displaySize * 2.0F, height - displaySize, -100.0F).uv(1.0F, 0.0F).endVertex();
					ShaderInstance shader = RenderSystem.getShader();
					int lod = meshGenerator.getLodConfig().getLods().length;
					shader.safeGetUniform("LodLevel").set(lod);
					shader.safeGetUniform("TotalCloudTypes").set(meshGenerator.getTotalCloudTypes());
					ProgramManager.glUseProgram(shader.getId());
					int loc = Uniform.glGetUniformLocation(shader.getId(), "TexRegionSampler");
					Uniform.uploadInteger(loc, 0);
					RenderSystem.activeTexture('\u84c0' + 0);
					GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, id);
					BufferUploader.drawWithShader(bufferbuilder.end());
				}
			}
			RenderSystem.setShaderTexture(0, SimpleCloudsRenderer.getInstance().getStormFogShadowMap().getColorTexId());
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			Matrix4f matrix4f = stack.pose().last().pose();
			BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
			bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
			bufferbuilder.vertex(matrix4f, 0.0F, height - displaySize, -100.0F).uv(0.0F, 0.0F).endVertex();
			bufferbuilder.vertex(matrix4f, 0.0F, height, -100.0F).uv(0.0F, 1.0F).endVertex();
			bufferbuilder.vertex(matrix4f, displaySize, height, -100.0F).uv(1.0F, 1.0F).endVertex();
			bufferbuilder.vertex(matrix4f, displaySize, height - displaySize, -100.0F).uv(1.0F, 0.0F).endVertex();
			BufferUploader.drawWithShader(bufferbuilder.end());
		}
	}
}
