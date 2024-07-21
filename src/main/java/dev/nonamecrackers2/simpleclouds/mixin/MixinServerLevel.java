package dev.nonamecrackers2.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.nonamecrackers2.simpleclouds.common.world.CloudData;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManagerAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.DimensionDataStorage;

@Mixin(ServerLevel.class)
public abstract class MixinServerLevel implements CloudManagerAccessor
{
	@Unique
	private CloudManager cloudManager;
	@Shadow @Final
	private MinecraftServer server;
	
	@Inject(method = "<init>", at = @At("TAIL"))
	public void simpleclouds$createCloudManager_init(CallbackInfo ci)
	{
		this.cloudManager = new CloudManager((Level)(Object)this);
		//Do this so we hide the world seed
		this.cloudManager.init(RandomSource.create(this.server.getWorldData().worldGenOptions().seed()).nextLong());
		this.getDataStorage().computeIfAbsent(tag -> CloudData.load(this.cloudManager, tag), () -> new CloudData(this.cloudManager), CloudData.ID);
	}
	
	@Override
	public CloudManager getCloudManager()
	{
		return this.cloudManager;
	}
	
	@Shadow
	public abstract DimensionDataStorage getDataStorage();
}
