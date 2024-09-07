package dev.nonamecrackers2.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.server.level.ServerLevel;

@Mixin(ServerLevel.class)
public interface MixinServerLevelAccessor
{
	@Invoker(value = "resetWeatherCycle")
	void simpleclouds$invokeResetWeatherCycle();
}
