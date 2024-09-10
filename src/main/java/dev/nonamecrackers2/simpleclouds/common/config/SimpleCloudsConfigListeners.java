package dev.nonamecrackers2.simpleclouds.common.config;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.common.packet.SimpleCloudsPacketHandlers;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.update.NotifyCloudModeUpdatedPacket;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.update.NotifySingleModeCloudTypeUpdatedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
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
		executeOnServerThread(() -> SimpleCloudsPacketHandlers.MAIN.send(PacketDistributor.ALL.noArg(), new NotifyCloudModeUpdatedPacket(newMode)));
	}
	
	public static void onSingleModeCloudTypeChanged(String newType)
	{
		executeOnServerThread(() -> SimpleCloudsPacketHandlers.MAIN.send(PacketDistributor.ALL.noArg(), new NotifySingleModeCloudTypeUpdatedPacket(newType)));
	}
	
	private static void executeOnServerThread(Runnable runnable)
	{
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null)
			server.execute(runnable);
	}
}
