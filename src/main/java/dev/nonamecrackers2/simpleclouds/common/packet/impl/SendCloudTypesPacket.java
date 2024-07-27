package dev.nonamecrackers2.simpleclouds.common.packet.impl;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.gson.JsonParser;

import dev.nonamecrackers2.simpleclouds.client.packet.SimpleCloudsClientPacketHandler;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import nonamecrackers2.crackerslib.common.packet.Packet;

public class SendCloudTypesPacket extends Packet
{
	public Map<ResourceLocation, CloudType> types;
	public CloudType[] indexed;
	
	public SendCloudTypesPacket(CloudTypeDataManager manager)
	{
		super(true);
		this.types = manager.getCloudTypes();
		this.indexed = manager.getIndexedCloudTypes();
	}
	
	public SendCloudTypesPacket()
	{
		super(false);
	}
	
	@Override
	protected void decode(FriendlyByteBuf buffer)
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
		this.types = map;
		this.indexed = indexed;
	}
	
	@Override
	protected void encode(FriendlyByteBuf buffer)
	{
		buffer.writeVarInt(this.types.size());
		for (CloudType type : this.indexed)
		{
			buffer.writeResourceLocation(type.id());
			buffer.writeUtf(type.toJson().toString());
		}
	}
	
	@Override
	public Runnable getProcessor(NetworkEvent.Context context)
	{
		return client(() -> SimpleCloudsClientPacketHandler.handleCloudTypesPacket(this));
	}
}
