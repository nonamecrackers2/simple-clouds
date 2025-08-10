package dev.nonamecrackers2.simpleclouds.common.packet.impl;

import java.util.List;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SendCloudManagerPayload(List<CloudRegion> cloudRegions, long seed, float speed, float scrollAngle, int cloudHeight) implements CustomPacketPayload, CloudManagerInfoPayload
{
	public static final CustomPacketPayload.Type<SendCloudManagerPayload> TYPE = new CustomPacketPayload.Type<>(SimpleCloudsMod.id("send_cloud_manager"));
	
	public static final StreamCodec<FriendlyByteBuf, SendCloudManagerPayload> CODEC = CloudManagerInfoPayload.codec(SendCloudManagerPayload::new); 
	
	public SendCloudManagerPayload(CloudManager<?> manager)
	{
		this(
			manager.getClouds(),
			manager.getSeed(),
			manager.getCloudSpeed(),
			manager.getScrollAngle(),
			manager.getCloudHeight()
		);
	}
	
	public SendCloudManagerPayload(FriendlyByteBuf buffer)
	{
		this(
				buffer.readList(CloudRegion::new),
				buffer.readLong(),
				buffer.readFloat(), 
				buffer.readFloat(), 
				buffer.readVarInt() 
		);
	}
	
	@Override
	public void encode(FriendlyByteBuf buffer)
	{
		buffer.writeCollection(this.cloudRegions(), (b, c) -> c.toPacket(b));
		buffer.writeLong(this.seed);
		CloudManagerInfoPayload.super.encode(buffer);
	}
	
	@Override
	public CustomPacketPayload.Type<SendCloudManagerPayload> type()
	{
		return TYPE;
	}
}
