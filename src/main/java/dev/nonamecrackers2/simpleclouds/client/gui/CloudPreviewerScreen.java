package dev.nonamecrackers2.simpleclouds.client.gui;

import javax.annotation.Nullable;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.nonamecrackers2.simpleclouds.client.gui.widget.ParametersList;
import dev.nonamecrackers2.simpleclouds.client.renderer.CloudMeshGenerator;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import dev.nonamecrackers2.simpleclouds.common.noise.ModifiableNoiseSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import nonamecrackers2.crackerslib.client.gui.Screen3D;

public class CloudPreviewerScreen extends Screen3D
{
	private static final Component LAYERS = Component.translatable("gui.simpleclouds.cloud_previewer.layers");
	private final SimpleCloudsRenderer renderer = SimpleCloudsRenderer.getInstance();
	private ParametersList parametersList;
	private Button addLayer;
	private Button removeLayer;
	private @Nullable Screen prev;
	
	public CloudPreviewerScreen(Screen prev)
	{
		super(Component.translatable("gui.simpleclouds.cloud_previewer.title"), 0.5F, 3000.0F);
		this.prev = prev;
	}
	
	@Override
	protected void init()
	{
		super.init();
		
		GridLayout layersToolbar = new GridLayout().columnSpacing(5);
		GridLayout.RowHelper layersToolbarRow = layersToolbar.createRowHelper(2);
		
		this.addLayer = layersToolbarRow.addChild(Button.builder(Component.literal("+").withStyle(ChatFormatting.GREEN), b -> 
		{
			this.renderer.getPreviewNoiseSettings().addNoiseLayer(new ModifiableNoiseSettings());
			this.parametersList.buildEntries();
			this.addLayer.active = this.renderer.getPreviewNoiseSettings().layerCount() < CloudMeshGenerator.MAX_NOISE_LAYERS;
		}).tooltip(Tooltip.create(Component.translatable("gui.simpleclouds.cloud_previewer.button.add_layer.title"))).width(20).build());
		this.addLayer.active = this.renderer.getPreviewNoiseSettings().layerCount() < CloudMeshGenerator.MAX_NOISE_LAYERS;
		
		this.removeLayer = layersToolbarRow.addChild(Button.builder(Component.literal("-").withStyle(ChatFormatting.RED), b -> 
		{
			var entry = this.parametersList.getSelected();
			if (entry != null && this.renderer.getPreviewNoiseSettings().removeNoiseLayer(entry.getLayer()))
			{
				this.parametersList.buildEntries();
				this.removeLayer.active = false;
				this.addLayer.active = this.renderer.getPreviewNoiseSettings().layerCount() < CloudMeshGenerator.MAX_NOISE_LAYERS;
			}
		}).tooltip(Tooltip.create(Component.translatable("gui.simpleclouds.cloud_previewer.button.remove_layer.title"))).width(20).build());
		this.removeLayer.active = false;
		
		int padding = 10;
		
		layersToolbar.arrangeElements();
		int height = layersToolbar.getHeight();
		FrameLayout.alignInRectangle(layersToolbar, 10, this.height - height - padding, this.width / 2, height + padding, 0.0F, 0.5F);
		layersToolbar.visitWidgets(this::addRenderableWidget);
		
		GridLayout secondaryToolbar = new GridLayout().columnSpacing(5);
		GridLayout.RowHelper secondaryToolbarRow = secondaryToolbar.createRowHelper(1);
		
		Button togglePreview = secondaryToolbarRow.addChild(Button.builder(Component.translatable("gui.simpleclouds.cloud_previewer.button.toggle_preview.title"), b -> 
		{
			this.renderer.togglePreview(!this.renderer.previewToggled());
			if (this.renderer.previewToggled())
				this.onClose();
		}).width(100).build());
		togglePreview.active = this.minecraft.level != null;
		
		secondaryToolbar.arrangeElements();
		FrameLayout.alignInRectangle(secondaryToolbar, this.width / 2, this.height - height - padding, this.width / 2 - 5, height + padding, 1.0F, 0.5F);
		secondaryToolbar.visitWidgets(this::addRenderableWidget);
		
		this.parametersList = new ParametersList(this.renderer.getPreviewNoiseSettings(), this.minecraft, padding, padding + this.font.lineHeight, this.width / 3, this.height - padding * 2 - height - this.font.lineHeight, () -> {
			this.removeLayer.active = this.parametersList.getSelected() != null;
		});
		this.addRenderableWidget(this.parametersList);
	}
	
	@Override
	public void render(GuiGraphics stack, int pMouseX, int pMouseY, float pPartialTick)
	{
		this.renderBackground(stack);
		super.render(stack, pMouseX, pMouseY, pPartialTick);
		stack.drawString(this.font, LAYERS, 10, 5, 0xFFFFFFFF);
	}
	
	@Override
	protected void render3D(PoseStack stack, MultiBufferSource buffers, int mouseX, int mouseY, float partialTick)
	{
		SimpleCloudsRenderer renderer = SimpleCloudsRenderer.getInstance();
		renderer.generateMesh(this.renderer.getPreviewNoiseSettings(), 0.0D, 0.0D, 0.0D, null);
		renderer.render(stack, RenderSystem.getProjectionMatrix(), partialTick, 1.0F, 1.0F, 1.0F);
		
		float radius = CloudMeshGenerator.getCloudRenderDistance();
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
		builder.vertex(pose, -radius, 0.0F, -radius).color(r, g, b, a).endVertex();
		builder.vertex(pose, radius, 0.0F, -radius).color(r, g, b, a).endVertex();
		builder.vertex(pose, radius, 0.0F, radius).color(r, g, b, a).endVertex();
		builder.vertex(pose, -radius, 0.0F, radius).color(r, g, b, a).endVertex();
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
