package dev.nonamecrackers2.simpleclouds.client;

import dev.nonamecrackers2.simpleclouds.client.config.SimpleCloudsClientConfigListeners;
import dev.nonamecrackers2.simpleclouds.client.event.SimpleCloudsClientEvents;
import dev.nonamecrackers2.simpleclouds.client.keybind.SimpleCloudsKeybinds;
import dev.nonamecrackers2.simpleclouds.client.packet.handler.SimpleCloudsClientPacketHandlerImpl;
import dev.nonamecrackers2.simpleclouds.common.packet.SimpleCloudsPayloadRegistrar;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class SimpleCloudsModClient
{
	public static void init(IEventBus modBus, IEventBus forgeBus)
	{
		modBus.addListener(SimpleCloudsClientEvents::registerReloadListeners);
		modBus.addListener(SimpleCloudsKeybinds::registerKeyMappings);
		modBus.addListener(SimpleCloudsClientEvents::registerOverlays);
		modBus.addListener(SimpleCloudsClientEvents::registerClientPresets);
		modBus.addListener(SimpleCloudsModClient::registerPayloads);
		SimpleCloudsClientConfigListeners.registerListener();
	}
	
	public static void registerPayloads(RegisterPayloadHandlersEvent event)
	{
		SimpleCloudsPayloadRegistrar.register(event, SimpleCloudsClientPacketHandlerImpl.INSTANCE);
	}
}
