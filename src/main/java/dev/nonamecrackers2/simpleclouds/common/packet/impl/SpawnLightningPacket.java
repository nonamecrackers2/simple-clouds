package dev.nonamecrackers2.simpleclouds.common.packet.impl;

import dev.nonamecrackers2.simpleclouds.client.packet.SimpleCloudsClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import nonamecrackers2.crackerslib.common.packet.Packet;

public class SpawnLightningPacket extends Packet
{
	public BlockPos pos;
	public boolean onlySound;
	public int seed;
	public int maxDepth;
	public int branchCount;
	public float maxBranchLength;
	public float maxWidth;
	public float minimumPitch;
	public float maximumPitch;

	public SpawnLightningPacket(BlockPos pos, boolean onlySound, int seed, int maxDepth, int branchCount, float maxBranchLength, float maxWidth, float minimumPitch, float maximumPitch)
	{
		super(true);
		this.pos = pos;
		this.onlySound = onlySound;
		this.seed = seed;
		this.maxDepth = maxDepth;
		this.branchCount = branchCount;
		this.maxBranchLength = maxBranchLength;
		this.maxWidth = maxWidth;
		this.minimumPitch = minimumPitch;
		this.maximumPitch = maximumPitch;
	}
	
	public SpawnLightningPacket()
	{
		super(false);
	}

	@Override
	protected void decode(FriendlyByteBuf buffer)
	{
		this.pos = buffer.readBlockPos();
		this.onlySound = buffer.readBoolean();
		this.seed = buffer.readVarInt();
		this.maxDepth = buffer.readVarInt();
		this.branchCount = buffer.readVarInt();
		this.maxBranchLength = buffer.readFloat();
		this.maxWidth = buffer.readFloat();
		this.minimumPitch = buffer.readFloat();
		this.maximumPitch = buffer.readFloat();
	}
	
	@Override
	protected void encode(FriendlyByteBuf buffer)
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
	public Runnable getProcessor(NetworkEvent.Context context)
	{
		return client(() -> SimpleCloudsClientPacketHandler.handleSpawnLightningPacket(this));
	}
}
