package dev.nonamecrackers2.simpleclouds.client.config;

import com.google.common.base.Joiner;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.cloud.ClientSideCloudTypeManager;
import dev.nonamecrackers2.simpleclouds.client.mesh.SingleRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.world.ClientCloudManager;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.config.ModConfig;
import nonamecrackers2.crackerslib.client.gui.Popup;
import nonamecrackers2.crackerslib.common.config.listener.ConfigListener;

public class SimpleCloudsClientConfigListeners
{
	public static void registerListener()
	{
		ConfigListener.builder(ModConfig.Type.CLIENT, SimpleCloudsMod.MODID)
				.addListener(SimpleCloudsConfig.CLIENT.cloudMode, (o, n) -> requestReload())
				.addListener(SimpleCloudsConfig.CLIENT.cloudStyle, (o, n) -> requestReload())
				.addListener(SimpleCloudsConfig.CLIENT.singleModeCloudType, (o, n) -> onSingleModeCloudTypeUpdated(n))
				.buildAndRegister();
	}
	
	/**
	 * Updates the instance of the server config on the client with the value from the server.
	 * After called, this method will then request a reload from the cloud renderer, which
	 * will reinitialize the mesh generator so the change in the config value is applied.
	 */
	public static void onCloudModeUpdatedFromServer(CloudMode mode)
	{
		SimpleCloudsConfig.SERVER.cloudMode.set(mode);
		Popup.createInfoPopup(null, 300, Component.translatable("gui.simpleclouds.reload_confirmation.server.info"), () -> {
			SimpleCloudsRenderer.getInstance().requestReload();
		});
	}
	
	/**
	 * Updates the instance of the server config on the client with the value from the server.
     * After called, this method will then update the single mode cloud type for the single mode cloud mesh
     * generator.
	 */
	public static void onSingleModeCloudTypeUpdatedFromServer(String type)
	{
		SimpleCloudsConfig.SERVER.singleModeCloudType.set(type);
		if (SimpleCloudsRenderer.getInstance().getMeshGenerator() instanceof SingleRegionCloudMeshGenerator generator)
		{
			ClientSideCloudTypeManager.getInstance().getCloudTypeFromRawId(type).ifPresentOrElse(t -> {
				generator.setCloudType(t);
			}, () -> {
				generator.setCloudType(SimpleCloudsConstants.FALLBACK);
			});
		}
	}
	
	public static void onSingleModeCloudTypeUpdated(String type)
	{
		Minecraft.getInstance().execute(() -> 
		{
			if (ClientCloudManager.isAvailableServerSide())
				return;
			
			ResourceLocation loc = ResourceLocation.tryParse(type);
			var types = ClientSideCloudTypeManager.getInstance().getCloudTypes();
			if (loc != null && types.containsKey(loc) && ClientSideCloudTypeManager.isValidClientSideSingleModeCloudType(types.get(loc)))
			{
				if (SimpleCloudsRenderer.getInstance().getMeshGenerator() instanceof SingleRegionCloudMeshGenerator generator)
					generator.setCloudType(types.get(loc));
			}
			else
			{
				Component valid = Component.literal(Joiner.on(", ").join(types.values().stream().filter(t -> {
					return ClientSideCloudTypeManager.isValidClientSideSingleModeCloudType(t);
				}).map(t -> t.id().toString()).iterator())).withStyle(ChatFormatting.YELLOW);
				Popup.createInfoPopup(null, 300, Component.translatable("gui.simpleclouds.unknown_or_invalid_client_side_cloud_type.info", loc == null ? type : loc.toString(), valid));
			}
		});
	}
	
	public static void requestReload()
	{
		Minecraft.getInstance().execute(() -> 
		{
			if (ClientCloudManager.isAvailableServerSide())
				return;
			Popup.createYesNoPopup(null, () -> {
				SimpleCloudsRenderer.getInstance().requestReload();
			}, 300, Component.translatable("gui.simpleclouds.requires_reload.info"));
		});
	}
}
