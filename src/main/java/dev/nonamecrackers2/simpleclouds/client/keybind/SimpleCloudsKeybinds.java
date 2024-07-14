package dev.nonamecrackers2.simpleclouds.client.keybind;

import org.lwjgl.glfw.GLFW;

import dev.nonamecrackers2.simpleclouds.client.gui.CloudPreviewerScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SimpleCloudsKeybinds
{
	public static final KeyMapping OPEN_GEN_PREVIEWER = new KeyMapping("simpleclouds.key.openGenPreviewer", GLFW.GLFW_KEY_F12, "simpleclouds.key.categories.main");
	
	public static void registerKeyMappings(RegisterKeyMappingsEvent event)
	{
		event.register(OPEN_GEN_PREVIEWER);
	}
	
	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event)
	{
		Minecraft mc = Minecraft.getInstance();
		while (OPEN_GEN_PREVIEWER.consumeClick())
			mc.setScreen(new CloudPreviewerScreen(null));
	}
}
