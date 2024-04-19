package dev.nonamecrackers2.simpleclouds.client.gui;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.loading.FMLEnvironment;
import nonamecrackers2.crackerslib.client.gui.ConfigHomeScreen;
import nonamecrackers2.crackerslib.client.gui.title.TitleLogo;

public class SimpleCloudsConfigScreen extends ConfigHomeScreen
{
	public SimpleCloudsConfigScreen(String modid, Map<Type, ForgeConfigSpec> specs, TitleLogo title, boolean isWorldLoaded, boolean hasSinglePlayerServer, Screen previous, List<Supplier<AbstractButton>> extraButtons, int totalColumns)
	{
		super(modid, specs, title, isWorldLoaded, hasSinglePlayerServer, previous, extraButtons, totalColumns);
	}
	
	@Override
	protected void init()
	{
		super.init();
		if (!FMLEnvironment.production)
			this.addRenderableWidget(Button.builder(Component.translatable("gui.simpleclouds.cloud_previewer.button.title"), b -> this.minecraft.setScreen(new CloudPreviewerScreen())).pos(5, 5).width(100).build());
	}
}
