package dev.nonamecrackers2.simpleclouds.common.packet.impl;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record UpdateCloudManagerPayload(float speed, float scrollAngle, int cloudHeight) implements CustomPacketPayload, CloudManagerInfoPayload
{
	public static final CustomPacketPayload.Type<UpdateCloudManagerPayload> TYPE = new CustomPacketPayload.Type<>(SimpleCloudsMod.id("update_cloud_manager"));
	
	public static final StreamCodec<FriendlyByteBuf, UpdateCloudManagerPayload> CODEC = CloudManagerInfoPayload.codec(UpdateCloudManagerPayload::new);
	
	public UpdateCloudManagerPayload(CloudManager<?> manager)
	{
		this(
			manager.getCloudSpeed(),
			manager.getScrollAngle(),
			manager.getCloudHeight()
		);
	}
	
	public UpdateCloudManagerPayload(FriendlyByteBuf buffer)
	{
		this(buffer.readFloat(), buffer.readFloat(), buffer.readVarInt());
	}
	
	@Override
	public CustomPacketPayload.Type<UpdateCloudManagerPayload> type()
	{
		return TYPE;
	}
}
