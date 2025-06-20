package dev.nonamecrackers2.simpleclouds.common.event;

import java.util.List;

import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudGenerator;
import dev.nonamecrackers2.simpleclouds.common.packet.SimpleCloudsPacketHandlers;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudManagerPacket;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudRegionsPacket;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.UpdateCloudManagerPacket;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import dev.nonamecrackers2.simpleclouds.common.world.ServerCloudManager;
import dev.nonamecrackers2.simpleclouds.common.world.SpawnRegion;
import dev.nonamecrackers2.simpleclouds.common.world.SyncType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

public class CloudManagerEvents
{
	@SubscribeEvent
	public static void onWorldTick(TickEvent.LevelTickEvent event)
	{
		Level level = event.level;
		if (event.phase == TickEvent.Phase.START)
		{
			CloudManager<?> manager = CloudManager.get(level);
			manager.tick();
			if (!level.isClientSide && manager instanceof ServerCloudManager serverManager)
			{
				SyncType syncType = serverManager.fetchNextSyncOperation();
				if (syncType != null)
				{
					switch (syncType)
					{
					case BASE_PROPERTIES:
					{
						SimpleCloudsPacketHandlers.MAIN.send(PacketDistributor.DIMENSION.with(level::dimension), new SendCloudManagerPacket(serverManager));
						break;
					}
					case MOVEMENT:
					{
						SimpleCloudsPacketHandlers.MAIN.send(PacketDistributor.DIMENSION.with(level::dimension), new UpdateCloudManagerPacket(serverManager));
						break;
					}
					case CLOUD_FORMATIONS:
					{
						for (ServerPlayer player : ((ServerLevel)level).players())
							sendCloudRegionsToPlayer(player);
						break;
					}
					default:
						throw new IllegalArgumentException("Unexpected value: " + syncType);
					}
				}
				else if (manager.getTickCount() % CloudManager.UPDATE_INTERVAL == 0)
				{
					SimpleCloudsPacketHandlers.MAIN.send(PacketDistributor.DIMENSION.with(level::dimension), new UpdateCloudManagerPacket(serverManager));
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event)
	{
		CloudManager.get(event.getEntity().level()).onPlayerJoin(event.getEntity());
		if (event.getEntity() instanceof ServerPlayer player)
			update(player);
	}
	
	@SubscribeEvent
	public static void onPlayerSwapDimensions(PlayerEvent.PlayerChangedDimensionEvent event)
	{
		System.out.println(event.getEntity().position());
		CloudManager.get(event.getEntity().level()).onPlayerJoin(event.getEntity());
		if (event.getEntity() instanceof ServerPlayer player)
			update(player);
	}
	
	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
	{
		CloudManager.get(event.getEntity().level()).onPlayerJoin(event.getEntity());
		if (event.getEntity() instanceof ServerPlayer player)
			update(player);
	}
	
	private static void update(ServerPlayer player)
	{
		SimpleCloudsPacketHandlers.MAIN.send(PacketDistributor.PLAYER.with(() -> player), new SendCloudManagerPacket(CloudManager.get((ServerLevel)player.level())));
		sendCloudRegionsToPlayer(player);
	}
	
	private static void sendCloudRegionsToPlayer(ServerPlayer player)
	{
		CloudManager<ServerLevel> manager = CloudManager.get(player.serverLevel());
		SpawnRegion region = new SpawnRegion(player.getBlockX(), player.getBlockZ(), CloudGenerator.SPAWN_RADIUS);
		List<CloudRegion> formationsForPlayer = manager.getCloudGenerator().getCloudsInRegion(region);
		System.out.println("sending " + formationsForPlayer.size() + " clouds to player " + player.getDisplayName().getString());
		SimpleCloudsPacketHandlers.MAIN.send(PacketDistributor.PLAYER.with(() -> player), new SendCloudRegionsPacket(formationsForPlayer));
	}
}
