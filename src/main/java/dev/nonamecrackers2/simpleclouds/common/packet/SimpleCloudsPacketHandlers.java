package dev.nonamecrackers2.simpleclouds.common.packet;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudManagerPacket;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudTypesPacket;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SpawnLightningPacket;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.UpdateCloudManagerPacket;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.update.NotifyCloudModeUpdatedPacket;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.update.NotifySingleModeCloudTypeUpdatedPacket;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import nonamecrackers2.crackerslib.common.packet.PacketUtil;

public class SimpleCloudsPacketHandlers
{
	public static final String VERSION = "1.0";
	public static final SimpleChannel MAIN = NetworkRegistry.newSimpleChannel(SimpleCloudsMod.id("main"), () -> VERSION, v -> true, VERSION::equals);
	
	public static void register()
	{
		PacketUtil.registerToClient(MAIN, UpdateCloudManagerPacket.class);
		PacketUtil.registerToClient(MAIN, SendCloudManagerPacket.class);
		PacketUtil.registerToClient(MAIN, SendCloudTypesPacket.class);
		PacketUtil.registerToClient(MAIN, SpawnLightningPacket.class);
		
		PacketUtil.registerToClient(MAIN, NotifyCloudModeUpdatedPacket.class);
		PacketUtil.registerToClient(MAIN, NotifySingleModeCloudTypeUpdatedPacket.class);
	}
}
