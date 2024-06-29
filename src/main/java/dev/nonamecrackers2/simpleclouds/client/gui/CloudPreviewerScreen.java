package dev.nonamecrackers2.simpleclouds.client.gui;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.joml.Matrix4f;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.nonamecrackers2.simpleclouds.client.gui.widget.LayerEditor;
import dev.nonamecrackers2.simpleclouds.client.mesh.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.mesh.SingleRegionCloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudInfo;
import dev.nonamecrackers2.simpleclouds.common.noise.AbstractNoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.ModifiableNoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.NoiseSettings;
import net.minecraft.ChatFormatting;
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
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import nonamecrackers2.crackerslib.client.gui.Screen3D;

public class CloudPreviewerScreen extends Screen3D
{
	private static final SingleRegionCloudMeshGenerator GENERATOR = (SingleRegionCloudMeshGenerator)new SingleRegionCloudMeshGenerator(SimpleCloudsRenderer.DEFAULT, CloudMeshGenerator.getCloudAreaMaxRadius() / 2.0F, CloudMeshGenerator.getCloudAreaMaxRadius()).setTestFacesFacingAway(true);
	private static final int PADDING = 10;
	private static final Component WARNING_TOO_MANY_CUBES = Component.translatable("gui.simpleclouds.cloud_previewer.warning.too_many_cubes").withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true));;
	private static final Component STORMINESS_TITLE = Component.translatable("gui.simpleclouds.cloud_previewer.storminess.title");
	private static final Component STORM_START_TITLE = Component.translatable("gui.simpleclouds.cloud_previewer.storm_start.title");
	private static final Component STORM_FADE_DISTANCE_TITLE = Component.translatable("gui.simpleclouds.cloud_previewer.storm_fade_distance.title");
	private Button addLayer;
	private Button removeLayer;
	private @Nullable Screen prev;
	private final List<ModifiableNoiseSettings> layers;
	private final List<LayerEditor> layerEditors = Lists.newArrayList();
	private int currentLayer;
	private float storminess = 0.6F;
	private float stormStart = 16.0F;
	private float stormFadeDistance = 128.0F;
	private final NoiseSettings previewNoiseSettings = new NoiseSettings()
	{
		@Override
		public float[] packForShader()
		{
			float[] values = new float[] {};
			for (NoiseSettings layer : CloudPreviewerScreen.this.layers)
				values = ArrayUtils.addAll(values, layer.packForShader());
			return values;
		}
		
		@Override
		public int layerCount()
		{
			return CloudPreviewerScreen.this.layers.size();
		}
	};
	private final CloudInfo cloudType = new CloudInfo()
	{
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
			return CloudPreviewerScreen.this.previewNoiseSettings;
		}
	};
	private int toolbarHeight;
	private boolean needsMeshRegen;
	private EditBox storminessBox;
	private EditBox stormStartBox;
	private EditBox stormFadeDistanceBox;
	
	public static void addCloudMeshListener(RegisterClientReloadListenersEvent event)
	{
		event.registerReloadListener((ResourceManagerReloadListener)(manager -> {
			GENERATOR.init(manager);
		}));
	}
	
	public static void destroyMeshGenerator()
	{
		GENERATOR.close();
	}
	
	public CloudPreviewerScreen(Screen prev)
	{
		super(Component.translatable("gui.simpleclouds.cloud_previewer.title"), 0.25F, 5000.0F);
		this.prev = prev;
		this.layers = Lists.newArrayList();
		this.layers.add(new ModifiableNoiseSettings()
				.setParam(AbstractNoiseSettings.Param.HEIGHT, 32.0F)
				.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, 1.0F)
				.setParam(AbstractNoiseSettings.Param.SCALE_X, 30.0F)
				.setParam(AbstractNoiseSettings.Param.SCALE_Y, 30.0F)
				.setParam(AbstractNoiseSettings.Param.SCALE_Z, 30.0F)
				.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, 10.0F)
				.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, 0.0F)
				.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, 1.0F));
		this.layers.add(new ModifiableNoiseSettings()
				.setParam(AbstractNoiseSettings.Param.HEIGHT, 256.0F)
				.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, 0.0F)
				.setParam(AbstractNoiseSettings.Param.SCALE_X, 400.0F)
				.setParam(AbstractNoiseSettings.Param.SCALE_Y, 400.0F)
				.setParam(AbstractNoiseSettings.Param.SCALE_Z, 400.0F)
				.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, 32.0F)
				.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, 0.0F)
				.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, 1.0F));
		this.layers.add(new ModifiableNoiseSettings()
				.setParam(AbstractNoiseSettings.Param.HEIGHT, 256.0F)
				.setParam(AbstractNoiseSettings.Param.VALUE_OFFSET, 0.0F)
				.setParam(AbstractNoiseSettings.Param.SCALE_X, 30.0F)
				.setParam(AbstractNoiseSettings.Param.SCALE_Y, 30.0F)
				.setParam(AbstractNoiseSettings.Param.SCALE_Z, 30.0F)
				.setParam(AbstractNoiseSettings.Param.FADE_DISTANCE, 16.0F)
				.setParam(AbstractNoiseSettings.Param.HEIGHT_OFFSET, 0.0F)
				.setParam(AbstractNoiseSettings.Param.VALUE_SCALE, 0.1F));
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
		GENERATOR.setCloudType(this.cloudType);
		GENERATOR.generateMesh(0.0D, 0.0D, 0.0D, 1.0F, null);
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
					System.out.println("yes");
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
	
	@Override
	protected void init()
	{
		super.init();
	
		this.generateMesh();
		
		GridLayout layersToolbar = new GridLayout().columnSpacing(5);
		GridLayout.RowHelper layersToolbarRow = layersToolbar.createRowHelper(4);
		
		this.addLayer = layersToolbarRow.addChild(Button.builder(Component.literal("+").withStyle(ChatFormatting.GREEN), b -> 
		{
			ModifiableNoiseSettings layer = new ModifiableNoiseSettings();
			this.layers.add(layer);
			this.layerEditors.add(new LayerEditor(layer, this.minecraft, PADDING, PADDING + this.font.lineHeight, Math.max(200, this.width / 4), this.height - PADDING * 2 - this.toolbarHeight - this.font.lineHeight, () -> {
				this.needsMeshRegen = true;
			}));
			this.swapToLayer(this.layers.indexOf(layer));
			this.addLayer.active = this.layers.size() < CloudMeshGenerator.MAX_NOISE_LAYERS;
			this.removeLayer.active = true;
			this.needsMeshRegen = true;
		}).tooltip(Tooltip.create(Component.translatable("gui.simpleclouds.cloud_previewer.button.add_layer.title"))).width(20).build());
		this.addLayer.active = this.layers.size() < CloudMeshGenerator.MAX_NOISE_LAYERS;
		
		this.removeLayer = layersToolbarRow.addChild(Button.builder(Component.literal("-").withStyle(ChatFormatting.RED), b -> 
		{
			this.layers.remove(this.currentLayer);
			this.removeWidget(this.layerEditors.get(this.currentLayer));
			this.layerEditors.remove(this.currentLayer);
			this.swapToLayer(this.currentLayer);
			this.removeLayer.active = !this.layers.isEmpty();
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
		FrameLayout.alignInRectangle(layersToolbar, 10, this.height - height - PADDING, this.width / 2, height + PADDING, 0.0F, 0.5F);
		layersToolbar.visitWidgets(this::addRenderableWidget);
		
//		GridLayout secondaryToolbar = new GridLayout().columnSpacing(5);
//		GridLayout.RowHelper secondaryToolbarRow = secondaryToolbar.createRowHelper(1);
//		
//		Button togglePreview = secondaryToolbarRow.addChild(Button.builder(Component.translatable("gui.simpleclouds.cloud_previewer.button.toggle_preview.title"), b -> 
//		{
//			this.renderer.togglePreview(!this.renderer.previewToggled());
//			if (this.renderer.previewToggled())
//				this.onClose();
//		}).width(100).build());
//		togglePreview.active = this.minecraft.level != null;
//		
//		secondaryToolbar.arrangeElements();
//		FrameLayout.alignInRectangle(secondaryToolbar, this.width / 2, this.height - height - PADDING, this.width / 2 - 5, height + PADDING, 1.0F, 0.5F);
//		secondaryToolbar.visitWidgets(this::addRenderableWidget);
		
		GridLayout cloudTypeOptions = new GridLayout().rowSpacing(this.font.lineHeight + 5);
		GridLayout.RowHelper cloudTypeOptionsRow = cloudTypeOptions.createRowHelper(1);
		this.storminessBox = this.valueEditor(this.storminess, cloudTypeOptionsRow.addChild(new EditBox(this.font, 0, 0, 100, 20, CommonComponents.EMPTY)), f -> this.storminess = f, 0.0F, 1.0F);
		this.stormStartBox = this.valueEditor(this.stormStart, cloudTypeOptionsRow.addChild(new EditBox(this.font, 0, 0, 100, 20, CommonComponents.EMPTY)), f -> this.stormStart = f, 0.0F, CloudMeshGenerator.LOCAL_SIZE * CloudMeshGenerator.WORK_SIZE * CloudMeshGenerator.VERTICAL_CHUNK_SPAN);
		this.stormFadeDistanceBox = this.valueEditor(this.stormFadeDistance, cloudTypeOptionsRow.addChild(new EditBox(this.font, 0, 0, 100, 20, CommonComponents.EMPTY)), f -> this.stormFadeDistance = f, 0.0F, 1600.0F);
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
		this.renderBackground(stack);
		super.render(stack, pMouseX, pMouseY, pPartialTick);
		stack.drawString(this.font, Component.translatable("gui.simpleclouds.cloud_previewer.current_layer", Component.literal(this.layers.isEmpty() ? "NONE" : String.valueOf(this.currentLayer + 1)).withStyle(Style.EMPTY.withBold(true))), 10, 5, 0xFFFFFFFF);
		
		if (GENERATOR.getTotalSides() * CloudMeshGenerator.BYTES_PER_SIDE > CloudMeshGenerator.SIDE_BUFFER_SIZE)
			stack.drawString(this.font, WARNING_TOO_MANY_CUBES, this.width - this.font.width(WARNING_TOO_MANY_CUBES) - 5, this.height - this.font.lineHeight - 5, 0xFFFFFFFF);
		
		stack.drawString(this.font, STORMINESS_TITLE, this.storminessBox.getX(), this.storminessBox.getY() - this.font.lineHeight - 2, 0xFFFFFFFF);
		stack.drawString(this.font, STORM_START_TITLE, this.stormStartBox.getX(), this.stormStartBox.getY() - this.font.lineHeight - 2, 0xFFFFFFFF);
		stack.drawString(this.font, STORM_FADE_DISTANCE_TITLE, this.stormFadeDistanceBox.getX(), this.stormFadeDistanceBox.getY() - this.font.lineHeight - 2, 0xFFFFFFFF);
	}
	
	@Override
	protected void render3D(PoseStack stack, MultiBufferSource buffers, int mouseX, int mouseY, float partialTick)
	{
		if (this.needsMeshRegen)
		{
			this.generateMesh();
			this.needsMeshRegen = false;
		}
		GENERATOR.render(stack, RenderSystem.getProjectionMatrix(), partialTick, 1.0F, 1.0F, 1.0F);
		
		float radius = CloudMeshGenerator.getCloudAreaMaxRadius();
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
