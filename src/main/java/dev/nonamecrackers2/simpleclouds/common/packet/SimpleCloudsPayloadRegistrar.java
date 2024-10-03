package dev.nonamecrackers2.simpleclouds.common.packet;

import dev.nonamecrackers2.simpleclouds.client.packet.handler.SimpleCloudsClientPacketHandler;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudManagerPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudTypesPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SpawnLightningPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.UpdateCloudManagerPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.update.NotifyCloudModeUpdatedPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.update.NotifySingleModeCloudTypeUpdatedPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class SimpleCloudsPayloadRegistrar
{
	public static void register(RegisterPayloadHandlersEvent event, SimpleCloudsClientPacketHandler clientHandler)
	{
		PayloadRegistrar registrar = event.registrar("1.0").optional();
		registrar.playToClient(
				NotifyCloudModeUpdatedPayload.TYPE, 
				NotifyCloudModeUpdatedPayload.CODEC, 
				clientHandler::handleNotifyCloudModeUpdatedPayload
		);
		registrar.playToClient(
				NotifySingleModeCloudTypeUpdatedPayload.TYPE, 
				NotifySingleModeCloudTypeUpdatedPayload.CODEC, 
				clientHandler::handleNotifySingleModeCloudTypeUpdatedPayload
		);
		registrar.playToClient(
				SendCloudManagerPayload.TYPE, 
				SendCloudManagerPayload.CODEC, 
				clientHandler::handleSendCloudManagerPayload
		);
		registrar.playToClient(
				SendCloudTypesPayload.TYPE, 
				SendCloudTypesPayload.CODEC, 
				clientHandler::handleSendCloudTypesPayload
		);
		registrar.playToClient(
				SpawnLightningPayload.TYPE, 
				SpawnLightningPayload.CODEC, 
				clientHandler::handleSpawnLightningPayload
		);
		registrar.playToClient(
				UpdateCloudManagerPayload.TYPE, 
				UpdateCloudManagerPayload.CODEC, 
				clientHandler::handleUpdateCloudManagerPayload
		);
	}
}
