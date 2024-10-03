package dev.nonamecrackers2.simpleclouds.common.packet.impl;

import org.joml.Vector3f;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record UpdateCloudManagerPayload(Vector3f direction, float speed, float scrollX, float scrollY, float scrollZ, int cloudHeight) implements CustomPacketPayload, CloudManagerInfoPayload
{
	public static final CustomPacketPayload.Type<UpdateCloudManagerPayload> TYPE = new CustomPacketPayload.Type<>(SimpleCloudsMod.id("update_cloud_manager"));
	
	public static final StreamCodec<FriendlyByteBuf, UpdateCloudManagerPayload> CODEC = CloudManagerInfoPayload.codec(UpdateCloudManagerPayload::new);
	
	public UpdateCloudManagerPayload(CloudManager<?> manager)
	{
		this(
			manager.getDirection(),
			manager.getSpeed(),
			manager.getScrollX(),
			manager.getScrollY(),
			manager.getScrollZ(),
			manager.getCloudHeight()
		);
	}
	
	public UpdateCloudManagerPayload(FriendlyByteBuf buffer)
	{
		this(buffer.readVector3f(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readVarInt());
	}
	
	@Override
	public CustomPacketPayload.Type<UpdateCloudManagerPayload> type()
	{
		return TYPE;
	}
}
