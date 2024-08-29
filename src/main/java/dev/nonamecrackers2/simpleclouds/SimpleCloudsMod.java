package dev.nonamecrackers2.simpleclouds;

import org.apache.maven.artifact.versioning.ArtifactVersion;

import dev.nonamecrackers2.simpleclouds.client.event.SimpleCloudsClientEvents;
import dev.nonamecrackers2.simpleclouds.client.keybind.SimpleCloudsKeybinds;
import dev.nonamecrackers2.simpleclouds.client.renderer.WorldEffects;
import dev.nonamecrackers2.simpleclouds.client.shader.SimpleCloudsShaders;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfigListeners;
import dev.nonamecrackers2.simpleclouds.common.event.CloudManagerEvents;
import dev.nonamecrackers2.simpleclouds.common.event.SimpleCloudsDataEvents;
import dev.nonamecrackers2.simpleclouds.common.event.SimpleCloudsEvents;
import dev.nonamecrackers2.simpleclouds.common.init.RegionTypes;
import dev.nonamecrackers2.simpleclouds.common.init.SimpleCloudsSounds;
import dev.nonamecrackers2.simpleclouds.common.packet.SimpleCloudsPacketHandlers;
import dev.nonamecrackers2.simpleclouds.common.registry.SimpleCloudsRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;

//TODO: Re-add open gl version check in mods.toml when building mod
@Mod(SimpleCloudsMod.MODID)
public class SimpleCloudsMod
{
	public static final String MODID = "simpleclouds";
	private static ArtifactVersion version;
	
	public SimpleCloudsMod()
	{
		version = ModLoadingContext.get().getActiveContainer().getModInfo().getVersion();
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		modBus.addListener(this::clientInit);
		modBus.addListener(this::commonInit);
		modBus.addListener(SimpleCloudsRegistries::registerRegistries);
		RegionTypes.register(modBus);
		SimpleCloudsSounds.register(modBus);
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			modBus.addListener(SimpleCloudsClientEvents::registerReloadListeners);
			modBus.addListener(SimpleCloudsKeybinds::registerKeyMappings);
			modBus.addListener(SimpleCloudsClientEvents::registerOverlays);
			modBus.addListener(SimpleCloudsClientEvents::registerClientPresets);
			forgeBus.register(WorldEffects.class);
		});
		modBus.addListener(SimpleCloudsDataEvents::gatherData);
		ModLoadingContext context = ModLoadingContext.get();
		context.registerConfig(ModConfig.Type.CLIENT, SimpleCloudsConfig.CLIENT_SPEC);
		context.registerConfig(ModConfig.Type.COMMON, SimpleCloudsConfig.COMMON_SPEC);
		context.registerConfig(ModConfig.Type.SERVER, SimpleCloudsConfig.SERVER_SPEC);
		ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
	}
	
	private void commonInit(FMLCommonSetupEvent event)
	{
		SimpleCloudsPacketHandlers.register();
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		forgeBus.register(CloudManagerEvents.class);
		forgeBus.register(SimpleCloudsEvents.class);
		SimpleCloudsConfigListeners.registerListener();
	}
	
	private void clientInit(FMLClientSetupEvent event)
	{
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.register(SimpleCloudsShaders.class);
		modBus.addListener(SimpleCloudsClientEvents::registerConfigMenu);
		modBus.addListener(SimpleCloudsClientEvents::registerConfigMenuButton);
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		forgeBus.register(SimpleCloudsClientEvents.class);
		forgeBus.register(SimpleCloudsKeybinds.class);
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
