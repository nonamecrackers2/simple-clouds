package dev.nonamecrackers2.simpleclouds.client.packet.handler;

import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudManagerPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudTypesPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SpawnLightningPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.UpdateCloudManagerPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.update.NotifyCloudModeUpdatedPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.update.NotifySingleModeCloudTypeUpdatedPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public interface SimpleCloudsClientPacketHandler
{
	void handleUpdateCloudManagerPayload(UpdateCloudManagerPayload packet, IPayloadContext context);
	
	void handleSendCloudManagerPayload(SendCloudManagerPayload packet, IPayloadContext context);
	
	void handleSendCloudTypesPayload(SendCloudTypesPayload packet, IPayloadContext context);
	
	void handleSpawnLightningPayload(SpawnLightningPayload packet, IPayloadContext context);
	
	void handleNotifyCloudModeUpdatedPayload(NotifyCloudModeUpdatedPayload packet, IPayloadContext context);
	
	void handleNotifySingleModeCloudTypeUpdatedPayload(NotifySingleModeCloudTypeUpdatedPayload packet, IPayloadContext context);
}
