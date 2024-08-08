package dev.nonamecrackers2.simpleclouds.common.event;

import dev.nonamecrackers2.simpleclouds.common.packet.SimpleCloudsPacketHandlers;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudManagerPacket;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.UpdateCloudManagerPacket;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
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
			CloudManager manager = CloudManager.get(level);
			manager.tick();
			if (!level.isClientSide)
			{
				CloudManager.SyncType syncType = manager.getAndResetSync();
				if (syncType != CloudManager.SyncType.NONE)
				{
					switch (syncType)
					{
					case BASE_PROPERTIES:
					{
						SimpleCloudsPacketHandlers.MAIN.send(PacketDistributor.DIMENSION.with(level::dimension), new SendCloudManagerPacket(manager));
						break;
					}
					case MOVEMENT:
					{
						SimpleCloudsPacketHandlers.MAIN.send(PacketDistributor.DIMENSION.with(level::dimension), new UpdateCloudManagerPacket(manager));
						break;
					}
					default:
						throw new IllegalArgumentException("Unexpected value: " + syncType);
					}
				}
				else if (manager.getTickCount() % CloudManager.UPDATE_INTERVAL == 0)
				{
					SimpleCloudsPacketHandlers.MAIN.send(PacketDistributor.DIMENSION.with(level::dimension), new UpdateCloudManagerPacket(manager));
				}
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
		SimpleCloudsPacketHandlers.MAIN.send(PacketDistributor.PLAYER.with(() -> player), new SendCloudManagerPacket(CloudManager.get(player.level())));
	}
}
