package dev.nonamecrackers2.simpleclouds.common.init;

import java.util.function.Supplier;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SimpleCloudsSounds
{
	private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, SimpleCloudsMod.MODID);
	
	public static final Supplier<SoundEvent> DISTANT_THUNDER = createSoundEvent("distant_thunder");
	public static final Supplier<SoundEvent> CLOSE_THUNDER = createSoundEvent("close_thunder");
	
	private static Supplier<SoundEvent> createSoundEvent(String name)
	{
		return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(SimpleCloudsMod.id(name)));
	}
	
	public static void register(IEventBus modBus)
	{
		SOUND_EVENTS.register(modBus);
	}
}
