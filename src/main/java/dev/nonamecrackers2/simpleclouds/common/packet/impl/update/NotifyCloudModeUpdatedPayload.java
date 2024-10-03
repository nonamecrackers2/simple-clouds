package dev.nonamecrackers2.simpleclouds.common.packet.impl.update;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nonamecrackers2.crackerslib.common.packet.PacketHelper;

public record NotifyCloudModeUpdatedPayload(CloudMode newMode) implements CustomPacketPayload
{
	public static final CustomPacketPayload.Type<NotifyCloudModeUpdatedPayload> TYPE = new CustomPacketPayload.Type<>(SimpleCloudsMod.id("notify_cloud_mode_updated"));
	
	public static final StreamCodec<ByteBuf, NotifyCloudModeUpdatedPayload> CODEC = StreamCodec.composite(
			PacketHelper.enumStreamCodec(CloudMode.class), NotifyCloudModeUpdatedPayload::newMode,
			NotifyCloudModeUpdatedPayload::new
	);
	
	@Override
	public CustomPacketPayload.Type<NotifyCloudModeUpdatedPayload> type()
	{
		return TYPE;
	}
}
