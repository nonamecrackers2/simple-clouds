package dev.nonamecrackers2.simpleclouds.common.packet.impl;

import org.joml.Vector3f;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.RegionType;
import dev.nonamecrackers2.simpleclouds.common.registry.SimpleCloudsRegistries;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SendCloudManagerPayload(Vector3f direction, float speed, float scrollX, float scrollY, float scrollZ, int cloudHeight, RegionType regionType, long seed) implements CustomPacketPayload, CloudManagerInfoPayload
{
	public static final CustomPacketPayload.Type<SendCloudManagerPayload> TYPE = new CustomPacketPayload.Type<>(SimpleCloudsMod.id("send_cloud_manager"));
	
	public static final StreamCodec<FriendlyByteBuf, SendCloudManagerPayload> CODEC = CloudManagerInfoPayload.codec(SendCloudManagerPayload::new); 
	
	public SendCloudManagerPayload(CloudManager<?> manager)
	{
		this(
			manager.getDirection(),
			manager.getSpeed(),
			manager.getScrollX(),
			manager.getScrollY(),
			manager.getScrollZ(),
			manager.getCloudHeight(),
			manager.getRegionGenerator(),
			manager.getSeed()
		);
	}
	
	public SendCloudManagerPayload(FriendlyByteBuf buffer)
	{
		this(
				buffer.readVector3f(), 
				buffer.readFloat(), 
				buffer.readFloat(), 
				buffer.readFloat(), 
				buffer.readFloat(), 
				buffer.readVarInt(), 
				SimpleCloudsRegistries.REGION_TYPES.get(buffer.<RegionType>readResourceKey(SimpleCloudsRegistries.REGION_TYPES_KEY)), 
				buffer.readLong()
		);
	}
	
	@Override
	public void encode(FriendlyByteBuf buffer)
	{
		CloudManagerInfoPayload.super.encode(buffer);
		buffer.writeResourceKey(SimpleCloudsRegistries.REGION_TYPES.getResourceKey(this.regionType).get());
		buffer.writeLong(this.seed);
	}
	
	@Override
	public CustomPacketPayload.Type<SendCloudManagerPayload> type()
	{
		return TYPE;
	}
}
