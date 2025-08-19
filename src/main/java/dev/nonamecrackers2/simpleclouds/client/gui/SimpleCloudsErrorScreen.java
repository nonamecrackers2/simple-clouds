package dev.nonamecrackers2.simpleclouds.client.gui;

import java.nio.file.Path;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import dev.nonamecrackers2.simpleclouds.client.mesh.RendererInitializeResult;
import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout.RowHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;


public class SimpleCloudsErrorScreen extends SimpleCloudsInfoScreen
{
	private static final Component DESCRIPTION = Component.translatable("gui.simpleclouds.error_screen.description");
	private final RendererInitializeResult result;
	private final Runnable onClose;
	private Path crashReportsFolder;
	
	public SimpleCloudsErrorScreen(RendererInitializeResult result, Runnable onClose)
	{
		super(Component.translatable("gui.simpleclouds.error_screen.title").withStyle(Style.EMPTY.withUnderlined(true).withBold(true)), 3);
		this.result = result;
		this.onClose = onClose;
	}
	
	@Override
	protected void generateText(List<FormattedCharSequence> text, int maxWidth)
	{
		text.addAll(this.font.split(DESCRIPTION, maxWidth));
		if (!this.result.getErrors().isEmpty())
		{
			RendererInitializeResult.Error error = this.result.getErrors().get(this.result.getErrors().size() - 1);
			text.add(FormattedCharSequence.EMPTY);
			text.addAll(this.font.split(error.text(), maxWidth));
			if (this.result.getErrors().size() > 1)
			{
				text.add(FormattedCharSequence.EMPTY);
				text.addAll(this.font.split(Component.translatable("gui.simpleclouds.error_screen.multiple"), maxWidth));
			}
		}
		else
		{
			text.add(Component.translatable("gui.simpleclouds.error_screen.no_errors").getVisualOrderText());
		}
	}
	
	@Override
	protected void generateButtons(RowHelper row)
	{
		super.generateButtons(row);
		
		Button button = row.addChild(Button.builder(Component.translatable("gui.simpleclouds.error_screen.button.crash_report"), b -> {
			var list = this.result.getSavedCrashReportPaths();
			if (list != null && list.size() == 1)
				Util.getPlatform().openUri(list.get(0).toUri());
			else
				Util.getPlatform().openUri(this.crashReportsFolder.toUri());
		}).width(100).build());
		button.active = this.result.getSavedCrashReportPaths() != null && !this.result.getSavedCrashReportPaths().isEmpty();
	}
	
	@Override
	protected void init()
	{
		this.crashReportsFolder = this.minecraft.gameDirectory.toPath().resolve("crash-reports");
		
		super.init();
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;
		
		if (keyCode == GLFW.GLFW_KEY_R && Screen.hasControlDown())
		{
			this.minecraft.reloadResourcePacks().thenRunAsync(() ->
			{
				var renderer = SimpleCloudsRenderer.getOptionalInstance().orElse(null);
				if (renderer == null)
					return;
				RendererInitializeResult result = renderer.getInitialInitializationResult();
				if (result != null && result.getState() == RendererInitializeResult.State.ERROR)
					this.minecraft.setScreen(new SimpleCloudsErrorScreen(renderer.getInitialInitializationResult(), this.onClose));
				else
					this.onClose();
			}, this.minecraft);
			return true;
		}
		
		return false;
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
	
	@Override
	public void onClose()
	{
		this.onClose.run();
	}
}