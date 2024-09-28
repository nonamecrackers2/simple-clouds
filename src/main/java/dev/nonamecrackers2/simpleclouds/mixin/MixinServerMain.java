package dev.nonamecrackers2.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.Main;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;

@Deprecated
@Mixin(Main.class)
public class MixinServerMain
{
	@Inject(method = "loadOrCreateConfig", at = @At("HEAD"))
	private static void simpleclouds$captureWorldPath_loadOrCreateConfig(DedicatedServerProperties properties, LevelStorageSource.LevelStorageAccess access, boolean flag, PackRepository repository, CallbackInfoReturnable<WorldLoader.InitConfig> ci)
	{
		//CloudTypeDataManager.setSimpleCloudsWorldPath(access.getLevelPath(CloudTypeDataManager.SIMPLE_CLOUDS_FOLDER));
	}
}
