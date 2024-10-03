package dev.nonamecrackers2.simpleclouds.common.event;

import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudManagerPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.UpdateCloudManagerPayload;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import dev.nonamecrackers2.simpleclouds.common.world.ServerCloudManager;
import dev.nonamecrackers2.simpleclouds.common.world.SyncType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class CloudManagerEvents
{
	@SubscribeEvent
	public static void onWorldTick(LevelTickEvent.Pre event)
	{
		Level level = event.getLevel();
		CloudManager<?> manager = CloudManager.get(level);
		manager.tick();
		if (!level.isClientSide() && manager instanceof ServerCloudManager serverManager)
		{
			SyncType syncType = serverManager.getAndResetSync();
			if (syncType != SyncType.NONE)
			{
				switch (syncType)
				{
				case BASE_PROPERTIES:
				{
					PacketDistributor.sendToPlayersInDimension((ServerLevel)level, new SendCloudManagerPayload(manager));
					break;
				}
				case MOVEMENT:
				{
					PacketDistributor.sendToPlayersInDimension((ServerLevel)level, new UpdateCloudManagerPayload(manager));
					break;
				}
				default:
					throw new IllegalArgumentException("Unexpected value: " + syncType);
				}
			}
			else if (manager.getTickCount() % CloudManager.UPDATE_INTERVAL == 0)
			{
				PacketDistributor.sendToPlayersInDimension((ServerLevel)level, new UpdateCloudManagerPayload(manager));
			}
		}
	}
	
	@SubscribeEvent
	public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event)
	{
		if (event.getEntity() instanceof ServerPlayer player)
			update(player);
	}
	
	@SubscribeEvent
	public static void onPlayerSwapDimensions(PlayerEvent.PlayerChangedDimensionEvent event)
	{
		if (event.getEntity() instanceof ServerPlayer player)
			update(player);
	}
	
	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
	{
		if (event.getEntity() instanceof ServerPlayer player)
			update(player);
	}
	
	private static void update(ServerPlayer player)
	{
		PacketDistributor.sendToPlayer(player, new SendCloudManagerPayload(CloudManager.get(player.level())));
	}
}
