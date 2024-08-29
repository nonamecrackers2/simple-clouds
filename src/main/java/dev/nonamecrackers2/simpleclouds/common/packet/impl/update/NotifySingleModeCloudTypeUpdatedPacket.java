package dev.nonamecrackers2.simpleclouds.common.packet.impl.update;

import dev.nonamecrackers2.simpleclouds.client.packet.SimpleCloudsClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import nonamecrackers2.crackerslib.common.packet.Packet;

public class NotifySingleModeCloudTypeUpdatedPacket extends Packet
{
	public String newType;
	
	public NotifySingleModeCloudTypeUpdatedPacket(String type)
	{
		super(true);
		this.newType = type;
	}
	
	public NotifySingleModeCloudTypeUpdatedPacket()
	{
		super(false);
	}
	
	@Override
	protected void decode(FriendlyByteBuf buffer)
	{
		this.newType = buffer.readUtf();
	}
	
	@Override
	protected void encode(FriendlyByteBuf buffer)
	{
		buffer.writeUtf(this.newType);
	}
	
	@Override
	public Runnable getProcessor(NetworkEvent.Context context)
	{
		return client(() -> SimpleCloudsClientPacketHandler.handleNotifySingleModeCloudTypeUpdatedPacket(this));
	}
}
