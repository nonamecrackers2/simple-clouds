package dev.nonamecrackers2.simpleclouds.client.event;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;

import org.joml.Math;
import org.joml.Vector3f;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.cloud.ClientSideCloudTypeManager;
import dev.nonamecrackers2.simpleclouds.client.command.ClientCloudCommandHelper;
import dev.nonamecrackers2.simpleclouds.client.gui.CloudPreviewerScreen;
import dev.nonamecrackers2.simpleclouds.client.gui.SimpleCloudsConfigScreen;
import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.mesh.SingleRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsDebugOverlayRenderer;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.renderer.WorldEffects;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.ComputeShader;
import dev.nonamecrackers2.simpleclouds.client.world.ClientCloudManager;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.RegionType;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.registry.SimpleCloudsRegistries;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
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
import nonamecrackers2.crackerslib.client.gui.title.TextTitle;
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
		event.registerReloadListener(ClientSideCloudTypeManager.getInstance().getClientSideDataManager());
		SimpleCloudsRenderer.initialize();
		event.registerReloadListener((ResourceManagerReloadListener)(manager -> {
			ComputeShader.destroyCompiledShaders();
		}));
		event.registerReloadListener(SimpleCloudsRenderer.getInstance());
		CloudPreviewerScreen.addCloudMeshListener(event);
	}
	
	public static void registerConfigMenu(RegisterConfigScreensEvent event)
	{
		event.builder(ConfigHomeScreen.builder(TextTitle.ofModDisplayName(SimpleCloudsMod.MODID))
						.crackersDefault()
						.build(SimpleCloudsConfigScreen::new))
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
		event.registerPreset(ModConfig.Type.CLIENT, ConfigPreset.builder(Component.translatable("simpleclouds.config.preset.optimal_mesh"))
				.setDescription(Component.translatable("simpleclouds.config.preset.optimal_mesh.description"))
				.setPreset(SimpleCloudsConfig.CLIENT.framesToGenerateMesh, 16)
				.setPreset(SimpleCloudsConfig.CLIENT.testSidesThatAreOccluded, true)
				.setPreset(SimpleCloudsConfig.CLIENT.frustumCulling, false).build());
		event.registerPreset(ModConfig.Type.CLIENT, ConfigPreset.builder(Component.translatable("simpleclouds.config.preset.fast_culled_mesh"))
				.setDescription(Component.translatable("simpleclouds.config.preset.fast_culled_mesh.description"))
				.setPreset(SimpleCloudsConfig.CLIENT.framesToGenerateMesh, 4).build());
	}
//	
//	@SubscribeEvent
//	public static void onSingleModeCloudTypeChanged(OnConfigOptionSaved<String> event)
//	{
//		if (event.getConfigOption().equals(SimpleCloudsConfig.CLIENT.singleModeCloudType))
//		{
//			String type = event.getNewValue();
//			ResourceLocation loc = ResourceLocation.tryParse(type);
//			var types = ClientSideCloudTypeManager.getInstance().getCloudTypes();
//			if (loc == null || !types.containsKey(loc))
//			{
//				Component valid = Component.literal(Joiner.on(", ").join(types.keySet().stream().map(ResourceLocation::toString).iterator())).withStyle(ChatFormatting.YELLOW);
//				Popup.createInfoPopup(null, 300, Component.translatable("gui.simpleclouds.unknown_cloud_type.info", loc == null ? type : loc.toString(), valid));
//				event.overrideValue(SimpleCloudsConfig.CLIENT.singleModeCloudType.getDefault());
//			}
//			else
//			{
//				if (SimpleCloudsRenderer.getInstance().getMeshGenerator() instanceof SingleRegionCloudMeshGenerator generator)
//					generator.setCloudType(types.get(loc));
//			}
//		}
//	}
//	
//	@SubscribeEvent
//	public static void onConfigChanged(OnConfigOptionSaved<?> event)
//	{
//		if (event.didValueChange() && (event.getConfigOption().equals(SimpleCloudsConfig.CLIENT.cloudMode) || event.getConfigOption().equals(SimpleCloudsConfig.CLIENT.cloudStyle)))
//		{
//			Popup.createYesNoPopup(null, () -> {
//				Minecraft.getInstance().reloadResourcePacks();
//			}, 300, Component.translatable("gui.simpleclouds.requires_reload.info"));
//		}
//	}
	
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
				if (event.isValue(SimpleCloudsConfig.CLIENT.cloudHeight) || event.isValue(SimpleCloudsConfig.CLIENT.speedModifier) || event.isValue(SimpleCloudsConfig.CLIENT.cloudMode) || event.isValue(SimpleCloudsConfig.CLIENT.singleModeCloudType) || event.isValue(SimpleCloudsConfig.CLIENT.cloudSeed) || event.isValue(SimpleCloudsConfig.CLIENT.useSpecificSeed))
					event.setCanceled(true);
			}
		}
	}
	
	@SubscribeEvent
	public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event)
	{
		ClientSideCloudTypeManager.getInstance().clearCloudTypes();
	}
	
	@SubscribeEvent
	public static void modifyFog(ViewportEvent.RenderFog event)
	{
		if (!SimpleCloudsConfig.CLIENT.renderTerrainFog.get() && event.getMode() == FogMode.FOG_TERRAIN && Minecraft.getInstance().gameRenderer.getMainCamera().getFluidInCamera() == FogType.NONE)
			FogRenderer.setupNoFog();
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
			int totalSides = renderer.getMeshGenerator().getTotalSides();
			text.add("Triangles: " + totalSides * 2 + "; Size: " + humanReadableByteCountSI(totalSides * CloudMeshGenerator.BYTES_PER_SIDE));
			int frames = SimpleCloudsConfig.CLIENT.framesToGenerateMesh.get();
			text.add("Mesh gen frames: " + SimpleCloudsConfig.CLIENT.framesToGenerateMesh.get() + "; Effective FPS: " + mc.getFps() / frames);
			text.add("Frustum culling: " + (SimpleCloudsConfig.CLIENT.frustumCulling.get() ? "ON" : "OFF"));
			boolean flag = ClientCloudManager.isAvailableServerSide();
			text.add("Server-side: " + (flag ? ChatFormatting.GREEN : ChatFormatting.RED) + flag);
			CloudMode mode = renderer.getCloudMode();
			text.add("Cloud mode: " + mode);
			RegionType generator = renderer.getRegionGenerator();
			if (generator != null)
				text.add("Region generator: " + ChatFormatting.GRAY + SimpleCloudsRegistries.getRegionTypeRegistry().getKey(generator));
			else
				text.add("Region generator: NONE");
			if (renderer.getMeshGenerator() instanceof SingleRegionCloudMeshGenerator meshGenerator)
			{
				text.add("Fade start: " + meshGenerator.getFadeStart() + "; Fade end: " + meshGenerator.getFadeEnd());
				if (meshGenerator.getCloudType() instanceof CloudType type)
					text.add("Cloud type: " + type.id());
			}
			else
			{
				text.add("Cloud types: " + ClientSideCloudTypeManager.getInstance().getCloudTypes().size());
			}
			if (mc.level != null)
			{
				CloudManager<ClientLevel> manager = CloudManager.get(mc.level);
				text.add("Speed: " + round(manager.getSpeed()) + "; Height: " + manager.getCloudHeight());
				text.add("Scroll XYZ: " + round(manager.getScrollX()) + " / " + round(manager.getScrollY()) + " / " + round(manager.getScrollZ()));
				Vector3f d = manager.getDirection();
				text.add("Direction XYZ: " + round(d.x) + " / " + round(d.y) + " / " + round(d.z));
				WorldEffects effects = renderer.getWorldEffectsManager();
				if (effects.getCloudTypeAtCamera() != null)
					text.add(effects.getCloudTypeAtCamera().id().toString());
				else
					text.add("UNKNOWN");
				String vanillaWeatherOverrideAppend = manager.shouldUseVanillaWeather() ? " (Vanilla Weather Enabled)" : "";
				text.add("Storminess: " + round(effects.getStorminessAtCamera()) + vanillaWeatherOverrideAppend);
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
