package dev.nonamecrackers2.simpleclouds.common.packet.impl;

import dev.nonamecrackers2.simpleclouds.client.packet.SimpleCloudsClientPacketHandler;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.RegionType;
import dev.nonamecrackers2.simpleclouds.common.registry.SimpleCloudsRegistries;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class SendCloudManagerPacket extends UpdateCloudManagerPacket
{
	public RegionType type;
	public long seed;
	
	public SendCloudManagerPacket(CloudManager manager)
	{
		super(manager);
		this.type = manager.getRegionGenerator();
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
		this.type = buffer.readRegistryId();
		this.seed = buffer.readLong();
	}
	
	@Override
	protected void encode(FriendlyByteBuf buffer)
	{
		super.encode(buffer);
		buffer.writeRegistryId(SimpleCloudsRegistries.getRegionTypeRegistry(), this.type);
		buffer.writeLong(this.seed);
	}
	
	@Override
	public Runnable getProcessor(NetworkEvent.Context context)
	{
		return client(() -> SimpleCloudsClientPacketHandler.handleSendCloudManagerPacket(this));
	}
}
