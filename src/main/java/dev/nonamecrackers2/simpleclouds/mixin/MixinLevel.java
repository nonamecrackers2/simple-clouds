package dev.nonamecrackers2.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Mixin(Level.class)
public class MixinLevel
{
	@Inject(method = "isRainingAt", at = @At("HEAD"), cancellable = true)
	public void simpleclouds$localizedWeather_isRainingAt(BlockPos pos, CallbackInfoReturnable<Boolean> ci)
	{
		ci.setReturnValue(CloudManager.get((Level)(Object)this).isRainingAt(pos));
	}
}
