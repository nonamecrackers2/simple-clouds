package dev.nonamecrackers2.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeDataManager;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;

@Mixin(WorldOpenFlows.class)
public class MixinWorldOpenFlows
{
	@Inject(method = "createWorldAccess", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
	public void simpleclouds$captureWorldPath_createWorldAccess(String level, CallbackInfoReturnable<LevelStorageSource.LevelStorageAccess> ci)
	{
		CloudTypeDataManager.setSimpleCloudsWorldPath(ci.getReturnValue().getLevelPath(CloudTypeDataManager.SIMPLE_CLOUDS_FOLDER));
	}
}
