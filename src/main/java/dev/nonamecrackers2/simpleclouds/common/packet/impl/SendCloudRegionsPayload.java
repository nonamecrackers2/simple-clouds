package dev.nonamecrackers2.simpleclouds.common.packet.impl;

import java.util.List;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SendCloudRegionsPayload(List<CloudRegion> cloudRegions) implements CustomPacketPayload
{
	public static final CustomPacketPayload.Type<SendCloudRegionsPayload> TYPE = new CustomPacketPayload.Type<>(SimpleCloudsMod.id("send_cloud_regions"));
	public static final StreamCodec<FriendlyByteBuf, SendCloudRegionsPayload> CODEC = StreamCodec.ofMember(SendCloudRegionsPayload::encode, SendCloudRegionsPayload::new);
	
	public SendCloudRegionsPayload(FriendlyByteBuf buffer)
	{
		this(buffer.readList(CloudRegion::new));
	}
	
	public void encode(FriendlyByteBuf buffer)
	{
		buffer.writeCollection(this.cloudRegions, (b, c) -> c.toPacket(b));
	}
			
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}
}
