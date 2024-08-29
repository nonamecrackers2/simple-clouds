package dev.nonamecrackers2.simpleclouds.common.packet.impl.update;

import dev.nonamecrackers2.simpleclouds.client.packet.SimpleCloudsClientPacketHandler;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;
import nonamecrackers2.crackerslib.common.packet.Packet;

public class NotifyCloudModeUpdatedPacket extends Packet
{
	public CloudMode newMode;
	
	public NotifyCloudModeUpdatedPacket(CloudMode mode)
	{
		super(true);
		this.newMode = mode;
	}

	public NotifyCloudModeUpdatedPacket()
	{
		super(false);
	}
	
	@Override
	protected void decode(FriendlyByteBuf buffer)
	{
		this.newMode = buffer.readEnum(CloudMode.class);
	}
	
	@Override
	protected void encode(FriendlyByteBuf buffer)
	{
		buffer.writeEnum(this.newMode);
	}
	
	@Override
	public Runnable getProcessor(Context context)
	{
		return client(() -> SimpleCloudsClientPacketHandler.handleNotifyCloudModeUpdatedPacket(this));
	}
}
