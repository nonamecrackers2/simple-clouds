package dev.nonamecrackers2.simpleclouds.client.config;

import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import net.minecraft.network.chat.Component;
import nonamecrackers2.crackerslib.client.gui.Popup;

public class SimpleCloudsClientConfigListeners
{
	public static void onCloudModeUpdated(CloudMode mode)
	{
		SimpleCloudsConfig.SERVER.cloudMode.set(mode);
		Popup.createInfoPopup(null, 300, Component.translatable("gui.simpleclouds.reload_confirmation.server.info"), () -> {
			SimpleCloudsRenderer.getInstance().requestReload();
		});
	}
	
	public static void onSingleModeCloudTypeUpdated(String type)
	{
		SimpleCloudsConfig.SERVER.singleModeCloudType.set(type);
		if (SimpleCloudsConfig.SERVER.cloudMode.get() == CloudMode.SINGLE)
		{
			Popup.createInfoPopup(null, 300, Component.translatable("gui.simpleclouds.reload_confirmation.server.info"), () -> {
				SimpleCloudsRenderer.getInstance().requestReload();
			});
		}
	}
}
