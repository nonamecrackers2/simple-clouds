package dev.nonamecrackers2.simpleclouds.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import dev.nonamecrackers2.simpleclouds.client.compat.SimpleCloudsSoundReplacements;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

@Mixin(targets = "net.minecraft.client.sounds.SoundManager$Preparations")
public class MixinSoundManagerPreperations
{
	@Shadow
	private Map<ResourceLocation, Resource> soundCache;
	
	@ModifyVariable(method = "handleRegistration", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/WeighedSoundEvents;addSound(Lnet/minecraft/client/sounds/Weighted;)V", shift = At.Shift.BEFORE))
	public Weighted<Sound> simpleclouds$overrideVanillaRainSounds_handleRegistration(Weighted<Sound> weighted, ResourceLocation loc, SoundEventRegistration soundReg)
	{
		return SimpleCloudsSoundReplacements.applyReplacement(weighted, loc, soundReg, this.soundCache);
	}
}
