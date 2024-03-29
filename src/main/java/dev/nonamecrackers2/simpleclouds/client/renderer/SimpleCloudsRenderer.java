package dev.nonamecrackers2.simpleclouds.client.renderer;

import java.nio.ByteBuffer;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.MemoryTracker;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.nonamecrackers2.simpleclouds.client.shader.SimpleCloudsShaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;

public class SimpleCloudsRenderer
{
	public static final SimpleCloudsRenderer INSTANCE = new SimpleCloudsRenderer();
	private static int currentIndex;
	private int vertexBufferId;
	private int indexBufferId;
	private int arrayObjectId;
//	private RenderSystem.AutoStorageIndexBuffer autoIndexBuffer;
	private final ByteBuffer vertexBuffer;
	
	public SimpleCloudsRenderer()
	{
		this.vertexBuffer = MemoryTracker.create(457);
		ByteBuffer indexBuffer = MemoryTracker.create(97);
		renderBox(1.0F, this.vertexBuffer, indexBuffer);

		this.vertexBufferId = GlStateManager._glGenBuffers();
		this.indexBufferId = GlStateManager._glGenBuffers();
		this.arrayObjectId = GlStateManager._glGenVertexArrays();
		
		BufferUploader.invalidate();
		GlStateManager._glBindVertexArray(this.arrayObjectId);
		
		GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vertexBufferId);
		DefaultVertexFormat.POSITION_COLOR_NORMAL.setupBufferState();
		RenderSystem.glBufferData(GL15.GL_ARRAY_BUFFER, this.vertexBuffer, GL15.GL_STATIC_DRAW);
		
		
//		this.autoIndexBuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
//		this.autoIndexBuffer.bind(currentIndex);
		GlStateManager._glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.indexBufferId);
		RenderSystem.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);

		BufferUploader.invalidate();
		GlStateManager._glBindVertexArray(0);
	}
	
	public void render(PoseStack stack, Matrix4f projMat, float partialTicks, double camX, double camY, double camZ)
	{
//		Tesselator tesselator = Tesselator.getInstance();
//		BufferBuilder builder = tesselator.getBuilder();
		RenderSystem.setShader(SimpleCloudsShaders::getCloudsShader);
		RenderSystem.disableBlend();
		RenderSystem.disableCull();
//		
		BufferUploader.invalidate();
		GlStateManager._glBindVertexArray(this.arrayObjectId);
		
		stack.pushPose();
		stack.translate(-camX, -camY, -camZ);
		prepareShader(RenderSystem.getShader(), stack.last().pose(), projMat);
		RenderSystem.getShader().apply();
		RenderSystem.drawElements(4, currentIndex, GL11.GL_UNSIGNED_INT);
		RenderSystem.getShader().clear();
		stack.popPose();
		
		BufferUploader.invalidate();
		GlStateManager._glBindVertexArray(0);
		
//		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_NORMAL);
//		builder.putBulkData(this.vertexBuffer);
//		VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
//		BufferBuilder.RenderedBuffer rendered = builder.end();
//		buffer.bind();
//		buffer.upload(rendered);
//		stack.pushPose();
//		stack.translate(-camX, -camY, -camZ);
//		buffer.drawWithShader(stack.last().pose(), projMat, RenderSystem.getShader());
//		stack.popPose();
//		VertexBuffer.unbind();
//		
//		buffer.close();
	}
	
	private static void prepareShader(ShaderInstance shader, Matrix4f modelView, Matrix4f projMat)
	{
		for (int i = 0; i < 12; ++i)
		{
			int j = RenderSystem.getShaderTexture(i);
			shader.setSampler("Sampler" + i, j);
		}

		if (shader.MODEL_VIEW_MATRIX != null)
			shader.MODEL_VIEW_MATRIX.set(modelView);

		if (shader.PROJECTION_MATRIX != null)
			shader.PROJECTION_MATRIX.set(projMat);

		if (shader.INVERSE_VIEW_ROTATION_MATRIX != null)
			shader.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());

		if (shader.COLOR_MODULATOR != null)
			shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());

		if (shader.GLINT_ALPHA != null)
			shader.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());

		if (shader.FOG_START != null)
			shader.FOG_START.set(RenderSystem.getShaderFogStart());

		if (shader.FOG_END != null)
			shader.FOG_END.set(RenderSystem.getShaderFogEnd());

		if (shader.FOG_COLOR != null)
			shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());

		if (shader.FOG_SHAPE != null)
			shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());

		if (shader.TEXTURE_MATRIX != null)
			shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());

		if (shader.GAME_TIME != null)
			shader.GAME_TIME.set(RenderSystem.getShaderGameTime());

		if (shader.SCREEN_SIZE != null)
		{
			Window window = Minecraft.getInstance().getWindow();
			shader.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
		}
	}
	
	public static void renderBox(float radius, ByteBuffer vertexBuffer, ByteBuffer indexBuffer)
	{
		//-Z
		vertex(vertexBuffer, indexBuffer, -radius, -radius, -radius);
		vertex(vertexBuffer, indexBuffer, -radius, radius, -radius);
		vertex(vertexBuffer, indexBuffer, radius, radius, -radius);
		vertex(vertexBuffer, indexBuffer, radius, -radius, -radius);
		
		//+Z
		vertex(vertexBuffer, indexBuffer, -radius, radius, radius);
		vertex(vertexBuffer, indexBuffer, -radius, -radius, radius);
		vertex(vertexBuffer, indexBuffer, radius, -radius, radius);
		vertex(vertexBuffer, indexBuffer, radius, radius, radius);
		
		//-Y
		vertex(vertexBuffer, indexBuffer, -radius, -radius, radius);
		vertex(vertexBuffer, indexBuffer, -radius, -radius, -radius);
		vertex(vertexBuffer, indexBuffer, radius, -radius, -radius);
		vertex(vertexBuffer, indexBuffer, radius, -radius, radius);
		
		//+Y
		vertex(vertexBuffer, indexBuffer, -radius, radius, -radius);
		vertex(vertexBuffer, indexBuffer, -radius, radius, radius);
		vertex(vertexBuffer, indexBuffer, radius, radius, radius);
		vertex(vertexBuffer, indexBuffer, radius, radius, -radius);
		
		//-X
		vertex(vertexBuffer, indexBuffer, -radius, -radius, -radius);
		vertex(vertexBuffer, indexBuffer, -radius, -radius, radius);
		vertex(vertexBuffer, indexBuffer, -radius, radius, radius);
		vertex(vertexBuffer, indexBuffer, -radius, radius, -radius);
		
		//+X
		vertex(vertexBuffer, indexBuffer, radius, -radius, radius);
		vertex(vertexBuffer, indexBuffer, radius, -radius, -radius);
		vertex(vertexBuffer, indexBuffer, radius, radius, -radius);
		vertex(vertexBuffer, indexBuffer, radius, radius, radius);
	}
	
	private static void vertex(ByteBuffer vertexBuffer, ByteBuffer indexBuffer, float x, float y, float z)
	{
		int nextIndex = currentIndex * 19;
		vertexBuffer.putFloat(0 + nextIndex, x);
		vertexBuffer.putFloat(4 + nextIndex, y);
		vertexBuffer.putFloat(8 + nextIndex, z);
		vertexBuffer.put(12 + nextIndex, (byte)255);
		vertexBuffer.put(13 + nextIndex, (byte)255);
		vertexBuffer.put(14 + nextIndex, (byte)255);
		vertexBuffer.put(15 + nextIndex, (byte)255);
		vertexBuffer.put(16 + nextIndex, normalIntValue(0.0F));
		vertexBuffer.put(17 + nextIndex, normalIntValue(1.0F));
		vertexBuffer.put(18 + nextIndex, normalIntValue(0.0F));
		indexBuffer.putInt(currentIndex++);
	}
	
	private static byte normalIntValue(float normal)
	{
		return (byte)((int)(Mth.clamp(normal, -1.0F, 1.0F) * 127.0F) & 255);
	}
}
