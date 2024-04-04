package dev.nonamecrackers2.simpleclouds;

import org.apache.maven.artifact.versioning.ArtifactVersion;

import dev.nonamecrackers2.simpleclouds.client.event.SimpleCloudsClientEvents;
import dev.nonamecrackers2.simpleclouds.client.shader.SimpleCloudsShaders;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.event.SimpleCloudsDataEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SimpleCloudsMod.MODID)
public class SimpleCloudsMod
{
	public static final String MODID = "simpleclouds";
	private static ArtifactVersion version;
	
	public SimpleCloudsMod()
	{
		version = ModLoadingContext.get().getActiveContainer().getModInfo().getVersion();
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.addListener(SimpleCloudsClientEvents::registerReloadListeners);
		modBus.addListener(this::clientInit);
		modBus.addListener(SimpleCloudsDataEvents::gatherData);
		ModLoadingContext context = ModLoadingContext.get();
		context.registerConfig(ModConfig.Type.CLIENT, SimpleCloudsConfig.CLIENT_SPEC);
	}
	
	private void clientInit(FMLClientSetupEvent event)
	{
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.register(SimpleCloudsShaders.class);
		modBus.addListener(SimpleCloudsClientEvents::registerConfigMenu);
		modBus.addListener(SimpleCloudsClientEvents::registerConfigMenuButton);
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		forgeBus.register(SimpleCloudsClientEvents.class);
	}
	
	public static ResourceLocation id(String path)
	{
		return new ResourceLocation(MODID, path);
	}
	
	public static ArtifactVersion getModVersion()
	{
		return version;
	}
}
