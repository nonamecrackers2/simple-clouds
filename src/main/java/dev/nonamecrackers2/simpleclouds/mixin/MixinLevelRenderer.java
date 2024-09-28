package dev.nonamecrackers2.simpleclouds.mixin;

import javax.annotation.Nullable;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;

@Mixin(value = LevelRenderer.class, priority = 1001)
public class MixinLevelRenderer
{
	@Shadow
	private @Nullable ClientLevel level;
	
	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	public void simpleclouds$overrideCloudRendering_renderClouds(PoseStack stack, Matrix4f projMat, float partialTick, double camX, double camY, double camZ, CallbackInfo ci)
	{
		if (SimpleCloudsRenderer.canRenderInDimension(this.level))
			ci.cancel();
	}
	
	@Inject(method = "renderLevel", at = @At("TAIL")) // @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate(Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;)V", ordinal = 0)
	public void simpleclouds$injectCustomCloudRenderingPost_renderLevel(PoseStack stack, float partialTick, long l, boolean flag, Camera camera, GameRenderer renderer, LightTexture texture, Matrix4f projMat, CallbackInfo ci)
	{
		if (SimpleCloudsRenderer.canRenderInDimension(this.level))
			SimpleCloudsRenderer.getInstance().renderAfterLevel(stack, RenderSystem.getProjectionMatrix(), partialTick, camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
	}
	
	@Inject(method = "renderLevel", at = @At("HEAD")) // @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate(Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;)V", ordinal = 0)
	public void simpleclouds$injectCustomCloudRenderingPre_renderLevel(PoseStack stack, float partialTick, long l, boolean flag, Camera camera, GameRenderer renderer, LightTexture texture, Matrix4f projMat, CallbackInfo ci)
	{
		if (SimpleCloudsRenderer.canRenderInDimension(this.level))
			SimpleCloudsRenderer.getInstance().renderBeforeLevel(stack, RenderSystem.getProjectionMatrix(), partialTick, camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
	}
	
	@Inject(method = "renderSky", at = @At("RETURN")) // @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate(Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;)V", ordinal = 0)
	public void simpleclouds$injectCustomCloudRenderingPre_renderSky(PoseStack stack, Matrix4f projMat, float partialTick, Camera camera, boolean flag, Runnable fogSetup, CallbackInfo ci)
	{
		if (SimpleCloudsRenderer.canRenderInDimension(this.level))
			SimpleCloudsRenderer.getInstance().renderAfterSky(stack, RenderSystem.getProjectionMatrix(), partialTick, camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
	}
	
	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getCloudsType()Lnet/minecraft/client/CloudStatus;")) // @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate(Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;)V", ordinal = 0)
	public void simpleclouds$injectCustomCloudRenderingPre_beforeWeather(PoseStack stack, float partialTick, long l, boolean flag, Camera camera, GameRenderer renderer, LightTexture texture, Matrix4f projMat, CallbackInfo ci)
	{
		if (SimpleCloudsRenderer.canRenderInDimension(this.level))
			SimpleCloudsRenderer.getInstance().renderBeforeWeather(stack, projMat, partialTick, camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
	}
	
	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V"))
	public void simpleclouds$injectCustomWeatherRendering_renderLevel(PoseStack stack, float partialTick, long l, boolean flag, Camera camera, GameRenderer renderer, LightTexture texture, Matrix4f projMat, CallbackInfo ci)
	{
		if (SimpleCloudsRenderer.canRenderInDimension(this.level))
			SimpleCloudsRenderer.getInstance().getWorldEffectsManager().renderWeather(texture, partialTick, camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
	}
	
	@Inject(method = "tick", at = @At("HEAD"))
	public void simpleclouds$tickCloudRenderer_tick(CallbackInfo ci)
	{
		if (SimpleCloudsRenderer.canRenderInDimension(this.level))
			SimpleCloudsRenderer.getInstance().tick();
	}
	
	@Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
	public void simpleclouds$overrideRainRendering_renderSnowAndRain(LightTexture texture, float partialTick, double camX, double camY, double camZ, CallbackInfo ci)
	{
		if (SimpleCloudsRenderer.canRenderInDimension(this.level))
			ci.cancel();
	}
//	
//	@ModifyVariable(method = "renderChunkLayer", at = @At("STORE"))
//	public boolean simpleclouds$enableTranslucentSortForNonTranslucent_enableBackwardsRender_renderChunkLayer(boolean flag1, RenderType type)
//	{
//		if (!flag1)
//			return false;
//		else
//			return !FogRenderMode.shouldUseTranslucency(type);
//	}
//	
//	@Redirect(method = "renderChunkLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;translucent()Lnet/minecraft/client/renderer/RenderType;", opcode = Opcodes.GETSTATIC, ordinal = 0))
//	public RenderType simpleclouds$disableTranslucentSort_renderChunkLayer(RenderType inputType)
//	{
//		return null;//FogRenderMode.shouldUseTranslucency(inputType) ? inputType : RenderType.translucent();
//	}
	
//	
//	@Inject(method = "renderLevel", at = @At("TAIL"))
//	public void simpleclouds$renderPost_renderLevel(PoseStack stack, float partialTicks, long l, boolean flag, Camera camera, GameRenderer renderer, LightTexture texture, Matrix4f projMat, CallbackInfo ci)
//	{
//		SimpleCloudsRenderer.getInstance().renderInWorldPost(stack, partialTicks, projMat, camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
//	}
}
