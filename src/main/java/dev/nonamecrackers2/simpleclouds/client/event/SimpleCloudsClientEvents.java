package dev.nonamecrackers2.simpleclouds.client.event;

import java.util.List;

import com.google.common.base.Joiner;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.client.gui.CloudPreviewerScreen;
import dev.nonamecrackers2.simpleclouds.client.gui.SimpleCloudsConfigScreen;
import dev.nonamecrackers2.simpleclouds.client.mesh.SingleRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsDebugOverlayRenderer;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.client.shader.compute.ComputeShader;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudMode;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeDataManager;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import nonamecrackers2.crackerslib.client.event.impl.AddConfigEntryToMenuEvent;
import nonamecrackers2.crackerslib.client.event.impl.ConfigMenuButtonEvent;
import nonamecrackers2.crackerslib.client.event.impl.RegisterConfigScreensEvent;
import nonamecrackers2.crackerslib.client.gui.ConfigHomeScreen;
import nonamecrackers2.crackerslib.client.gui.Popup;
import nonamecrackers2.crackerslib.client.gui.title.TextTitle;
import nonamecrackers2.crackerslib.common.command.ConfigCommandBuilder;
import nonamecrackers2.crackerslib.common.config.preset.ConfigPreset;
import nonamecrackers2.crackerslib.common.config.preset.RegisterConfigPresetsEvent;
import nonamecrackers2.crackerslib.common.event.impl.OnConfigOptionSaved;

public class SimpleCloudsClientEvents
{
	public static void registerOverlays(RegisterGuiOverlaysEvent event)
	{
		event.registerBelow(VanillaGuiOverlay.DEBUG_TEXT.id(), "simple_clouds_debug", SimpleCloudsDebugOverlayRenderer::render);
	}
	
	public static void registerReloadListeners(RegisterClientReloadListenersEvent event)
	{
		event.registerReloadListener(CloudTypeDataManager.INSTANCE);
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
				.crackersDefault().build(SimpleCloudsConfigScreen::new)
		).addSpec(ModConfig.Type.CLIENT, SimpleCloudsConfig.CLIENT_SPEC).register();
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
	
	@SubscribeEvent
	public static void onSingleModeCloudTypeChanged(OnConfigOptionSaved<String> event)
	{
		if (event.getConfigOption().equals(SimpleCloudsConfig.CLIENT.singleModeCloudType))
		{
			String type = event.getNewValue();
			ResourceLocation loc = ResourceLocation.tryParse(type);
			var types = CloudTypeDataManager.INSTANCE.getCloudTypes();
			if (loc == null || !types.containsKey(loc))
			{
				Component valid = Component.literal(Joiner.on(", ").join(types.keySet().stream().map(ResourceLocation::toString).iterator())).withStyle(ChatFormatting.YELLOW);
				Popup.createInfoPopup(null, 300, Component.translatable("gui.simpleclouds.unknown_cloud_type.info", loc == null ? type : loc.toString(), valid));
				event.overrideValue(SimpleCloudsConfig.CLIENT.singleModeCloudType.getDefault());
			}
			else
			{
				if (SimpleCloudsRenderer.getInstance().getMeshGenerator() instanceof SingleRegionCloudMeshGenerator generator)
					generator.setCloudType(types.get(loc));
			}
		}
	}
	
	@SubscribeEvent
	public static void onCloudModeChanged(OnConfigOptionSaved<CloudMode> event)
	{
		if (event.didValueChange() && event.getConfigOption().equals(SimpleCloudsConfig.CLIENT.cloudMode))
		{
			Popup.createYesNoPopup(null, () -> {
				Minecraft.getInstance().reloadResourcePacks();
			}, 300, Component.translatable("gui.simpleclouds.requires_reload.info"));
		}
	}
	
	@SubscribeEvent
	public static void registerClientCommands(RegisterClientCommandsEvent event)
	{
		ConfigCommandBuilder.builder(event.getDispatcher(), "simpleclouds").addSpec(ModConfig.Type.CLIENT, SimpleCloudsConfig.CLIENT_SPEC).register();
	}
	
	@SubscribeEvent
	public static void onAddConfigOptionToMenu(AddConfigEntryToMenuEvent event)
	{
		if (event.getModId().equals(SimpleCloudsMod.MODID) && event.getType() == ModConfig.Type.CLIENT)
		{
			if (event.isValue(SimpleCloudsConfig.CLIENT.showCloudPreviewerInfoPopup))
				event.setCanceled(true);
		}
	}
	
	@SubscribeEvent
	public static void onRenderDebugOverlay(CustomizeGuiOverlayEvent.DebugText event)
	{
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.renderDebug)
		{
			List<String> text = event.getRight();
			text.add("");
			text.add(ChatFormatting.GREEN + SimpleCloudsMod.MODID + ": " + SimpleCloudsMod.getModVersion());
			text.add("Triangles: " + SimpleCloudsRenderer.getInstance().getMeshGenerator().getTotalSides() * 2);
			int frames = SimpleCloudsConfig.CLIENT.framesToGenerateMesh.get();
			text.add("Frames to generate mesh: " + SimpleCloudsConfig.CLIENT.framesToGenerateMesh.get());
			text.add("Effective framerate: " + mc.getFps() / frames);
			text.add("Frustum culling: " + (SimpleCloudsConfig.CLIENT.frustumCulling.get() ? "ON" : "OFF"));
		}
	}
}
