package dev.nonamecrackers2.simpleclouds.common.event;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeDataManager;
import dev.nonamecrackers2.simpleclouds.common.command.CloudCommandSource;
import dev.nonamecrackers2.simpleclouds.common.command.CloudCommands;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudTypesPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nonamecrackers2.crackerslib.common.command.ConfigCommandBuilder;

public class SimpleCloudsEvents
{
	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event)
	{
		ConfigCommandBuilder.builder(event.getDispatcher(), SimpleCloudsMod.MODID).addSpec(ModConfig.Type.SERVER, SimpleCloudsConfig.SERVER_SPEC).addSpec(ModConfig.Type.COMMON, SimpleCloudsConfig.COMMON_SPEC).register();
		CloudCommands.register(event.getDispatcher(), "clouds", src -> src.hasPermission(2), CloudCommandSource.SERVER);
	}
	
	@SubscribeEvent
	public static void registerReloadListeners(AddReloadListenerEvent event)
	{
		event.addListener(CloudTypeDataManager.getServerInstance());
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
//	
//	@SubscribeEvent
//	public static void onServerStopping(ServerStoppingEvent event)
//	{
//		CloudTypeDataManager.setSimpleCloudsWorldPath(null);
//	}
}
