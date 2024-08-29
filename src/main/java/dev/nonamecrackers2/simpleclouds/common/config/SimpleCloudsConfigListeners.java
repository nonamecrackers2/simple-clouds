package dev.nonamecrackers2.simpleclouds.common.config;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.config.SimpleCloudsClientConfigListeners;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.common.packet.SimpleCloudsPacketHandlers;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.update.NotifyCloudModeUpdatedPacket;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.update.NotifySingleModeCloudTypeUpdatedPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.network.PacketDistributor;
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
		SimpleCloudsPacketHandlers.MAIN.send(PacketDistributor.ALL.noArg(), new NotifyCloudModeUpdatedPacket(newMode)); //TODO: Test on a dedicated server
	}
	
	public static void onSingleModeCloudTypeChanged(String newType)
	{
		SimpleCloudsPacketHandlers.MAIN.send(PacketDistributor.ALL.noArg(), new NotifySingleModeCloudTypeUpdatedPacket(newType));
	}
}
