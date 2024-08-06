package dev.nonamecrackers2.simpleclouds.client.gui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.nonamecrackers2.simpleclouds.client.cloud.ClientSideCloudTypeManager;
import dev.nonamecrackers2.simpleclouds.client.gui.widget.LayerEditor;
import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.mesh.CloudStyle;
import dev.nonamecrackers2.simpleclouds.client.mesh.LevelOfDetailOptions;
import dev.nonamecrackers2.simpleclouds.client.mesh.SingleRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudInfo;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.weather.WeatherType;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.noise.AbstractLayeredNoise;
import dev.nonamecrackers2.simpleclouds.common.noise.AbstractNoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.ModifiableLayeredNoise;
import dev.nonamecrackers2.simpleclouds.common.noise.ModifiableNoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.NoiseSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import nonamecrackers2.crackerslib.client.gui.Popup;
import nonamecrackers2.crackerslib.client.gui.Screen3D;
import nonamecrackers2.crackerslib.client.gui.widget.CyclableButton;

public class CloudPreviewerScreen extends Screen3D
{
	private static @Nullable SingleRegionCloudMeshGenerator generator;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Logger LOGGER = LogManager.getLogger();
	private static final int PADDING = 10;
	private static final Component WARNING_TOO_MANY_CUBES = Component.translatable("gui.simpleclouds.cloud_previewer.warning.too_many_cubes").withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true));;
	private static final Component WEATHER_TYPE_TITLE = Component.translatable("gui.simpleclouds.cloud_previewer.weather_type.title");
	private static final Component STORMINESS_TITLE = Component.translatable("gui.simpleclouds.cloud_previewer.storminess.title");
	private static final Component STORM_START_TITLE = Component.translatable("gui.simpleclouds.cloud_previewer.storm_start.title");
	private static final Component STORM_FADE_DISTANCE_TITLE = Component.translatable("gui.simpleclouds.cloud_previewer.storm_fade_distance.title");
	private static final Component LOAD = Component.translatable("gui.simpleclouds.cloud_previewer.load.title");
	private static final Component EXPORT = Component.translatable("gui.simpleclouds.cloud_previewer.export.title");
	private static final Component SELECT_A_CLOUD_TYPE = Component.translatable("gui.simpleclouds.cloud_previewer.popup.select.cloud_type");
	private static final Component EXPORT_CLOUD_TYPE = Component.translatable("gui.simpleclouds.cloud_previewer.popup.export.cloud_type");
	private static final Component FILE_ALREADY_EXISTS = Component.translatable("gui.simpleclouds.cloud_previewer.popup.export.exists");
	private static final Component INFO = Component.translatable("gui.simpleclouds.cloud_previewer.info");
	private Button addLayer;
	private Button removeLayer;
	private @Nullable Screen prev;
	private final List<ModifiableNoiseSettings> layers;
	private final List<LayerEditor> layerEditors = Lists.newArrayList();
	private int currentLayer;
	private WeatherType weatherType = WeatherType.NONE;
	private float storminess = 0.0F;
	private float stormStart = 16.0F;
	private float stormFadeDistance = 32.0F;
	private final CloudInfo cloudType = new CloudInfo()
	{
		@Override
		public WeatherType weatherType() 
		{
			return CloudPreviewerScreen.this.weatherType;
		}
		
		@Override
		public float storminess()
		{
			return CloudPreviewerScreen.this.storminess;
		}
		
		@Override
		public float stormStart()
		{
			return CloudPreviewerScreen.this.stormStart;
		}
		
		@Override
		public float stormFadeDistance()
		{
			return CloudPreviewerScreen.this.stormFadeDistance;
		}
		
		@Override
		public NoiseSettings noiseConfig()
		{
			if (CloudPreviewerScreen.this.layers.isEmpty())
				return NoiseSettings.EMPTY;
			else if (CloudPreviewerScreen.this.layers.size() > 1)
				return new ModifiableLayeredNoise(CloudPreviewerScreen.this.layers);
			else
				return CloudPreviewerScreen.this.layers.get(0);
		}
	};
	private final File directory;
	private int toolbarHeight;
	private boolean needsMeshRegen = true;
	private CyclableButton<WeatherType> weatherTypeButton;
	private EditBox storminessBox;
	private EditBox stormStartBox;
	private EditBox stormFadeDistanceBox;
	private Button exportButton;
	
	public static void addCloudMeshListener(RegisterClientReloadListenersEvent event)
	{
		event.registerReloadListener((ResourceManagerReloadListener)(manager ->
		{
			if (generator != null)
				generator.init(manager);
		}));
	}
	
	public static void destroyMeshGenerator()
	{
		if (generator != null)
			generator.close();
	}
	
	public CloudPreviewerScreen(Screen prev)
	{
		super(Component.translatable("gui.simpleclouds.cloud_previewer.title"), 0.25F, 5000.0F);
		if (generator == null)
		{
			generator = (SingleRegionCloudMeshGenerator)new SingleRegionCloudMeshGenerator(SimpleCloudsRenderer.FALLBACK, LevelOfDetailOptions.HIGH.getConfig(), 3, 0.5F, 1.0F, CloudStyle.DEFAULT).setTestFacesFacingAway(true);
			generator.init(Minecraft.getInstance().getResourceManager());
		}
		this.prev = prev;
		this.layers = Lists.newArrayList();
		this.layers.add(new ModifiableNoiseSettings());
		this.directory = new File(Minecraft.getInstance().gameDirectory, "simpleclouds/cloud_types");
		if (!this.directory.exists())
			this.directory.mkdirs();
	}
	
	private void swapToLayer(int index)
	{
		if (!this.layers.isEmpty())
		{
			if (index >= this.layers.size())
				index = 0;
			else if (index < 0)
				index = this.layers.size() - 1;
			if (this.currentLayer >= 0 && this.currentLayer < this.layerEditors.size())
				this.removeWidget(this.layerEditors.get(this.currentLayer));
			this.currentLayer = index;
			this.addRenderableWidget(this.layerEditors.get(this.currentLayer));
		}
	}
	
	private void jumpLayer(int jump)
	{
		this.swapToLayer(this.currentLayer + jump);
	}
	
	private void generateMesh()
	{
		generator.setCloudType(this.cloudType);
		while (generator.tick(0.0D, 0.0D, 0.0D, 1.0F, null)) {}
		this.needsMeshRegen = false;
	}
	
	private EditBox valueEditor(float currentValue, EditBox box, Consumer<Float> valueSetter, float min, float max)
	{
		box.setValue(String.valueOf(currentValue));
		box.setResponder(s -> {
			try
			{
				float parsed = Float.parseFloat(s);
				if (parsed < min || parsed > max)
				{
					valueSetter.accept(Mth.clamp(parsed, min, max));
					box.setTextColor(ChatFormatting.RED.getColor());
				}
				else
				{
					valueSetter.accept(Float.parseFloat(s));
					box.setTextColor(0xFFFFFFFF);
				}
			}
			catch (NumberFormatException e)
			{
				box.setTextColor(ChatFormatting.RED.getColor());
			}
			this.needsMeshRegen = true;
		});
		return box;
	}
	
	private void loadCloudType()
	{
		Popup.<CloudType>createOptionListPopup(this, list -> 
		{
			for (var entry : ClientSideCloudTypeManager.getInstance().getCloudTypes().entrySet())
			{
				CloudType type = entry.getValue();
				if (type.noiseConfig() instanceof AbstractNoiseSettings || type.noiseConfig() instanceof AbstractLayeredNoise)
				{
					MutableComponent name = Component.literal(entry.getKey().toString());
					if (!ClientSideCloudTypeManager.getInstance().getClientSideDataManager().getCloudTypes().containsKey(entry.getKey()))
						name.append(Component.literal(" (Server Side)").withStyle(ChatFormatting.DARK_GRAY));
					list.addObject(name, type);
				}
			}
		}, type -> 
		{
			this.clearAllLayers();
			if (type.noiseConfig() instanceof AbstractNoiseSettings<?> settings)
			{
				this.addLayer(new ModifiableNoiseSettings(settings));
			}
			else if (type.noiseConfig() instanceof AbstractLayeredNoise<?> layeredSettings)
			{
				for (AbstractNoiseSettings<?> settings : layeredSettings.getNoiseLayers())
					this.addLayer(new ModifiableNoiseSettings(settings));
			}
			this.weatherType = type.weatherType();
			this.weatherTypeButton.setValue(this.weatherType);
			this.storminess = type.storminess();
			this.storminessBox.setValue(String.valueOf(this.storminess));
			this.stormStart = type.stormStart();
			this.stormStartBox.setValue(String.valueOf(this.stormStart));
			this.stormFadeDistance = type.stormFadeDistance();
			this.stormFadeDistanceBox.setValue(String.valueOf(this.stormFadeDistance));
		}, 400, 100, SELECT_A_CLOUD_TYPE);
	}
	
	private void attemptToExportCloudType()
	{
		Popup.createTextFieldPopup(this, value -> {
			File file = new File(this.directory, value.replaceAll("[^a-zA-Z0-9\\.\\-]", "_") + ".json");
			if (!file.exists())
			{
				this.exportCloudType(file);
				Popup.createInfoPopup(this, 200, Component.translatable("gui.simpleclouds.cloud_previewer.popup.exported.cloud_type", file.getAbsolutePath()));
			}
			else
			{
				Popup.createYesNoPopup(this, () -> {
					this.exportCloudType(file);
					Popup.createInfoPopup(this, 200, Component.translatable("gui.simpleclouds.cloud_previewer.popup.exported.cloud_type", file.getAbsolutePath()));
				}, this::attemptToExportCloudType, 200, FILE_ALREADY_EXISTS);
			}
		}, 200, EXPORT_CLOUD_TYPE);
	}
	
	private void exportCloudType(File file)
	{
		try (FileWriter writer = new FileWriter(file))
		{
			GSON.toJson(this.cloudType.toJson(), writer);
		}
		catch (JsonIOException | IOException e)
		{
			LOGGER.error("Failed to export cloud type json file", e);
		}
		catch (JsonSyntaxException | IllegalStateException e)
		{
			LOGGER.error("An internal error occured while serializing cloud type", e);
		}
	}
	
	private void addLayer(ModifiableNoiseSettings layer)
	{
		this.layers.add(layer);
		this.layerEditors.add(new LayerEditor(layer, this.minecraft, PADDING, PADDING + this.font.lineHeight, Math.max(200, this.width / 4), this.height - PADDING * 2 - this.toolbarHeight - this.font.lineHeight, () -> {
			this.needsMeshRegen = true;
		}));
		this.swapToLayer(this.layers.indexOf(layer));
		this.addLayer.active = this.layers.size() < CloudMeshGenerator.MAX_NOISE_LAYERS;
		this.removeLayer.active = true;
		this.exportButton.active = true;
		this.needsMeshRegen = true;
	}
	
	private void clearAllLayers()
	{
		var layerEditorIterator = this.layerEditors.iterator();
		while (layerEditorIterator.hasNext())
		{
			this.removeWidget(layerEditorIterator.next());
			layerEditorIterator.remove();
		}
		this.layers.clear();
		this.addLayer.active = true;
		this.removeLayer.active = false;
		this.exportButton.active = false;
		this.needsMeshRegen = true;
	}
	
	@Override
	protected void init()
	{
		super.init();
	
		GridLayout layersToolbar = new GridLayout().columnSpacing(5);
		GridLayout.RowHelper layersToolbarRow = layersToolbar.createRowHelper(4);
		
		this.addLayer = layersToolbarRow.addChild(Button.builder(Component.literal("+").withStyle(ChatFormatting.GREEN), b -> {
			this.addLayer(new ModifiableNoiseSettings());
		}).tooltip(Tooltip.create(Component.translatable("gui.simpleclouds.cloud_previewer.button.add_layer.title"))).width(20).build());
		this.addLayer.active = this.layers.size() < CloudMeshGenerator.MAX_NOISE_LAYERS;
		
		this.removeLayer = layersToolbarRow.addChild(Button.builder(Component.literal("-").withStyle(ChatFormatting.RED), b -> 
		{
			this.layers.remove(this.currentLayer);
			this.removeWidget(this.layerEditors.get(this.currentLayer));
			this.layerEditors.remove(this.currentLayer);
			this.swapToLayer(this.currentLayer);
			this.removeLayer.active = !this.layers.isEmpty();
			this.exportButton.active = !this.layers.isEmpty();
			this.addLayer.active = true;
			this.needsMeshRegen = true;
		}).tooltip(Tooltip.create(Component.translatable("gui.simpleclouds.cloud_previewer.button.remove_layer.title"))).width(20).build());
		this.removeLayer.active = !this.layers.isEmpty();
		
		layersToolbarRow.addChild(Button.builder(Component.literal("<"), b -> {
			this.jumpLayer(-1);
		}).tooltip(Tooltip.create(Component.translatable("gui.simpleclouds.cloud_previewer.button.previous_layer.title"))).width(20).build());
		
		layersToolbarRow.addChild(Button.builder(Component.literal(">"), b -> {
			this.jumpLayer(1);
		}).tooltip(Tooltip.create(Component.translatable("gui.simpleclouds.cloud_previewer.button.next_layer.title"))).width(20).build());
		
		layersToolbar.arrangeElements();
		int height = layersToolbar.getHeight();
		this.toolbarHeight = height;
		FrameLayout.alignInRectangle(layersToolbar, PADDING, this.height - height - PADDING, this.width / 2, height + PADDING, 0.0F, 0.5F);
		layersToolbar.visitWidgets(this::addRenderableWidget);
		
		GridLayout secondaryToolbar = new GridLayout().spacing(5);
		GridLayout.RowHelper secondaryToolbarRow = secondaryToolbar.createRowHelper(1);
		
		secondaryToolbarRow.addChild(Button.builder(LOAD, b -> {
			this.loadCloudType();
		}).size(100, 20).build());
		
		this.exportButton = secondaryToolbarRow.addChild(Button.builder(EXPORT, b -> {
			this.attemptToExportCloudType();
		}).size(100, 20).build());
		this.exportButton.active = !this.layers.isEmpty();
		
		secondaryToolbar.arrangeElements();
		FrameLayout.alignInRectangle(secondaryToolbar, PADDING, PADDING, this.width - PADDING * 2, this.height - PADDING * 2, 1.0F, 1.0F);
		secondaryToolbar.visitWidgets(this::addRenderableWidget);
		
		GridLayout cloudTypeOptions = new GridLayout().rowSpacing(this.font.lineHeight + 5);
		GridLayout.RowHelper cloudTypeOptionsRow = cloudTypeOptions.createRowHelper(1);
		this.weatherTypeButton = cloudTypeOptionsRow.addChild(new CyclableButton<>(0, 0, 100, Lists.newArrayList(WeatherType.values()), this.weatherType));
		this.weatherTypeButton.setResponder(type -> this.weatherType = type);
		this.storminessBox = this.valueEditor(this.storminess, cloudTypeOptionsRow.addChild(new EditBox(this.font, 0, 0, 100, 20, CommonComponents.EMPTY)), f -> this.storminess = f, 0.0F, CloudInfo.STORMINESS_MAX);
		this.stormStartBox = this.valueEditor(this.stormStart, cloudTypeOptionsRow.addChild(new EditBox(this.font, 0, 0, 100, 20, CommonComponents.EMPTY)), f -> this.stormStart = f, 0.0F, CloudInfo.STORM_START_MAX);
		this.stormFadeDistanceBox = this.valueEditor(this.stormFadeDistance, cloudTypeOptionsRow.addChild(new EditBox(this.font, 0, 0, 100, 20, CommonComponents.EMPTY)), f -> this.stormFadeDistance = f, 0.0F, CloudInfo.STORM_FADE_DISTANCE_MAX);
		cloudTypeOptions.arrangeElements();
		FrameLayout.alignInRectangle(cloudTypeOptions, 10, 10 + this.font.lineHeight, this.width - 20, this.height - 20 - this.font.lineHeight * 2, 1.0F, 0.0F);
		cloudTypeOptions.visitWidgets(this::addRenderableWidget);
		
		this.layerEditors.clear();
		for (int i = 0; i < this.layers.size(); i++)
		{
			ModifiableNoiseSettings layer = this.layers.get(i);
			LayerEditor list = new LayerEditor(layer, this.minecraft, PADDING, PADDING + this.font.lineHeight, Math.max(200, this.width / 4), this.height - PADDING * 2 - height - this.font.lineHeight, () -> {
				this.needsMeshRegen = true;
			});
			if (i == this.currentLayer)
				this.addRenderableWidget(list);
			this.layerEditors.add(list);
		}
	}
	
	@Override
	public void render(GuiGraphics stack, int pMouseX, int pMouseY, float pPartialTick)
	{
		if (SimpleCloudsConfig.CLIENT.showCloudPreviewerInfoPopup.get())
		{
			Popup.createInfoPopup(this, 200, INFO);
			SimpleCloudsConfig.CLIENT.showCloudPreviewerInfoPopup.set(false);
		}
		
		this.renderBackground(stack);
		super.render(stack, pMouseX, pMouseY, pPartialTick);
		stack.drawString(this.font, Component.translatable("gui.simpleclouds.cloud_previewer.current_layer", Component.literal(this.layers.isEmpty() ? "NONE" : String.valueOf(this.currentLayer + 1)).withStyle(Style.EMPTY.withBold(true))), 10, 5, 0xFFFFFFFF);
		
		if (generator.getTotalSides() * CloudMeshGenerator.BYTES_PER_SIDE > CloudMeshGenerator.SIDE_BUFFER_SIZE)
			stack.drawString(this.font, WARNING_TOO_MANY_CUBES, this.width - this.font.width(WARNING_TOO_MANY_CUBES) - 5, this.height - this.font.lineHeight - 5, 0xFFFFFFFF);
		
		stack.drawString(this.font, WEATHER_TYPE_TITLE, this.weatherTypeButton.getX(), this.weatherTypeButton.getY() - this.font.lineHeight - 2, 0xFFFFFFFF);
		stack.drawString(this.font, STORMINESS_TITLE, this.storminessBox.getX(), this.storminessBox.getY() - this.font.lineHeight - 2, 0xFFFFFFFF);
		stack.drawString(this.font, STORM_START_TITLE, this.stormStartBox.getX(), this.stormStartBox.getY() - this.font.lineHeight - 2, 0xFFFFFFFF);
		stack.drawString(this.font, STORM_FADE_DISTANCE_TITLE, this.stormFadeDistanceBox.getX(), this.stormFadeDistanceBox.getY() - this.font.lineHeight - 2, 0xFFFFFFFF);
	}
	
	@Override
	protected void render3D(PoseStack stack, MultiBufferSource buffers, int mouseX, int mouseY, float partialTick)
	{
		if (this.needsMeshRegen)
			this.generateMesh();
		generator.render(stack, RenderSystem.getProjectionMatrix(), partialTick, 1.0F, 1.0F, 1.0F);
		
		float radius = generator.getCloudAreaMaxRadius();
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder builder = tesselator.getBuilder();
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f pose = stack.last().pose();
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		float r = 0.3F, g = 0.3F, b = 0.3F, a = 0.5F;
		builder.vertex(pose, -radius, -32.0F, -radius).color(r, g, b, a).endVertex();
		builder.vertex(pose, radius, -32.0F, -radius).color(r, g, b, a).endVertex();
		builder.vertex(pose, radius, -32.0F, radius).color(r, g, b, a).endVertex();
		builder.vertex(pose, -radius, -32.0F, radius).color(r, g, b, a).endVertex();
		tesselator.end();
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}
	
	@Override
	public void onClose()
	{
		if (this.prev != null)
			this.minecraft.setScreen(this.prev);
		else
			super.onClose();
	}
}
