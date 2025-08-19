package dev.nonamecrackers2.simpleclouds.mixin.vanillacompat;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.nonamecrackers2.simpleclouds.api.common.cloud.weather.WeatherType;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.WeatherCheck;
import net.minecraft.world.phys.Vec3;

@Mixin(WeatherCheck.class)
public class MixinWeatherCheck
{
	@Redirect(method = "test", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;isRaining()Z"))
	public boolean simpleclouds$localizedRainCheck_test(ServerLevel level, LootContext context)
	{
		return weatherCheck(level, context, WeatherType::includesRain);
	}
	
	@Redirect(method = "test", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;isThundering()Z"))
	public boolean simpleclouds$localizedThunderCheck_test(ServerLevel level, LootContext context)
	{
		return weatherCheck(level, context, WeatherType::includesThunder);
	}
	
	private static boolean weatherCheck(ServerLevel level, LootContext context, Predicate<WeatherType> weatherType)
	{
		Vec3 pos = null;
		if (context.hasParam(LootContextParams.DAMAGE_SOURCE))
			pos = context.getParam(LootContextParams.DAMAGE_SOURCE).getSourcePosition();
		else if (context.hasParam(LootContextParams.THIS_ENTITY))
			pos = context.getParam(LootContextParams.THIS_ENTITY).position();
		else if (context.hasParam(LootContextParams.ATTACKING_ENTITY))
			pos = context.getParam(LootContextParams.ATTACKING_ENTITY).position();
		else if (context.hasParam(LootContextParams.DIRECT_ATTACKING_ENTITY))
			pos = context.getParam(LootContextParams.DIRECT_ATTACKING_ENTITY).position();
		else if (context.hasParam(LootContextParams.BLOCK_ENTITY))
			pos = context.getParam(LootContextParams.BLOCK_ENTITY).getBlockPos().getCenter();
		else if (context.hasParam(LootContextParams.ORIGIN))
			pos = context.getParam(LootContextParams.ORIGIN);
		
		if (pos != null)
			return weatherType.test(CloudManager.get(level).getCloudTypeAtWorldPos((float)pos.x, (float)pos.z).getLeft().weatherType());
		else
			return false;
	}
}
