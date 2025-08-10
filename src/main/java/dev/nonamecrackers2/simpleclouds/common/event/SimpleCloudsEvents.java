package dev.nonamecrackers2.simpleclouds.common.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeDataManager;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudGenerator;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudSpawningDataManager;
import dev.nonamecrackers2.simpleclouds.common.command.CloudCommandSource;
import dev.nonamecrackers2.simpleclouds.common.command.CloudCommands;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudTypesPayload;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nonamecrackers2.crackerslib.common.command.ConfigCommandBuilder;

public class SimpleCloudsEvents
{
	private static final Logger LOGGER = LogManager.getLogger("simpleclouds/SimpleCloudsEvents");
	
	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event)
	{
		ConfigCommandBuilder.builder(event.getDispatcher(), SimpleCloudsMod.MODID).addSpec(ModConfig.Type.SERVER, SimpleCloudsConfig.SERVER_SPEC).addSpec(ModConfig.Type.COMMON, SimpleCloudsConfig.COMMON_SPEC).register();
		CloudCommands.register(event.getDispatcher(), "clouds", src -> src.hasPermission(2), CloudCommandSource.SERVER, CloudTypeDataManager.getServerInstance());
	}
	
	@SubscribeEvent
	public static void registerReloadListeners(AddReloadListenerEvent event)
	{
		CloudTypeDataManager manager = CloudTypeDataManager.getServerInstance();
		event.addListener(manager);
		CloudSpawningDataManager.optionalInitialize(manager);
		event.addListener(CloudSpawningDataManager.getInstance());
	}
	
	@SubscribeEvent
	public static void onDataSync(OnDatapackSyncEvent event)
	{
		CloudTypeDataManager manager = CloudTypeDataManager.getServerInstance();
		SendCloudTypesPayload payload = new SendCloudTypesPayload(manager.getCloudTypes(), manager.getIndexedCloudTypes());
		if (event.getPlayer() != null)
			PacketDistributor.sendToPlayer(event.getPlayer(), payload);
		else
			PacketDistributor.sendToAllPlayers(payload);
	}
	
	@SubscribeEvent
	public static void allowSleepingDuringThunderClouds(CanContinueSleepingEvent event)
	{
		LivingEntity entity = event.getEntity();
		if (!(entity instanceof Player))
			return;
		CloudManager<?> manager = CloudManager.get(entity.level());
		if (!manager.shouldUseVanillaWeather() && manager.getCloudTypeAtWorldPos((float)entity.getX(), (float)entity.getZ()).getLeft().weatherType().includesThunder())
			event.setContinueSleeping(true);
	}
	
	@SubscribeEvent
	public static void removeStormsAfterSleeping(SleepFinishedTimeEvent event)
	{
		LevelAccessor levelAccessor = event.getLevel();
		if (levelAccessor instanceof ServerLevel level)
		{
			CloudManager<?> manager = CloudManager.get(level);
			if (manager.shouldUseVanillaWeather())
				return;
			CloudGenerator generator = manager.getCloudGenerator();
			for (Player player : level.players())
			{
				CloudRegion region = generator.getCloudAtWorldPosition((float)player.getX(), (float)player.getZ());
				if (region == null)
					continue;
				CloudType type = manager.getCloudTypeForId(region.getCloudTypeId());
				if (type == null)
				{
					LOGGER.warn("Could not find cloud type with ID '{}' for cloud region; this should not happen!", region.getCloudTypeId());
					continue;
				}
				if (type.weatherType().includesThunder())
					generator.removeClouds(r -> r == region);
			}
		}
	}
}
