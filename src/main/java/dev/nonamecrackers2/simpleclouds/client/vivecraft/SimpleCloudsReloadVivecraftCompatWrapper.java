package dev.nonamecrackers2.simpleclouds.client.vivecraft;

import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import nonamecrackers2.crackerslib.common.compat.CompatHelper;

public class SimpleCloudsReloadVivecraftCompatWrapper implements ResourceManagerReloadListener
{
	private final SimpleCloudsRenderer renderer;
	
	public SimpleCloudsReloadVivecraftCompatWrapper(SimpleCloudsRenderer renderer)
	{
		this.renderer = renderer;
	}
	
	@Override
	public void onResourceManagerReload(ResourceManager manager)
	{
		if (!CompatHelper.isVrActive())
			this.renderer.onResourceManagerReload(manager);
	}
}
