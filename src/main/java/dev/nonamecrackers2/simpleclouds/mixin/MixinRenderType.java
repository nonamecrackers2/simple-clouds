package dev.nonamecrackers2.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.google.common.base.Predicate;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.nonamecrackers2.simpleclouds.client.world.FogRenderMode;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

@Mixin(RenderType.class)
public class MixinRenderType extends RenderStateShard
{
	private MixinRenderType()
	{
		super(null, null, null);
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setupRenderState()
	{
		super.setupRenderState();
		if (SimpleCloudsConfig.CLIENT.fogMode.get() == FogRenderMode.SCREEN_SPACE && FogRenderMode.shouldUseTranslucency((RenderType)(Object)this))
		{
			RenderSystem.enableBlend();
		    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		}
	}
	
	@Override
	public void clearRenderState()
	{
		super.clearRenderState();
		if (SimpleCloudsConfig.CLIENT.fogMode.get() == FogRenderMode.SCREEN_SPACE && FogRenderMode.shouldUseTranslucency((RenderType)(Object)this))
		{
			RenderSystem.disableBlend();
		    RenderSystem.defaultBlendFunc();
		}
	}

}
