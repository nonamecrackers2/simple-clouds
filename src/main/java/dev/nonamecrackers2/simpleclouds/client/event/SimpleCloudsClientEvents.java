package dev.nonamecrackers2.simpleclouds.client.event;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.api.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.client.cloud.ClientSideCloudTypeManager;
import dev.nonamecrackers2.simpleclouds.client.cloud.spawning.ClientSideCloudSpawningManager;
import dev.nonamecrackers2.simpleclouds.client.command.ClientCloudCommandHelper;
import dev.nonamecrackers2.simpleclouds.client.gui.CloudPreviewerScreen;
import dev.nonamecrackers2.simpleclouds.client.gui.SimpleCloudsConfigScreen;
import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.mesh.LevelOfDetailOptions;
import dev.nonamecrackers2.simpleclouds.client.mesh.SingleRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.mesh.multiregion.MultiRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsDebugOverlayRenderer;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.renderer.WorldEffects;
import dev.nonamecrackers2.simpleclouds.client.renderer.settings.CloudsRendererSettings;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.ComputeShader;
import dev.nonamecrackers2.simpleclouds.client.world.ClientCloudManager;
import dev.nonamecrackers2.simpleclouds.client.world.FogRenderMode;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeDataManager;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import nonamecrackers2.crackerslib.client.event.impl.AddConfigEntryToMenuEvent;
import nonamecrackers2.crackerslib.client.event.impl.ConfigMenuButtonEvent;
import nonamecrackers2.crackerslib.client.event.impl.RegisterConfigScreensEvent;
import nonamecrackers2.crackerslib.client.gui.ConfigHomeScreen;
import nonamecrackers2.crackerslib.client.gui.title.ImageTitle;
import nonamecrackers2.crackerslib.common.command.ConfigCommandBuilder;
import nonamecrackers2.crackerslib.common.config.preset.ConfigPreset;
import nonamecrackers2.crackerslib.common.config.preset.RegisterConfigPresetsEvent;

public class SimpleCloudsClientEvents
{
	public static void registerOverlays(RegisterGuiOverlaysEvent event)
	{
		event.registerBelow(VanillaGuiOverlay.DEBUG_TEXT.id(), "simple_clouds_debug", SimpleCloudsDebugOverlayRenderer::render);
	}
	
	public static void registerReloadListeners(RegisterClientReloadListenersEvent event)
	{
		CloudTypeDataManager manager = ClientSideCloudTypeManager.getInstance().getClientSideDataManager();
		event.registerReloadListener(manager);
		ClientSideCloudSpawningManager.optionalInitializeOnClient(manager);
		event.registerReloadListener(ClientSideCloudSpawningManager.getClientInstance());
		SimpleCloudsRenderer.initialize(CloudsRendererSettings.DEFAULT);
		event.registerReloadListener((ResourceManagerReloadListener)(m -> {
			ComputeShader.destroyCompiledShaders();
		}));
		event.registerReloadListener(SimpleCloudsRenderer.getInstance());
		CloudPreviewerScreen.addCloudMeshListener(event);
	}
	
	public static void registerConfigMenu(RegisterConfigScreensEvent event)
	{
		event.builder(ConfigHomeScreen.builder(ImageTitle.ofMod(SimpleCloudsMod.MODID, 192, 96, 1.0F)).crackersDefault().build(SimpleCloudsConfigScreen::new))
				.addSpec(ModConfig.Type.CLIENT, SimpleCloudsConfig.CLIENT_SPEC)
				.addSpec(ModConfig.Type.COMMON, SimpleCloudsConfig.COMMON_SPEC)
				.addSpec(ModConfig.Type.SERVER, SimpleCloudsConfig.SERVER_SPEC).register();
	}
	
	public static void registerConfigMenuButton(ConfigMenuButtonEvent event)
	{
		event.defaultButtonWithSingleCharacter('S', 0xFFADF7FF);
	}
	
	public static void registerClientPresets(RegisterConfigPresetsEvent event)
	{
		//TODO: Presets don't seem to work very well in the config menu, probably something with CrackersLib
		event.registerPreset(ModConfig.Type.CLIENT, ConfigPreset.builder(Component.translatable("simpleclouds.config.preset.medium"))
				.setDescription(Component.translatable("simpleclouds.config.preset.medium.description"))
				.setPreset(SimpleCloudsConfig.CLIENT.framesToGenerateMesh, 10)
				.setPreset(SimpleCloudsConfig.CLIENT.levelOfDetail, LevelOfDetailOptions.MEDIUM)
				.setPreset(SimpleCloudsConfig.CLIENT.shadowDistance, 2500).build());
		event.registerPreset(ModConfig.Type.CLIENT, ConfigPreset.builder(Component.translatable("simpleclouds.config.preset.low"))
				.setDescription(Component.translatable("simpleclouds.config.preset.low.description"))
				.setPreset(SimpleCloudsConfig.CLIENT.framesToGenerateMesh, 20)
				.setPreset(SimpleCloudsConfig.CLIENT.levelOfDetail, LevelOfDetailOptions.LOW)
				.setPreset(SimpleCloudsConfig.CLIENT.transparency, false)
				.setPreset(SimpleCloudsConfig.CLIENT.atmosphericClouds, false)
				.setPreset(SimpleCloudsConfig.CLIENT.shadowDistance, 2500)
				.setPreset(SimpleCloudsConfig.CLIENT.distantShadows, false).build());
		event.registerPreset(ModConfig.Type.CLIENT, ConfigPreset.builder(Component.translatable("simpleclouds.config.preset.ultra_low"))
				.setDescription(Component.translatable("simpleclouds.config.preset.ultra_low.description"))
				.setPreset(SimpleCloudsConfig.CLIENT.framesToGenerateMesh, 20)
				.setPreset(SimpleCloudsConfig.CLIENT.levelOfDetail, LevelOfDetailOptions.LOW)
				.setPreset(SimpleCloudsConfig.CLIENT.transparency, false)
				.setPreset(SimpleCloudsConfig.CLIENT.renderStormFog, false)
				.setPreset(SimpleCloudsConfig.CLIENT.atmosphericClouds, false)
				.setPreset(SimpleCloudsConfig.CLIENT.shadowDistance, 1000)
				.setPreset(SimpleCloudsConfig.CLIENT.distantShadows, false).build());
		event.registerPreset(ModConfig.Type.CLIENT, ConfigPreset.builder(Component.translatable("simpleclouds.config.preset.classic_style"))
				.setDescription(Component.translatable("simpleclouds.config.preset.classic_style.description"))
				.setPreset(SimpleCloudsConfig.CLIENT.transparency, false)
				.setPreset(SimpleCloudsConfig.CLIENT.cubeNormals, true)
				.setPreset(SimpleCloudsConfig.CLIENT.shadedClouds, false)
				.setPreset(SimpleCloudsConfig.CLIENT.atmosphericClouds, false).build());
	}
	
	@SubscribeEvent
	public static void registerClientCommands(RegisterClientCommandsEvent event)
	{
		ConfigCommandBuilder.builder(event.getDispatcher(), "simpleclouds").addSpec(ModConfig.Type.CLIENT, SimpleCloudsConfig.CLIENT_SPEC).register();
		ClientCloudCommandHelper.register(event.getDispatcher());
	}
	
	@SubscribeEvent
	public static void onAddConfigOptionToMenu(AddConfigEntryToMenuEvent event)
	{
		if (event.getModId().equals(SimpleCloudsMod.MODID) && event.getType() == ModConfig.Type.CLIENT)
		{
			if (event.isValue(SimpleCloudsConfig.CLIENT.showCloudPreviewerInfoPopup))
				event.setCanceled(true);
			if (ClientCloudManager.isAvailableServerSide())
			{
				if (event.isValue(SimpleCloudsConfig.CLIENT.cloudHeight) 
						|| event.isValue(SimpleCloudsConfig.CLIENT.speedModifier) 
						|| event.isValue(SimpleCloudsConfig.CLIENT.cloudMode) 
						|| event.isValue(SimpleCloudsConfig.CLIENT.singleModeCloudType) 
						|| event.isValue(SimpleCloudsConfig.CLIENT.cloudSeed) 
						|| event.isValue(SimpleCloudsConfig.CLIENT.useSpecificSeed)
						|| event.isValue(SimpleCloudsConfig.CLIENT.whitelistAsBlacklist)
						|| event.isValue(SimpleCloudsConfig.CLIENT.dimensionWhitelist)) 
				{
					event.setCanceled(true);
				}
			}
			if (!SimpleCloudsMod.dhLoaded())
			{
				if (event.isValue(SimpleCloudsConfig.CLIENT.distantShadows) || event.isValue(SimpleCloudsConfig.CLIENT.shadowDistance))
					event.setCanceled(true);
			}
		}
	}
	
	@SubscribeEvent
	public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event)
	{
		CloudManager.get(event.getPlayer().level()).onPlayerJoin(event.getPlayer());
		SimpleCloudsRenderer.getInstance().requestReload();
	}
	
	@SubscribeEvent
	public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event)
	{
		ClientSideCloudTypeManager.getInstance().clearSynced();
	}
	
	@SubscribeEvent
	public static void modifyFog(ViewportEvent.RenderFog event)
	{
		if (event.getMode() == FogMode.FOG_TERRAIN && Minecraft.getInstance().gameRenderer.getMainCamera().getFluidInCamera() == FogType.NONE)
		{
			if (SimpleCloudsConfig.CLIENT.fogMode.get() == FogRenderMode.OFF)
			{
				FogRenderer.setupNoFog();
				return;
			}
			SimpleCloudsRenderer renderer = SimpleCloudsRenderer.getInstance();
			WorldEffects effects = renderer.getWorldEffectsManager();
			float partialTick = (float)event.getPartialTick();
			float storminess = Mth.sqrt(effects.getDarkenFactor(partialTick, 2.0F));
			RenderSystem.setShaderFogStart(RenderSystem.getShaderFogStart() * storminess);
		}
	}
	
	@SubscribeEvent
	public static void modifyFogColor(ViewportEvent.ComputeFogColor event)
	{
		if (SimpleCloudsConfig.CLIENT.fogMode.get() != FogRenderMode.OFF && Minecraft.getInstance().gameRenderer.getMainCamera().getFluidInCamera() == FogType.NONE)
		{
			SimpleCloudsRenderer renderer = SimpleCloudsRenderer.getInstance();
			WorldEffects effects = renderer.getWorldEffectsManager();
			float partialTick = (float)event.getPartialTick();
			float lerp = effects.getDarkenFactor(partialTick);
//			event.setRed(207.0F / 255.0F);
//			event.setGreen(231.0F / 255.0F);
//			event.setBlue(255.0F / 255.0F);
			event.setRed(Mth.lerp(event.getRed(), 0.03F, lerp));
			event.setGreen(Mth.lerp(event.getGreen(), 0.02F, lerp));
			event.setBlue(Mth.lerp(event.getBlue(), 0.0F, lerp));
		}
	}
	
	@SubscribeEvent
	public static void onRenderDebugOverlay(CustomizeGuiOverlayEvent.DebugText event)
	{
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug)
		{
			SimpleCloudsRenderer renderer = SimpleCloudsRenderer.getInstance();
			List<String> text = event.getRight();
			text.add("");
			text.add(ChatFormatting.GREEN + SimpleCloudsMod.MODID + ": " + SimpleCloudsMod.getModVersion());
			text.add(renderer.getClientCloudManagerString());
			if (SimpleCloudsRenderer.canRenderInDimension(mc.level))
			{
				CloudMeshGenerator generator = renderer.getMeshGenerator();
				
				var meshGenResult = generator.getMeshGenStatus();
				CloudMeshGenerator.MeshGenStatus opaqueStatus = meshGenResult.getLeft();
				CloudMeshGenerator.MeshGenStatus transparentStatus = meshGenResult.getRight();
				if (opaqueStatus.isErroneous())
					text.add(ChatFormatting.RED + "MESH ERROR OPAQUE: " + opaqueStatus);
				if (transparentStatus.isErroneous())
					text.add(ChatFormatting.RED + "MESH ERROR TRANSPARENT: " + transparentStatus);
				
				String opaqueGeomInfo = humanReadableByteCountSI(generator.getOpaqueBufferBytesUsed()) + "/" + humanReadableByteCountSI(generator.getOpaqueBufferSize());
				String transparentGeomInfo = humanReadableByteCountSI(generator.getTransparentBufferBytesUsed()) + "/" + humanReadableByteCountSI(generator.getTransparentBufferSize());
				text.add("O: " + opaqueGeomInfo + " | T: " + transparentGeomInfo);
				
				int frames = SimpleCloudsConfig.CLIENT.framesToGenerateMesh.get();
				text.add("Mesh gen frames: " + frames + "; Effective FPS: " + mc.getFps() / frames);
				
				text.add("Frustum culling: " + (SimpleCloudsConfig.CLIENT.frustumCulling.get() ? "ON" : "OFF"));
				
				boolean flag = ClientCloudManager.isAvailableServerSide();
				text.add("Server-side: " + (flag ? ChatFormatting.GREEN : ChatFormatting.RED) + flag);
				
				CloudMode mode = renderer.getSettings().getCurrentCloudMode();
				text.add("Cloud mode: " + mode);
				
				if (generator instanceof SingleRegionCloudMeshGenerator singleGenerator)
				{
					text.add("Fade start: " + singleGenerator.getFadeStart() + "; Fade end: " + singleGenerator.getFadeEnd());
					if (singleGenerator.getCloudType() instanceof CloudType type)
						text.add("Cloud type: " + type.id());
				}
				else if (generator instanceof MultiRegionCloudMeshGenerator multiRegion)
				{
					text.add("Cloud types: " + ClientSideCloudTypeManager.getInstance().getCloudTypes().size());
					int formationCount =  multiRegion.getCloudFormationCount();
					String formationText = "Cloud formations: " + formationCount + "/" + MultiRegionCloudMeshGenerator.MAX_CLOUD_FORMATIONS;
					if (formationCount > MultiRegionCloudMeshGenerator.MAX_CLOUD_FORMATIONS)
						formationText = ChatFormatting.RED + formationText;
					text.add(formationText);
				}
				
				if (mc.level != null)
				{
					CloudManager<ClientLevel> manager = CloudManager.get(mc.level);
					
					text.add("Speed: " + round(manager.getCloudSpeed()) + "; Height: " + manager.getCloudHeight());
					text.add("Scroll XYZ: " + round(manager.getScrollX()) + " / " + round(manager.getScrollY()) + " / " + round(manager.getScrollZ()));
					
					WorldEffects effects = renderer.getWorldEffectsManager();
					CloudType atCamera = effects.getCloudTypeAtCamera();
					if (atCamera != null)
						text.add(atCamera.id().toString());
					else
						text.add("UNKNOWN");
					
					String vanillaWeatherOverrideAppend = manager.shouldUseVanillaWeather() ? " (Vanilla Weather Enabled)" : "";
					text.add("Storminess: " + round(effects.getStorminessAtCamera()) + vanillaWeatherOverrideAppend);
				}
			}
			else
			{
				text.add(ChatFormatting.RED + "Disabled in this dimension");
			}
		}
	}
	
	//https://stackoverflow.com/questions/3758606/how-can-i-convert-byte-size-into-a-human-readable-format-in-java#:~:text=public%20static%20String%20humanReadableByteCountSI,1000.0%2C%20ci.current())%3B%0A%7D
	private static String humanReadableByteCountSI(long bytes)
	{
		if (-1000 < bytes && bytes < 1000)
			return bytes + " B";
		CharacterIterator ci = new StringCharacterIterator("kMGTPE");
		while (bytes <= -999_950 || bytes >= 999_950)
		{
			bytes /= 1000;
			ci.next();
		}
		return String.format("%.1f %cB", bytes / 1000.0, ci.current());
	}
	
	private static float round(float val)
	{
		return (float)Math.round(val * 100.0F) / 100.0F;
	}
}
