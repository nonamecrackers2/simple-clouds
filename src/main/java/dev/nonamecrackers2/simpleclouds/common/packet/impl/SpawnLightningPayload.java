package dev.nonamecrackers2.simpleclouds.common.packet.impl;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SpawnLightningPayload(
		BlockPos pos, 
		boolean onlySound, 
		int seed, 
		int maxDepth, 
		int branchCount, 
		float maxBranchLength, 
		float maxWidth, 
		float minimumPitch, 
		float maximumPitch
) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SpawnLightningPayload> TYPE = new CustomPacketPayload.Type<>(SimpleCloudsMod.id("spawn_lightning"));
	
	public static final StreamCodec<FriendlyByteBuf, SpawnLightningPayload> CODEC = StreamCodec.ofMember(SpawnLightningPayload::encode, SpawnLightningPayload::new);
	
	public SpawnLightningPayload(FriendlyByteBuf buffer)
	{
		this(
			buffer.readBlockPos(),
			buffer.readBoolean(),
			buffer.readVarInt(),
			buffer.readVarInt(),
			buffer.readVarInt(),
			buffer.readFloat(),
			buffer.readFloat(),
			buffer.readFloat(),
			buffer.readFloat()
		);
	}
	
	public void encode(FriendlyByteBuf buffer)
	{
		buffer.writeBlockPos(this.pos);
		buffer.writeBoolean(this.onlySound);
		buffer.writeVarInt(this.seed);
		buffer.writeVarInt(this.maxDepth);
		buffer.writeVarInt(this.branchCount);
		buffer.writeFloat(this.maxBranchLength);
		buffer.writeFloat(this.maxWidth);
		buffer.writeFloat(this.minimumPitch);
		buffer.writeFloat(this.maximumPitch);
	}
	
	@Override
	public CustomPacketPayload.Type<SpawnLightningPayload> type()
	{
		return TYPE;
	}
}
