package dev.nonamecrackers2.simpleclouds.common.packet.impl;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;

public interface CloudManagerInfoPayload
{
	public static <T extends CloudManagerInfoPayload> StreamCodec<FriendlyByteBuf, T> codec(StreamDecoder<FriendlyByteBuf, T> decoder)
	{
		return StreamCodec.ofMember(CloudManagerInfoPayload::encode, decoder);
	}
	
	default void encode(FriendlyByteBuf buffer)
	{
		buffer.writeFloat(this.speed());
		buffer.writeFloat(this.scrollAngle());
		buffer.writeVarInt(this.cloudHeight());
	}
	
	float speed();
	
	float scrollAngle();
	
	int cloudHeight();
}
