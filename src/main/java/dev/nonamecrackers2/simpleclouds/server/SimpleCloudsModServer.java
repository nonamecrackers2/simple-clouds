package dev.nonamecrackers2.simpleclouds.server;

import dev.nonamecrackers2.simpleclouds.common.packet.SimpleCloudsPayloadRegistrar;
import dev.nonamecrackers2.simpleclouds.common.packet.handler.EmptySimpleCloudsClientPacketHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public class SimpleCloudsModServer
{
	public static void init(IEventBus modBus, IEventBus forgeBus)
	{
		modBus.addListener(SimpleCloudsModServer::registerPayloads);
	}
	
	private static void registerPayloads(RegisterPayloadHandlersEvent event)
	{
		SimpleCloudsPayloadRegistrar.register(event, EmptySimpleCloudsClientPacketHandler.INSTANCE);
	}
}
