package dev.nonamecrackers2.simpleclouds.common.event;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.command.CloudCommandSource;
import dev.nonamecrackers2.simpleclouds.common.command.CloudCommands;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import nonamecrackers2.crackerslib.common.command.ConfigCommandBuilder;

public class SimpleCloudsEvents
{
	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event)
	{
		ConfigCommandBuilder.builder(event.getDispatcher(), SimpleCloudsMod.MODID).addSpec(ModConfig.Type.SERVER, SimpleCloudsConfig.SERVER_SPEC).register();
		CloudCommands.register(event.getDispatcher(), "clouds", src -> src.hasPermission(2), CloudCommandSource.SERVER);
	}
}
