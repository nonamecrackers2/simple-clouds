package dev.nonamecrackers2.simpleclouds.client.renderer;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.nonamecrackers2.simpleclouds.client.framebuffer.CloudRenderTarget;
import dev.nonamecrackers2.simpleclouds.client.framebuffer.WeightedBlendingTarget;
import dev.nonamecrackers2.simpleclouds.client.mesh.generator.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.mixin.MixinPostChain;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CloudImageRenderer implements AutoCloseable
{
	private static final int DEFAULT_WIDTH = 2048;
	private static final int DEFAULT_HEIGHT = 2048;
	private static final float ZOOM_CONSTANT = 1.0F;
	private static final Logger LOGGER = LogManager.getLogger();
	private final Minecraft mc;
	private final File path;
	private final CloudMeshGenerator generator;
	private @Nullable PostChain finalComposite;
	private @Nullable RenderTarget finalTarget;
	private @Nullable RenderTarget cloudsTarget;
	private @Nullable WeightedBlendingTarget transparencyTarget;
	private int oldWindowHeight = -1;
	private int oldWindowWidth = -1;
	private float rotX;
	private float rotY;
	private float zoom;
	private float r;
	private float g;
	private float b;
	
	public CloudImageRenderer(Minecraft mc, File path, float rotX, float rotY, float zoom, CloudMeshGenerator generator)
	{
		this.mc = mc;
		this.path = path;
		this.rotX = rotX;
		this.rotY = rotY;
		this.zoom = zoom;
		this.generator = generator;
	}
	
	public static CloudImageRenderer basicIsometric(File path, CloudMeshGenerator generator)
	{
		float fadeDist = generator.getFadeStart() + 32.0F;
		float zoom = (float)DEFAULT_WIDTH / (fadeDist * 2.0F);
		return new CloudImageRenderer(Minecraft.getInstance(), path, 180.0F + 45.0F, 45.0F, zoom, generator);
	}
	
	public @Nullable RenderTarget getFrameBuffer()
	{
		return this.finalTarget;
	}
	
	public void setRotX(float rot)
	{
		this.rotX = rot;
	}
	
	public void setRotY(float rotY) 
	{	
		this.rotY = rotY;
	}
	
	public void setZoom(float zoom)
	{
		this.zoom = zoom;
	}
	
	public void setBgCol(float r, float g, float b)
	{
		this.r = r;
		this.g = g;
		this.b = b;
	}
	
	public void initialize()
	{
		RenderSystem.assertOnRenderThread();
		
		Window window = this.mc.getWindow();
		this.oldWindowWidth = window.getWidth();
		this.oldWindowHeight = window.getHeight();
		window.setWidth(DEFAULT_WIDTH);
		window.setHeight(DEFAULT_HEIGHT);
		window.setGuiScale(window.getGuiScale());
		
		this.cloudsTarget = new CloudRenderTarget(DEFAULT_WIDTH, DEFAULT_HEIGHT, Minecraft.ON_OSX, false);
		this.cloudsTarget.setClearColor(this.r, this.g, this.b, 1.0F);
		this.cloudsTarget.clear(Minecraft.ON_OSX);
		
		if (this.generator.transparencyEnabled())
			this.transparencyTarget = new WeightedBlendingTarget(DEFAULT_WIDTH, DEFAULT_HEIGHT, Minecraft.ON_OSX, false);;
		
		this.finalTarget = new TextureTarget(DEFAULT_WIDTH, DEFAULT_HEIGHT, true, Minecraft.ON_OSX);
		this.finalTarget.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
		this.finalTarget.clear(Minecraft.ON_OSX);
		
		ResourceLocation finalComLoc = this.generator.transparencyEnabled() ? SimpleCloudsRenderer.FINAL_COMPOSITE_LOC : SimpleCloudsRenderer.FINAL_COMPOSITE_NO_TRANSPARENCY_LOC;
		try 
		{
			this.finalComposite = new PostChain(this.mc.getTextureManager(), this.mc.getResourceManager(), this.finalTarget, finalComLoc);
			this.finalComposite.resize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
			for (PostPass pass : ((MixinPostChain)this.finalComposite).simpleclouds$getPostPasses())
			{
				EffectInstance effect = pass.getEffect();
				if (this.generator.transparencyEnabled())
				{
					effect.setSampler("AccumTexture", () -> this.transparencyTarget.getColorTextureId());
					effect.setSampler("RevealageTexture", () -> this.transparencyTarget.getRevealageTextureId());
				}
				effect.setSampler("CloudsTexture", () -> this.cloudsTarget.getColorTextureId());
				effect.setSampler("CloudsDepthTexture", () -> this.cloudsTarget.getDepthTextureId());
			}
		}
		catch (JsonSyntaxException | IOException e) 
		{
			LOGGER.error("Failed to create final composite post process chain", e);
			if (this.finalComposite != null)
			{
				this.finalComposite.close();
				this.finalComposite = null;
			}
		}
	}
	
	private void assertValid()
	{
		Objects.requireNonNull(this.finalComposite, "Not properly initialized");
		Objects.requireNonNull(this.finalTarget, "Not properly initialized");
		Objects.requireNonNull(this.cloudsTarget, "Not properly initialized");
		Objects.requireNonNull(this.transparencyTarget, "Not properly initialized");
		if (this.oldWindowWidth < 0 || this.oldWindowHeight < 0)
			throw new IllegalStateException("Not properly initialized");
	}
	
	//TODO: test renders
	public void render()
	{
		RenderSystem.assertOnRenderThread();
		this.assertValid();
		
		float farPlane = this.generator.getFadeEnd() * 2.0F;
		
		Matrix4f projectionMat = new Matrix4f().setOrtho(0.0F, (float)DEFAULT_WIDTH, (float)DEFAULT_HEIGHT, 0.0F, 0.0F, farPlane);
		Quaternionf rot = new Quaternionf().rotateX(this.rotX * ((float)Math.PI / 180.0F)).rotateY((float)Math.PI + this.rotY * ((float)Math.PI / 180.0F));
		PoseStack stack = new PoseStack();
		stack.setIdentity();
		stack.translate((double)(DEFAULT_WIDTH / 2), (double)(DEFAULT_HEIGHT / 2), 0.0F);
		stack.mulPose(new Matrix4f().scaling(this.zoom * ZOOM_CONSTANT, this.zoom * ZOOM_CONSTANT, -1.0F));
		stack.translate(0.0D, 0.0D, farPlane / 2.0F);
		stack.mulPose(rot);
		
		this.cloudsTarget.clear(Minecraft.ON_OSX);
		this.cloudsTarget.bindWrite(true);
		
		SimpleCloudsRenderer.renderCloudsOpaque(this.generator, stack, projectionMat, Float.MAX_VALUE, Float.MAX_VALUE, 1.0F, 1.0F, 1.0F, 1.0F, null, false);
		
		this.transparencyTarget.clear(Minecraft.ON_OSX);
		
		if (this.generator.transparencyEnabled())
		{
			this.transparencyTarget.bindWrite(true);
			this.transparencyTarget.copyDepthFrom(this.cloudsTarget);
			
			if (GlStateManager._getError() != GL11.GL_NO_ERROR)
				throw new RuntimeException("Failed to copy depth buffers");
			
			this.transparencyTarget.bindWrite(false);
			
			SimpleCloudsRenderer.renderCloudsTransparency(this.generator, stack, projectionMat, Float.MAX_VALUE, Float.MAX_VALUE, 1.0F, 1.0F, 1.0F, 1.0F, null, false);
		}
		
		this.finalTarget.clear(Minecraft.ON_OSX);

		RenderSystem.disableDepthTest();
		RenderSystem.resetTextureMatrix();
		RenderSystem.depthMask(false);
		
		Matrix4f invertedProjMat = new Matrix4f(projectionMat).invert();
		Matrix4f invertedModelViewMat = new Matrix4f(stack.last().pose()).invert();
		for (PostPass pass : ((MixinPostChain)this.finalComposite).simpleclouds$getPostPasses())
		{
			EffectInstance effect = pass.getEffect();
			effect.safeGetUniform("InverseWorldProjMat").set(invertedProjMat);
			effect.safeGetUniform("InverseModelViewMat").set(invertedModelViewMat);
			effect.safeGetUniform("FogStart").set(Float.MAX_VALUE);
			effect.safeGetUniform("FogEnd").set(Float.MAX_VALUE);
		}
		
		this.finalComposite.process(1.0F);
		
		RenderSystem.depthMask(true);
		
		this.mc.getMainRenderTarget().bindWrite(true);
	}
	
	public void exportToRenderedImage(Consumer<Component> messageAcceptor)
	{
		RenderSystem.assertOnRenderThread();
		this.assertValid();
		
		NativeImage image = new NativeImage(DEFAULT_WIDTH, DEFAULT_HEIGHT, false);
		RenderSystem.bindTexture(this.finalTarget.getColorTextureId());
		image.downloadTexture(0, true);
		image.flipY();
		
		File loc = getFile(this.path);
		
		Util.ioPool().execute(() -> 
		{
			try 
			{
				image.writeToFile(loc);
				Component component = Component.literal(loc.getName()).withStyle(ChatFormatting.UNDERLINE).withStyle(s -> {
					return s.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, loc.getAbsolutePath()));
				});
				messageAcceptor.accept(Component.translatable("screenshot.success", component));
			}
			catch (Exception e)
			{
				LOGGER.warn("Failed to save render", e);
				messageAcceptor.accept(Component.translatable("screenshot.failure", e.getMessage()));
			}
			finally
			{
				image.close();
			}
		});
	}
	
	public void finalize()
	{
		RenderSystem.assertOnRenderThread();
		this.assertValid();
		
		Window window = this.mc.getWindow();
		window.setWidth(this.oldWindowWidth);
		window.setHeight(this.oldWindowHeight);
		window.setGuiScale(window.getGuiScale());
	}
	
	@Override
	public void close()
	{
		if (this.finalTarget != null)
		{
			this.finalTarget.destroyBuffers();
			this.finalTarget = null;
		}
		
		if (this.cloudsTarget != null)
		{
			this.cloudsTarget.destroyBuffers();
			this.cloudsTarget = null;
		}
		
		if (this.transparencyTarget != null)
		{
			this.transparencyTarget.destroyBuffers();
			this.transparencyTarget = null;
		}
		
		if (this.finalComposite != null)
		{
			this.finalComposite.close();
			this.finalComposite = null;
		}
	}
	
	// From Screenshot
	private static File getFile(File file)
	{
		String s = Util.getFilenameFormattedDateTime();
		int i = 1;

		while (true)
		{
			File renderFile = new File(file, s + (i == 1 ? "" : "_" + i) + ".png");
			if (!renderFile.exists())
				return renderFile;

			i++;
		}
	}
}
