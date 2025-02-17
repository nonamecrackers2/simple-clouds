package dev.nonamecrackers2.simpleclouds.common.packet.impl;

import java.util.List;

import dev.nonamecrackers2.simpleclouds.client.packet.SimpleCloudsClientPacketHandler;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;
import nonamecrackers2.crackerslib.common.packet.Packet;

public class SendCloudRegionsPacket extends Packet
{
	public List<CloudRegion> cloudRegions;
	
	public SendCloudRegionsPacket(CloudManager<ServerLevel> manager)
	{
		super(true);
		this.cloudRegions = manager.getCloudGenerator().getClouds();
	}
	
	public SendCloudRegionsPacket()
	{
		super(false);
	}
	
	@Override
	protected void decode(FriendlyByteBuf buffer)
	{
		this.cloudRegions = buffer.readList(CloudRegion::new);
	}
	
	@Override
	protected void encode(FriendlyByteBuf buffer)
	{
		buffer.writeCollection(this.cloudRegions, (b, c) -> c.toPacket(b));
	}
	
	@Override
	public Runnable getProcessor(NetworkEvent.Context context)
	{
		return client(() -> SimpleCloudsClientPacketHandler.handleSendCloudRegionsPacket(this));
	}
}
