package dev.nonamecrackers2.simpleclouds.common.event;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeDataManager;
import dev.nonamecrackers2.simpleclouds.common.command.CloudCommandSource;
import dev.nonamecrackers2.simpleclouds.common.command.CloudCommands;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.packet.SimpleCloudsPacketHandlers;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudTypesPacket;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.PacketTarget;
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
		PacketTarget target;
		if (event.getPlayer() != null)
			target = PacketDistributor.PLAYER.with(event::getPlayer);
		else
			target = PacketDistributor.ALL.noArg();
		SimpleCloudsPacketHandlers.MAIN.send(target, new SendCloudTypesPacket(CloudTypeDataManager.getServerInstance()));
	}
//	
//	@SubscribeEvent
//	public static void onServerStopping(ServerStoppingEvent event)
//	{
//		CloudTypeDataManager.setSimpleCloudsWorldPath(null);
//	}
}
