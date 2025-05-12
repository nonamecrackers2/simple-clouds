package dev.nonamecrackers2.simpleclouds.mixin.vanillacompat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.Level;

@Mixin(ThrownTrident.class)
public abstract class MixinThrownTrident extends AbstractArrow
{
	protected MixinThrownTrident(EntityType<? extends AbstractArrow> type, Level level)
	{
		super(type, level);
		throw new IllegalAccessError();
	}

	@Redirect(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isThundering()Z"))
	public boolean simpleclouds$channelingFix_onHitEntity(Level level)
	{
		CloudManager<?> manager = CloudManager.get(level);
		if (manager.shouldUseVanillaWeather())
			return level.isThundering();
		else
			return manager.getCloudTypeAtWorldPos((float)this.getX(), (float)this.getZ()).getLeft().weatherType().includesThunder();
	}
}
