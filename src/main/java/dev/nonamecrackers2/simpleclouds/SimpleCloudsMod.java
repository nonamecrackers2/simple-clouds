package dev.nonamecrackers2.simpleclouds;

import dev.nonamecrackers2.simpleclouds.client.shader.SimpleCloudsShaders;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SimpleCloudsMod.MODID)
public class SimpleCloudsMod
{
	public static final String MODID = "simpleclouds";
	
	public SimpleCloudsMod()
	{
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.addListener(this::clientInit);
	}
	
	private void clientInit(FMLClientSetupEvent event)
	{
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.register(SimpleCloudsShaders.class);
	}
	
	public static ResourceLocation id(String path)
	{
		return new ResourceLocation(MODID, path);
	}
}
