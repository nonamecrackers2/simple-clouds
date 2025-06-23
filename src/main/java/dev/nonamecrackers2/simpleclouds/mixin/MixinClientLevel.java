package dev.nonamecrackers2.simpleclouds.mixin;

import java.awt.Color;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.world.ClientCloudManager;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManagerHolder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel extends Level implements CloudManagerHolder<ClientLevel>
{
	protected MixinClientLevel(WritableLevelData data, ResourceKey<Level> dimension, RegistryAccess registry, Holder<DimensionType> dimensionType, Supplier<ProfilerFiller> profiler, boolean isClientSide, boolean isDebug, long seed, int maxChainedNeighbourUpdates)
	{
		super(data, dimension, registry, dimensionType, profiler, isClientSide, isDebug, seed, maxChainedNeighbourUpdates);
		throw new UnsupportedOperationException();
	}

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
	
	@Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
	public void simpleclouds$modifySkyColor_getSkyColor(Vec3 col, float partialTick, CallbackInfoReturnable<Vec3> ci)
	{
		Vec3 defaultCol = ci.getReturnValue();
		Color finalCol = SimpleCloudsRenderer.getInstance().getWorldEffectsManager().calculateSkyColor((float)defaultCol.x, (float)defaultCol.y, (float)defaultCol.z, partialTick);
		ci.setReturnValue(new Vec3(finalCol.getRed() / 255.0F, finalCol.getGreen() / 255.0F, finalCol.getBlue() / 255.0F));
	}
//	
//	@Override
//	public boolean isRaining()
//	{
//		if (CompatHelper.areShadersRunning())
//		{
//			ClientCloudManager manager = this.getCloudManager();
//			if (manager.hasReceivedSync() && manager.isRainingAt(Minecraft.getInstance().player.blockPosition()))
//				return true;
//		}
//		return super.isRaining();
//	}
//	
//	@Override
//	public boolean isThundering()
//	{
//		if (CompatHelper.areShadersRunning())
//		{
//			ClientCloudManager manager = this.getCloudManager();
//			Vec3 playerPos = Minecraft.getInstance().player.position();
//			if (manager.hasReceivedSync() && manager.getCloudTypeAtWorldPos((float)playerPos.x, (float)playerPos.z).getLeft().weatherType().includesThunder())
//				return true;
//		}
//		return super.isRaining();
//	}
}
