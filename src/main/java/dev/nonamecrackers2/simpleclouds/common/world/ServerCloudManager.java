package dev.nonamecrackers2.simpleclouds.common.world;

import java.util.List;

import org.joml.Vector2i;

import com.google.common.collect.Lists;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeDataManager;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SpawnLightningPayload;
import dev.nonamecrackers2.simpleclouds.mixin.MixinServerLevelAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.neoforged.neoforge.network.PacketDistributor;

public class ServerCloudManager extends CloudManager<ServerLevel>
{
	private SyncType syncType = SyncType.NONE;
	
	public ServerCloudManager(ServerLevel level)
	{
		super(level, CloudTypeDataManager.getServerInstance());
	}
	
	@Override
	public CloudMode getCloudMode()
	{
		return SimpleCloudsConfig.SERVER.cloudMode.get();
	}
	
	@Override
	public String getSingleModeCloudTypeRawId()
	{
		return SimpleCloudsConfig.SERVER.singleModeCloudType.get();
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		if (!this.useVanillaWeather)
			this.level.setRainLevel(0.0F);
	}
	
	@Override
	protected void resetVanillaWeather()
	{
		((MixinServerLevelAccessor)this.level).simpleclouds$invokeResetWeatherCycle();
		PlayerList list = this.level.getServer().getPlayerList();
		list.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 0.0F), this.level.dimension());
        list.broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0.0F), this.level.dimension());
	}

	@Override
	protected void attemptToSpawnLightning()
	{
		List<ServerCloudManager.LightningRegion> regions = this.level.players().stream().map(player -> {
			return new ServerCloudManager.LightningRegion(player.getBlockX(), player.getBlockZ(), SimpleCloudsConstants.LIGHTNING_SPAWN_DIAMETER / 2);
		}).toList();
		
		List<Vector2i> prevPositions = Lists.newArrayList();
		for (ServerCloudManager.LightningRegion region : regions)
		{
			if (prevPositions.stream().anyMatch(pos -> region.includesPoint(pos.x, pos.y)))
				continue;
			for (int i = 0; i < SimpleCloudsConstants.LIGHTNING_SPAWN_ATTEMPTS; i++)
			{
				int x = this.random.nextInt(region.radius() * 2) - region.radius() + region.x();
				int z = this.random.nextInt(region.radius() * 2) - region.radius() + region.z();
				var info = this.getCloudTypeAtPosition((float)x + 0.5F, (float)z + 0.5F);
				CloudType type = info.getLeft();
				if (!isValidLightning(type, info.getRight(), this.random))
					continue;
				this.spawnLightning(type, info.getRight(), x, z, this.random.nextInt(3) == 0);
				prevPositions.add(new Vector2i(x, z));
				break;
			}
		}
	}

	@Override
	protected void spawnLightning(CloudType type, float fade, int x, int z, boolean soundOnly)
	{
		int y = (int)(type.stormStart() * SimpleCloudsConstants.CLOUD_SCALE + 256.0F);
		float spreadnessFactor = this.random.nextFloat();
		float length = spreadnessFactor * 300.0F + 200.0F;
		float minPitch = 20.0F + spreadnessFactor * 40.0F;
		float maxPitch = 80.0F + spreadnessFactor * 10.0F;
		PacketDistributor.sendToPlayersInDimension(this.level, new SpawnLightningPayload(new BlockPos(x, y, z), soundOnly, this.random.nextInt(), 4, 2, length, 20.0F, minPitch, maxPitch));
	}
	
	public void setRequiresSync(SyncType syncType)
	{
		this.syncType = syncType;
	}
	
	public SyncType getAndResetSync()
	{
		if (this.syncType != SyncType.NONE)
		{
			SyncType syncType = this.syncType;
			this.syncType = SyncType.NONE;
			return syncType;
		}
		else
		{
			return SyncType.NONE;
		}
	}
	
	private static record LightningRegion(int x, int z, int radius)
	{
		public boolean includesPoint(int x, int z)
		{
			return x >= this.getMinX() && x <= this.getMaxX() && z >= this.getMinZ() && z <= this.getMaxZ();
		}
		
		public int getMinX()
		{
			return this.x - this.radius;
		}
		
		public int getMaxX()
		{
			return this.x + this.radius;
		}
		
		public int getMinZ()
		{
			return this.z - this.radius;
		}
		
		public int getMaxZ()
		{
			return this.z + this.radius;
		}
	}
}
