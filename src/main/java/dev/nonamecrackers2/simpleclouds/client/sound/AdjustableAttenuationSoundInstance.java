package dev.nonamecrackers2.simpleclouds.client.sound;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class AdjustableAttenuationSoundInstance extends SimpleSoundInstance
{
	private final int attenuationDistance;
	
	public AdjustableAttenuationSoundInstance(SoundEvent sound, SoundSource source, float volume, float pitch, RandomSource random, double x, double y, double z, int attenuationDistance)
	{
		super(sound, source, volume, pitch, random, x, y, z);
		this.attenuationDistance = attenuationDistance;
	}
	
	@Override
	public WeighedSoundEvents resolve(SoundManager manager)
	{
		WeighedSoundEvents events = super.resolve(manager);
		this.sound = wrap(this.sound, this.attenuationDistance);
		return events;
	}
	
	@Override
	public Attenuation getAttenuation()
	{
		return Attenuation.LINEAR;
	}
	
	private static Sound wrap(Sound sound, int attenuationDistance)
	{
		return new Sound(sound.getLocation().toString(), sound.getVolume(), sound.getPitch(), sound.getWeight(), sound.getType(), sound.shouldStream(), sound.shouldPreload(), attenuationDistance);
	}
}
