package dev.nonamecrackers2.simpleclouds.common.packet.impl;

import org.joml.Vector3f;

import dev.nonamecrackers2.simpleclouds.client.packet.SimpleCloudsClientPacketHandler;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;
import nonamecrackers2.crackerslib.common.packet.Packet;

public class UpdateCloudManagerPacket extends Packet
{
	public float speed;
	public float scrollAngle;
	public int cloudHeight;
	
	public UpdateCloudManagerPacket(CloudManager<ServerLevel> manager)
	{
		super(true);
		this.speed = manager.getCloudSpeed();
		this.scrollAngle = manager.getScrollAngle();
		this.cloudHeight = manager.getCloudHeight();
	}
	
	public UpdateCloudManagerPacket()
	{
		super(false);
	}
	
	@Override
	protected void decode(FriendlyByteBuf buffer)
	{
		this.speed = buffer.readFloat();
		this.scrollAngle = buffer.readFloat();
		this.cloudHeight = buffer.readVarInt();
	}
	
	@Override
	protected void encode(FriendlyByteBuf buffer)
	{
		buffer.writeFloat(this.speed);
		buffer.writeFloat(this.scrollAngle);
		buffer.writeVarInt(this.cloudHeight);
	}
	
	@Override
	public Runnable getProcessor(NetworkEvent.Context context)
	{
		return client(() -> SimpleCloudsClientPacketHandler.handleUpdateCloudManagerPacket(this));
	}
}
