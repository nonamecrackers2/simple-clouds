package dev.nonamecrackers2.simpleclouds.common.packet.impl;

import org.joml.Vector3f;

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
		buffer.writeVector3f(this.direction());
		buffer.writeFloat(this.speed());
		buffer.writeFloat(this.scrollX());
		buffer.writeFloat(this.scrollY());
		buffer.writeFloat(this.scrollZ());
		buffer.writeVarInt(this.cloudHeight());
	}
	
	Vector3f direction();
	
	float speed();
	
	float scrollX();
	
	float scrollY();
	
	float scrollZ();
	
	int cloudHeight();
}
