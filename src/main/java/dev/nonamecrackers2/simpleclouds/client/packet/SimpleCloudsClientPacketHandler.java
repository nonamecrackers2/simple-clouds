package dev.nonamecrackers2.simpleclouds.client.packet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.world.ClientCloudManager;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudManagerPacket;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.UpdateCloudManagerPacket;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.client.Minecraft;

public class SimpleCloudsClientPacketHandler
{
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static void handleUpdateCloudManagerPacket(UpdateCloudManagerPacket packet)
	{
		Minecraft mc = Minecraft.getInstance();
		CloudManager manager = CloudManager.get(mc.level);
		handleUpdateCloudManagerPacket(packet, manager);
		//LOGGER.debug("Updating client-side cloud manager");
	}
	
	public static void handleUpdateCloudManagerPacket(UpdateCloudManagerPacket packet, CloudManager manager)
	{
		manager.setScrollX(packet.scrollX);
		manager.setScrollY(packet.scrollY);
		manager.setScrollZ(packet.scrollZ);
		manager.setDirection(packet.direction);
		manager.setSpeed(packet.speed);
//		manager.setCloudMode(packet.cloudMode);
//		manager.setSingleModeFadeStart(packet.singleModeFadeStart);
//		manager.setSingleModeFadeEnd(packet.singleModeFadeEnd);
//		if (CloudTypeDataManager.INSTANCE.getCloudTypes().containsKey(packet.singleModeCloudType))
//			manager.setSingleModeCloudType(packet.singleModeCloudType);
//		else
//			LOGGER.warn("Client does not have cloud type with id '{}'", packet.singleModeCloudType);
		manager.setCloudHeight(packet.cloudHeight);
		if (manager instanceof ClientCloudManager clientManager)
			clientManager.setReceivedSync();
	}
	
	public static void handleSendCloudManagerPacket(SendCloudManagerPacket packet)
	{
		Minecraft mc = Minecraft.getInstance();
		CloudManager manager = CloudManager.get(mc.level);
		handleUpdateCloudManagerPacket(packet, manager);
		manager.setSeed(packet.seed);
		SimpleCloudsRenderer renderer = SimpleCloudsRenderer.getInstance();
		if (SimpleCloudsConfig.SERVER_SPEC.isLoaded())
		{
			if (SimpleCloudsConfig.SERVER.cloudMode.get() != renderer.getCurrentCloudMode())
			{
				LOGGER.debug("Looks like the server cloud mode does not match with the client. Requesting a reload...");
				renderer.onResourceManagerReload(mc.getResourceManager());
			}
		}
		else
		{
			LOGGER.warn("Server spec is not loaded");
		}
		LOGGER.debug("Received cloud manager info");
	}
}
