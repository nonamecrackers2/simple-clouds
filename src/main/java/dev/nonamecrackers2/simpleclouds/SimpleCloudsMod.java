package dev.nonamecrackers2.simpleclouds;

import org.apache.maven.artifact.versioning.ArtifactVersion;

import dev.nonamecrackers2.simpleclouds.client.SimpleCloudsModClient;
import dev.nonamecrackers2.simpleclouds.client.event.SimpleCloudsClientEvents;
import dev.nonamecrackers2.simpleclouds.client.keybind.SimpleCloudsKeybinds;
import dev.nonamecrackers2.simpleclouds.client.shader.SimpleCloudsShaders;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfigListeners;
import dev.nonamecrackers2.simpleclouds.common.event.CloudManagerEvents;
import dev.nonamecrackers2.simpleclouds.common.event.SimpleCloudsDataEvents;
import dev.nonamecrackers2.simpleclouds.common.event.SimpleCloudsEvents;
import dev.nonamecrackers2.simpleclouds.common.init.RegionTypes;
import dev.nonamecrackers2.simpleclouds.common.init.SimpleCloudsSounds;
import dev.nonamecrackers2.simpleclouds.common.registry.SimpleCloudsRegistries;
import dev.nonamecrackers2.simpleclouds.server.SimpleCloudsModServer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(SimpleCloudsMod.MODID)
public class SimpleCloudsMod
{
	public static final String MODID = "simpleclouds";
	private static ArtifactVersion version;
	
	public SimpleCloudsMod(IEventBus modBus, ModContainer container)
	{
		version = container.getModInfo().getVersion();
		IEventBus forgeBus = NeoForge.EVENT_BUS;
		modBus.addListener(this::clientInit);
		modBus.addListener(this::commonInit);
		modBus.addListener(SimpleCloudsRegistries::registerRegistries);
		RegionTypes.register(modBus);
		SimpleCloudsSounds.register(modBus);
		setupSideOnly(modBus, forgeBus);
		modBus.addListener(SimpleCloudsDataEvents::gatherData);
		container.registerConfig(ModConfig.Type.CLIENT, SimpleCloudsConfig.CLIENT_SPEC);
		container.registerConfig(ModConfig.Type.COMMON, SimpleCloudsConfig.COMMON_SPEC);
		container.registerConfig(ModConfig.Type.SERVER, SimpleCloudsConfig.SERVER_SPEC);
	}
	
	private static void setupSideOnly(IEventBus modBus, IEventBus forgeBus)
	{
		switch (FMLEnvironment.dist)
		{
		case CLIENT:
		{
			SimpleCloudsModClient.init(modBus, forgeBus);
			break;
		}
		case DEDICATED_SERVER:
		{
			SimpleCloudsModServer.init(modBus, forgeBus);
			break;
		}
		}
	}
	
	private void commonInit(FMLCommonSetupEvent event)
	{
		IEventBus forgeBus = NeoForge.EVENT_BUS;
		forgeBus.register(CloudManagerEvents.class);
		forgeBus.register(SimpleCloudsEvents.class);
		SimpleCloudsConfigListeners.registerListener();
	}
	
	private void clientInit(FMLClientSetupEvent event)
	{
		IEventBus modBus = ModLoadingContext.get().getActiveContainer().getEventBus();
		modBus.register(SimpleCloudsShaders.class);
		modBus.addListener(SimpleCloudsClientEvents::registerConfigMenu);
		modBus.addListener(SimpleCloudsClientEvents::registerConfigMenuButton);
		IEventBus forgeBus = NeoForge.EVENT_BUS;
		forgeBus.register(SimpleCloudsClientEvents.class);
		forgeBus.register(SimpleCloudsKeybinds.class);
	}
	
	public static ResourceLocation id(String path)
	{
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}
	
	public static ArtifactVersion getModVersion()
	{
		return version;
	}
}
