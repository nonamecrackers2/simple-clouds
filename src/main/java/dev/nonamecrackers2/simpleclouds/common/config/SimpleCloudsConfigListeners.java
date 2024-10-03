package dev.nonamecrackers2.simpleclouds.common.config;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.update.NotifyCloudModeUpdatedPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.update.NotifySingleModeCloudTypeUpdatedPayload;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import nonamecrackers2.crackerslib.common.config.listener.ConfigListener;

public class SimpleCloudsConfigListeners
{
	public static void registerListener()
	{
		ConfigListener.builder(ModConfig.Type.SERVER, SimpleCloudsMod.MODID)
				.addListener(SimpleCloudsConfig.SERVER.cloudMode, (o, n) -> onCloudModeChanged(n))
				.addListener(SimpleCloudsConfig.SERVER.singleModeCloudType, (o, n) -> onSingleModeCloudTypeChanged(n))
				.buildAndRegister();
	}
	
	public static void onCloudModeChanged(CloudMode newMode)
	{
		executeOnServerThread(() -> PacketDistributor.sendToAllPlayers(new NotifyCloudModeUpdatedPayload(newMode)));
	}
	
	public static void onSingleModeCloudTypeChanged(String newType)
	{
		executeOnServerThread(() -> PacketDistributor.sendToAllPlayers(new NotifySingleModeCloudTypeUpdatedPayload(newType)));
	}
	
	private static void executeOnServerThread(Runnable runnable)
	{
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null)
			server.execute(runnable);
	}
}
