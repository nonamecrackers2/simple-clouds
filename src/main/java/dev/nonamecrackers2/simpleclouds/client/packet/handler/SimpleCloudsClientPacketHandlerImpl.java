package dev.nonamecrackers2.simpleclouds.client.packet.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.nonamecrackers2.simpleclouds.client.cloud.ClientSideCloudTypeManager;
import dev.nonamecrackers2.simpleclouds.client.config.SimpleCloudsClientConfigListeners;
import dev.nonamecrackers2.simpleclouds.client.mesh.multiregion.MultiRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.world.ClientCloudManager;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.CloudManagerInfoPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudManagerPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SendCloudTypesPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.SpawnLightningPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.UpdateCloudManagerPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.update.NotifyCloudModeUpdatedPayload;
import dev.nonamecrackers2.simpleclouds.common.packet.impl.update.NotifySingleModeCloudTypeUpdatedPayload;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SimpleCloudsClientPacketHandlerImpl implements SimpleCloudsClientPacketHandler
{
	private static final Logger LOGGER = LogManager.getLogger();
	public static final SimpleCloudsClientPacketHandlerImpl INSTANCE = new SimpleCloudsClientPacketHandlerImpl();
	
	private SimpleCloudsClientPacketHandlerImpl() {}
	
	@Override
	public void handleUpdateCloudManagerPayload(UpdateCloudManagerPayload packet, IPayloadContext context)
	{
		Minecraft mc = Minecraft.getInstance();
		CloudManager<ClientLevel> manager = CloudManager.get(mc.level);
		handleUpdateCloudManagerPayload(packet, manager);
		//LOGGER.debug("Updating client-side cloud manager");
	}
	
	private static void handleUpdateCloudManagerPayload(CloudManagerInfoPayload payload, CloudManager<ClientLevel> manager)
	{
		manager.setScrollX(payload.scrollX());
		manager.setScrollY(payload.scrollY());
		manager.setScrollZ(payload.scrollZ());
		manager.setDirection(payload.direction());
		manager.setSpeed(payload.speed());
		manager.setCloudHeight(payload.cloudHeight());
		if (manager instanceof ClientCloudManager clientManager)
			clientManager.setReceivedSync();
	}
	
	@Override
	public void handleSendCloudManagerPayload(SendCloudManagerPayload packet, IPayloadContext context)
	{
		Minecraft mc = Minecraft.getInstance();
		CloudManager<ClientLevel> manager = CloudManager.get(mc.level);
		handleUpdateCloudManagerPayload(packet, manager);
		manager.setSeed(packet.seed());
		manager.setRegionGenerator(packet.regionType());
		SimpleCloudsRenderer renderer = SimpleCloudsRenderer.getInstance();
		if (SimpleCloudsConfig.SERVER_SPEC.isLoaded())
		{
			if (SimpleCloudsConfig.SERVER.cloudMode.get() != renderer.getCloudMode() || packet.regionType() != renderer.getRegionGenerator())
			{
				LOGGER.debug("Looks like the server cloud mode or region generator does not match with the client. Requesting a reload...");
				renderer.requestReload();
			}
		}
		else
		{
			LOGGER.warn("Server spec is not loaded");
		}
		LOGGER.debug("Received cloud manager info");
	}
	
	@Override
	public void handleSendCloudTypesPayload(SendCloudTypesPayload packet, IPayloadContext context)
	{
		LOGGER.debug("Received {} synced cloud types", packet.types().size());
		ClientSideCloudTypeManager.getInstance().receiveSynced(packet.types(), packet.indexed());
		if (SimpleCloudsRenderer.getInstance().getMeshGenerator() instanceof MultiRegionCloudMeshGenerator meshGenerator)
		{
			if (packet.types().size() > MultiRegionCloudMeshGenerator.MAX_CLOUD_TYPES)
				LOGGER.warn("The amount of loaded cloud types exceeds the maximum of {}. Please be aware that not all cloud types loaded will be used.", MultiRegionCloudMeshGenerator.MAX_CLOUD_TYPES);
			else
				meshGenerator.setCloudTypes(packet.indexed());
		}
	}
	
	@Override
	public void handleSpawnLightningPayload(SpawnLightningPayload packet, IPayloadContext context)
	{
		SimpleCloudsRenderer.getInstance().getWorldEffectsManager().spawnLightning(packet.pos(), packet.onlySound(), packet.seed(), packet.maxDepth(), packet.branchCount(), packet.maxBranchLength(), packet.maxWidth(), packet.minimumPitch(), packet.maximumPitch());
	}
	
	@Override
	public void handleNotifyCloudModeUpdatedPayload(NotifyCloudModeUpdatedPayload packet, IPayloadContext context)
	{
		SimpleCloudsClientConfigListeners.onCloudModeUpdatedFromServer(packet.newMode());
	}
	
	@Override
	public void handleNotifySingleModeCloudTypeUpdatedPayload(NotifySingleModeCloudTypeUpdatedPayload packet, IPayloadContext context)
	{
		SimpleCloudsClientConfigListeners.onSingleModeCloudTypeUpdatedFromServer(packet.newType());
	}
}
