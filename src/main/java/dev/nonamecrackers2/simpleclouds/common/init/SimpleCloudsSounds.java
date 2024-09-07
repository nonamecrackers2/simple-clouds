package dev.nonamecrackers2.simpleclouds.common.init;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SimpleCloudsSounds
{
	private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SimpleCloudsMod.MODID);
	
	public static final RegistryObject<SoundEvent> DISTANT_THUNDER = createSoundEvent("distant_thunder");
	public static final RegistryObject<SoundEvent> CLOSE_THUNDER = createSoundEvent("close_thunder");
	
	private static RegistryObject<SoundEvent> createSoundEvent(String name)
	{
		return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(SimpleCloudsMod.id(name)));
	}
	
	public static void register(IEventBus modBus)
	{
		SOUND_EVENTS.register(modBus);
	}
}
