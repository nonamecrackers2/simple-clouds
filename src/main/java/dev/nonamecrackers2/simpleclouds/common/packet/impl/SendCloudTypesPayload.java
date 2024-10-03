package dev.nonamecrackers2.simpleclouds.common.packet.impl;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.gson.JsonParser;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SendCloudTypesPayload(Map<ResourceLocation, CloudType> types, CloudType[] indexed) implements CustomPacketPayload
{
	public static final CustomPacketPayload.Type<SendCloudTypesPayload> TYPE = new CustomPacketPayload.Type<>(SimpleCloudsMod.id("send_cloud_types"));
	
	public static final StreamCodec<FriendlyByteBuf, SendCloudTypesPayload> CODEC = StreamCodec.ofMember(SendCloudTypesPayload::encode, SendCloudTypesPayload::decode);
	
	private void encode(FriendlyByteBuf buffer)
	{
		buffer.writeVarInt(this.types.size());
		for (CloudType type : this.indexed)
		{
			buffer.writeResourceLocation(type.id());
			buffer.writeUtf(type.toJson().toString());
		}
	}
	
	private static SendCloudTypesPayload decode(FriendlyByteBuf buffer)
	{
		int count = buffer.readVarInt();
		Map<ResourceLocation, CloudType> map = Maps.newHashMap();
		CloudType[] indexed = new CloudType[count];
		for (int i = 0; i < count; i++)
		{
			ResourceLocation id = buffer.readResourceLocation();
			CloudType type = CloudType.readFromJson(id, JsonParser.parseString(buffer.readUtf()).getAsJsonObject());
			map.put(id, type);
			indexed[i] = type;
		}
		return new SendCloudTypesPayload(map, indexed);
	}
	
	@Override
	public CustomPacketPayload.Type<SendCloudTypesPayload> type()
	{
		return TYPE;
	}
}
