package dev.nonamecrackers2.simpleclouds.common.packet.impl;

import org.joml.Vector3f;

import dev.nonamecrackers2.simpleclouds.client.packet.SimpleCloudsClientPacketHandler;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import nonamecrackers2.crackerslib.common.packet.Packet;

public class UpdateCloudManagerPacket extends Packet
{
	public Vector3f direction;
	public float speed;
	public float scrollX;
	public float scrollY;
	public float scrollZ;
	public int cloudHeight;
	
	public UpdateCloudManagerPacket(CloudManager<?> manager)
	{
		super(true);
		this.direction = manager.getDirection();
		this.speed = manager.getSpeed();
		this.scrollX = manager.getScrollX();
		this.scrollY = manager.getScrollY();
		this.scrollZ = manager.getScrollZ();
		this.cloudHeight = manager.getCloudHeight();
	}
	
	public UpdateCloudManagerPacket()
	{
		super(false);
	}
	
	@Override
	protected void decode(FriendlyByteBuf buffer)
	{
		this.direction = buffer.readVector3f();
		this.speed = buffer.readFloat();
		this.scrollX = buffer.readFloat();
		this.scrollY = buffer.readFloat();
		this.scrollZ = buffer.readFloat();
		this.cloudHeight = buffer.readVarInt();
	}
	
	@Override
	protected void encode(FriendlyByteBuf buffer)
	{
		buffer.writeVector3f(this.direction);
		buffer.writeFloat(this.speed);
		buffer.writeFloat(this.scrollX);
		buffer.writeFloat(this.scrollY);
		buffer.writeFloat(this.scrollZ);
		buffer.writeVarInt(this.cloudHeight);
	}
	
	@Override
	public Runnable getProcessor(NetworkEvent.Context context)
	{
		return client(() -> SimpleCloudsClientPacketHandler.handleUpdateCloudManagerPacket(this));
	}
}
