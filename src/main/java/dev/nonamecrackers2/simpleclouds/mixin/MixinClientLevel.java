package dev.nonamecrackers2.simpleclouds.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.world.ClientCloudManager;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManagerAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;

@Mixin(ClientLevel.class)
public class MixinClientLevel implements CloudManagerAccessor<ClientLevel>
{
	@Unique
	private ClientCloudManager cloudManager;
	
	@Inject(method = "<init>", at = @At("TAIL"))
	public void simpleclouds$createCloudManager_init(CallbackInfo ci)
	{
		this.cloudManager = new ClientCloudManager((ClientLevel)(Object)this);
		this.cloudManager.init(SimpleCloudsConfig.CLIENT.useSpecificSeed.get() ? SimpleCloudsConfig.CLIENT.cloudSeed.get() : RandomSource.create().nextLong());
	}
	
	@Inject(method = "getSkyDarken", at = @At("RETURN"), cancellable = true)
	public void simpleclouds$modifySkyDarken_getSkyDarken(float partialTick, CallbackInfoReturnable<Float> ci)
	{
		ci.setReturnValue(ci.getReturnValue() * SimpleCloudsRenderer.getInstance().getWorldEffectsManager().getDarkenFactor(partialTick));
	}
	
	@Override
	public ClientCloudManager getCloudManager()
	{
		return this.cloudManager;
	}
}
