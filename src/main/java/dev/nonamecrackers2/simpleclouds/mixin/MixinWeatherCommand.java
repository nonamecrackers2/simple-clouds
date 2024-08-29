package dev.nonamecrackers2.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.brigadier.CommandDispatcher;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.WeatherCommand;

@Mixin(WeatherCommand.class)
public class MixinWeatherCommand
{
	@Inject(method = "register", at = @At("HEAD"), cancellable = true)
	private static void simpleclouds$preventWeatherCommandsFromRegistering_register(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo ci)
	{
		if (!SimpleCloudsConfig.SERVER_SPEC.isLoaded() || SimpleCloudsConfig.SERVER.cloudMode.get() != CloudMode.SINGLE)
			ci.cancel();
	}
}
