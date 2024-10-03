package dev.nonamecrackers2.simpleclouds.common.packet.impl.update;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record NotifySingleModeCloudTypeUpdatedPayload(String newType) implements CustomPacketPayload
{
	public static final CustomPacketPayload.Type<NotifySingleModeCloudTypeUpdatedPayload> TYPE = new CustomPacketPayload.Type<>(SimpleCloudsMod.id("notify_single_mode_cloud_type_updated"));
	
	public static final StreamCodec<ByteBuf, NotifySingleModeCloudTypeUpdatedPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, NotifySingleModeCloudTypeUpdatedPayload::newType,
			NotifySingleModeCloudTypeUpdatedPayload::new
	);
	
	@Override
	public CustomPacketPayload.Type<NotifySingleModeCloudTypeUpdatedPayload> type()
	{
		return TYPE;
	}
}
