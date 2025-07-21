package dev.nonamecrackers2.simpleclouds.client.compat;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableMap;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvents;

public class SimpleCloudsSoundReplacements
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/SimpleCloudsSoundReplacements");
	private static final Map<ResourceLocation, SimpleCloudsSoundReplacements.Replacements> REPLACEMENT_SOUNDS = ImmutableMap.of(
			SoundEvents.WEATHER_RAIN.getLocation(), new SimpleCloudsSoundReplacements.Replacements(IntStream.range(1, 9).mapToObj(i -> "ambient/weather/rain" + i).toList(), SimpleCloudsCompatHelper::useCustomRainSounds),
			SoundEvents.WEATHER_RAIN_ABOVE.getLocation(), new SimpleCloudsSoundReplacements.Replacements(IntStream.range(1, 5).mapToObj(i -> "ambient/weather/rain" + i).toList(), SimpleCloudsCompatHelper::useCustomRainSounds)
	);
	
	public static Weighted<Sound> applyReplacement(Weighted<Sound> currentSound, ResourceLocation soundLoc, SoundEventRegistration soundReg, Map<ResourceLocation, Resource> soundCache)
	{
		if (currentSound instanceof Sound sound)
		{
			if (!REPLACEMENT_SOUNDS.containsKey(soundLoc))
				return currentSound;
			SimpleCloudsSoundReplacements.Replacements replacements = REPLACEMENT_SOUNDS.get(soundLoc);
			if (!replacements.condition().get() || !replacements.replacements().contains(sound.getLocation().getPath()))
				return currentSound;
			String newLoc = SimpleCloudsMod.MODID + ":" + sound.getLocation().getPath();
			Sound newSound = new Sound(newLoc, sound.getVolume(), sound.getPitch(), sound.getWeight(), Sound.Type.FILE, false, sound.shouldPreload(), sound.getAttenuationDistance());
			if (!soundCache.containsKey(newSound.getPath()))
			{
				LOGGER.error("Could not find replacement sound '{}'", newLoc);
				return currentSound;
			}
			return newSound;
		}
		return currentSound;
	}
	
	private static record Replacements(List<String> replacements, Supplier<Boolean> condition) {}
}
