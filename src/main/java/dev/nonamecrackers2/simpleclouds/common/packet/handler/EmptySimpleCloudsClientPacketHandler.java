package dev.nonamecrackers2.simpleclouds.common.packet.handler;

import dev.nonamecrackers2.simpleclouds.client.packet.handler.SimpleCloudsClientPacketHandler;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudManagerPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudTypesPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SpawnLightningPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.UpdateCloudManagerPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.update.NotifyCloudModeUpdatedPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.update.NotifySingleModeCloudTypeUpdatedPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class EmptySimpleCloudsClientPacketHandler implements SimpleCloudsClientPacketHandler
{
	public static final EmptySimpleCloudsClientPacketHandler INSTANCE = new EmptySimpleCloudsClientPacketHandler();
	
	private EmptySimpleCloudsClientPacketHandler() {}
	
	@Override
	public void handleUpdateCloudManagerPayload(UpdateCloudManagerPayload packet, IPayloadContext context)
	{
	}

	@Override
	public void handleSendCloudManagerPayload(SendCloudManagerPayload packet, IPayloadContext context)
	{
	}

	@Override
	public void handleSendCloudTypesPayload(SendCloudTypesPayload packet, IPayloadContext context)
	{
	}

	@Override
	public void handleSpawnLightningPayload(SpawnLightningPayload packet, IPayloadContext context)
	{
	}

	@Override
	public void handleNotifyCloudModeUpdatedPayload(NotifyCloudModeUpdatedPayload packet, IPayloadContext context)
	{
	}

	@Override
	public void handleNotifySingleModeCloudTypeUpdatedPayload(NotifySingleModeCloudTypeUpdatedPayload packet, IPayloadContext context)
	{
	}
}
