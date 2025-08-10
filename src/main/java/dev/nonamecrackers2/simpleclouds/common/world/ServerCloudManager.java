package dev.nonamecrackers2.simpleclouds.common.world;

import java.util.List;
import java.util.Queue;

import javax.annotation.Nullable;

import com.google.common.collect.Queues;

import dev.nonamecrackers2.simpleclouds.api.SimpleCloudsAPI;
import dev.nonamecrackers2.simpleclouds.api.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeDataManager;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudSpawningDataManager;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.ServerCloudGenerator;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SpawnLightningPayload;
import dev.nonamecrackers2.simpleclouds.mixin.MixinServerLevelAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

public class ServerCloudManager extends CloudManager<ServerLevel>
{
	private Queue<SyncType> toSync = Queues.newArrayDeque();
	
	public ServerCloudManager(ServerLevel level)
	{
		super(level, CloudTypeDataManager.getServerInstance(), CloudSpawningDataManager.getInstance()::getConfig, ServerCloudGenerator::new);
	}
	
	@Override
	public ServerCloudGenerator getCloudGenerator()
	{
		return (ServerCloudGenerator)super.getCloudGenerator();
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
		
		if (!this.useVanillaWeather && !SimpleCloudsAPI.getApi().getHooks().isExternalWeatherControlEnabled())
			this.level.setRainLevel(0.0F);
		
		if (this.isCloudGeneratorActive() && ((ServerCloudGenerator)this.getCloudGenerator()).checkAndResetSync())
			this.queueSync(SyncType.CLOUD_FORMATIONS);
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
		List<SpawnRegion> regions = regionsFromEntities(this.level.players(), SimpleCloudsConstants.LIGHTNING_SPAWN_DIAMETER / 2);
		
		SpawnRegion.randomPointForEachRegion(regions, this.random, SimpleCloudsConstants.LIGHTNING_SPAWN_ATTEMPTS, (r, p) -> 
		{
			var info = this.getCloudTypeAtWorldPos((float)p.x + 0.5F, (float)p.y + 0.5F);
			CloudType type = info.getLeft();
			if (!isValidLightning(type, info.getRight(), this.random))
				return false;
			this.spawnLightning(type, info.getRight(), p.x, p.y, this.random.nextInt(3) == 0);
			return true;
		});
	}

	@Override
	protected void spawnLightning(CloudType type, float fade, int x, int z, boolean soundOnly)
	{
		int y = (int)(type.stormStart() * SimpleCloudsConstants.CLOUD_SCALE + this.getCloudHeight());
		float spreadnessFactor = this.random.nextFloat();
		float length = spreadnessFactor * 300.0F + 200.0F;
		float minPitch = 20.0F + spreadnessFactor * 40.0F;
		float maxPitch = 80.0F + spreadnessFactor * 10.0F;
		PacketDistributor.sendToPlayersInDimension(this.level, new SpawnLightningPayload(new BlockPos(x, y, z), soundOnly, this.random.nextInt(), 4, 2, length, 20.0F, minPitch, maxPitch));
	}
	
	public void queueSync(SyncType syncType)
	{
		if (!this.toSync.contains(syncType))
			this.toSync.add(syncType);
	}
	
	public @Nullable SyncType fetchNextSyncOperation()
	{
		return this.toSync.poll();
	}
	
	public static List<SpawnRegion> regionsFromEntities(List<? extends Entity> entities, int radius)
	{
		return entities.stream().map(e -> {
			return new SpawnRegion(e.getBlockX(), e.getBlockZ(), radius);
		}).toList();
	}
}
