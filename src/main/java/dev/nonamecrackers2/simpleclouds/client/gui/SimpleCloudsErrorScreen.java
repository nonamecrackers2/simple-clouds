package dev.nonamecrackers2.simpleclouds.client.gui;

import java.nio.file.Path;

import org.lwjgl.glfw.GLFW;

import dev.nonamecrackers2.simpleclouds.client.mesh.GeneratorInitializeResult;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.neoforged.fml.loading.ImmediateWindowHandler;
import nonamecrackers2.crackerslib.client.util.GUIUtils;

public class SimpleCloudsErrorScreen extends Screen
{
	private static final int PADDING = 20;
	private static final Component DESCRIPTION = Component.translatable("gui.simpleclouds.error_screen.description");
	private final GeneratorInitializeResult result;
	private Path crashReportsFolder;
	private Component openGLVersion;
	
	public SimpleCloudsErrorScreen(GeneratorInitializeResult result)
	{
		super(Component.translatable("gui.simpleclouds.error_screen.title").withStyle(Style.EMPTY.withUnderlined(true).withBold(true)));
		this.result = result;
	}
	
	@Override
	protected void init()
	{
		this.openGLVersion = Component.literal("OpenGL " + ImmediateWindowHandler.getGLVersion());
		this.crashReportsFolder = this.minecraft.gameDirectory.toPath().resolve("crash-reports");
		
		MutableComponent text = DESCRIPTION.copy();
		text.append("\n\n");
		if (!this.result.getErrors().isEmpty())
		{
			GeneratorInitializeResult.Error error = this.result.getErrors().get(this.result.getErrors().size() - 1);
			text.append(error.text());
			if (this.result.getErrors().size() > 1)
			{
				text.append("\n\n");
				text.append(Component.translatable("gui.simpleclouds.error_screen.multiple"));
			}
		}
		else
		{
			text.append(Component.translatable("gui.simpleclouds.error_screen.no_errors"));
		}
		
		
		var textWidget = this.addRenderableWidget(new FocusableTextWidget(Math.min(this.width, 400), text, this.font, 20));
		textWidget.setCentered(true);
        textWidget.setPosition(this.width / 2 - textWidget.getWidth() / 2, this.height / 2 - textWidget.getHeight() / 2);
        this.setInitialFocus(textWidget);
		
		GridLayout layout = new GridLayout().spacing(10);
		GridLayout.RowHelper row = layout.createRowHelper(3);
		
		row.addChild(Button.builder(Component.translatable("gui.crackerslib.screen.config.github"), b -> {
			GUIUtils.openLink("https://github.com/nonamecrackers2/simple-clouds-new/issues");
		}).width(100).build());
		
		row.addChild(Button.builder(Component.translatable("gui.crackerslib.screen.config.discord"), b -> {
			GUIUtils.openLink("https://discord.com/invite/cracker-s-modded-community-987817685293355028");
		}).width(100).build());
		
		Button button = row.addChild(Button.builder(Component.translatable("gui.simpleclouds.error_screen.button.crash_report"), b -> {
			var list = this.result.getSavedCrashReportPaths();
			if (list != null && list.size() == 1)
				Util.getPlatform().openUri(list.get(0).toUri());
			else
				Util.getPlatform().openUri(this.crashReportsFolder.toUri());
		}).width(100).build());
		button.active = this.result.getSavedCrashReportPaths() != null && !this.result.getSavedCrashReportPaths().isEmpty();
		
		layout.arrangeElements();
		FrameLayout.centerInRectangle(layout, 0, this.height - 40, this.width, 40);
		layout.visitWidgets(this::addRenderableWidget);
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if (super.keyPressed(keyCode, scanCode, modifiers))
			return true;
		
		if (keyCode == GLFW.GLFW_KEY_R && Screen.hasControlDown())
		{
			this.minecraft.reloadResourcePacks();
			return true;
		}
		
		return false;
	}
	
	@Override
	public void render(GuiGraphics stack, int mouseX, int mouseY, float partialTick)
	{
		super.render(stack, mouseX, mouseY, partialTick);
		
		stack.drawCenteredString(this.font, this.getTitle(), this.width / 2, PADDING, 0xFFFFFFFF);
		stack.drawString(this.font, this.openGLVersion, PADDING, PADDING, 0xFFFFFFFF);
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
	
	@Override
	public void onClose() {}
}