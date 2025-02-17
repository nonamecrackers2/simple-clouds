package dev.nonamecrackers2.simpleclouds.common.packet.impl;

import java.util.List;

import dev.nonamecrackers2.simpleclouds.client.packet.SimpleCloudsClientPacketHandler;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;

public class SendCloudManagerPacket extends UpdateCloudManagerPacket
{
	public List<CloudRegion> cloudRegions;
	public long seed;
	
	public SendCloudManagerPacket(CloudManager<ServerLevel> manager)
	{
		super(manager);
		this.cloudRegions = manager.getClouds();
		this.seed = manager.getSeed();
	}
	
	public SendCloudManagerPacket()
	{
		super();
	}
	
	@Override
	protected void decode(FriendlyByteBuf buffer)
	{
		super.decode(buffer);
		this.cloudRegions = buffer.readList(CloudRegion::new);
		this.seed = buffer.readLong();
	}
	
	@Override
	protected void encode(FriendlyByteBuf buffer)
	{
		super.encode(buffer);
		buffer.writeCollection(this.cloudRegions, (b, c) -> c.toPacket(b));
		buffer.writeLong(this.seed);
	}
	
	@Override
	public Runnable getProcessor(NetworkEvent.Context context)
	{
		return client(() -> SimpleCloudsClientPacketHandler.handleSendCloudManagerPacket(this));
	}
}
