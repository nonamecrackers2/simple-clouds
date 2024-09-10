package dev.nonamecrackers2.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeDataManager;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.WeatherCommand;
import net.minecraft.world.level.Level;

@Mixin(WeatherCommand.class)
public class MixinWeatherCommand
{
	private static final SimpleCommandExceptionType WEATHER_CANNOT_BE_MODIFIED = new SimpleCommandExceptionType(Component.translatable("command.simpleclouds.weather.override"));
	
//	@Inject(method = "register", at = @At("HEAD"), cancellable = true)
//	private static void simpleclouds$preventWeatherCommandsFromRegistering_register(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo ci)
//	{
//		if (!SimpleCloudsConfig.SERVER_SPEC.isLoaded() || SimpleCloudsConfig.SERVER.cloudMode.get() != CloudMode.SINGLE)
//			ci.cancel();
//	}
	
	private static void checkAndOrThrow(Level level) throws CommandSyntaxException
	{
		if (!CloudManager.useVanillaWeather(level, CloudTypeDataManager.getServerInstance()))
			throw WEATHER_CANNOT_BE_MODIFIED.create();
	}
	
	@Inject(method = "setClear", at = @At("HEAD"), cancellable = true)
	private static void simpleclouds$preventWeatherModification_setClear(CommandSourceStack stack, int duration, CallbackInfoReturnable<Integer> ci) throws CommandSyntaxException
	{
		checkAndOrThrow(stack.getLevel());
	}
	
	@Inject(method = "setRain", at = @At("HEAD"), cancellable = true)
	private static void simpleclouds$preventWeatherModification_setRain(CommandSourceStack stack, int duration, CallbackInfoReturnable<Integer> ci) throws CommandSyntaxException
	{
		checkAndOrThrow(stack.getLevel());
	}
	
	@Inject(method = "setThunder", at = @At("HEAD"), cancellable = true)
	private static void simpleclouds$preventWeatherModification_setThunder(CommandSourceStack stack, int duration, CallbackInfoReturnable<Integer> ci) throws CommandSyntaxException
	{
		checkAndOrThrow(stack.getLevel());
	}
}
