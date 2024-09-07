package dev.nonamecrackers2.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

@Mixin(BiomeColors.class)
@Deprecated
public class MixinBiomeColors
{
	@Inject(method = "getAverageWaterColor", at = @At("RETURN"))
	private static void simpleclouds$modifyWaterColor_getAverageWaterColor(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> ci)
	{
		//ci.setReturnValue(SimpleCloudsRenderer.getOptionalInstance().map(renderer -> renderer.getWorldEffectsManager().modifyWaterColor(ci.getReturnValue())).orElse(ci.getReturnValue()));
	}
}
