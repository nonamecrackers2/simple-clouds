package dev.nonamecrackers2.simpleclouds.client.gui;

import java.nio.file.Path;
import java.util.List;

import com.google.common.collect.Lists;

import dev.nonamecrackers2.simpleclouds.client.mesh.GeneratorInitializeResult;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.loading.ImmediateWindowHandler;
import nonamecrackers2.crackerslib.client.util.GUIUtils;

public class SimpleCloudsErrorScreen extends Screen
{
	private static final int PADDING = 20;
	private static final Component DESCRIPTION = Component.translatable("gui.simpleclouds.error_screen.description");
	private final GeneratorInitializeResult result;
	private Path crashReportsFolder;
	private Component openGLVersion;
	private List<FormattedCharSequence> text;
	private int totalTextHeight;
	
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
		
		this.text = Lists.newArrayList();
		int textMaxWidth = Mth.floor((float)this.width / 1.5F);
		this.text.addAll(this.font.split(DESCRIPTION, textMaxWidth));
		if (!this.result.getErrors().isEmpty())
		{
			GeneratorInitializeResult.Error error = this.result.getErrors().get(this.result.getErrors().size() - 1);
			this.text.add(FormattedCharSequence.EMPTY);
			this.text.addAll(this.font.split(error.text(), textMaxWidth));
			if (this.result.getErrors().size() > 1)
			{
				this.text.add(FormattedCharSequence.EMPTY);
				this.text.addAll(this.font.split(Component.translatable("gui.simpleclouds.error_screen.multiple"), textMaxWidth));
			}
		}
		else
		{
			this.text.add(Component.translatable("gui.simpleclouds.error_screen.no_errors").getVisualOrderText());
		}
		
		this.totalTextHeight = this.text.size() * (this.font.lineHeight + 2);
		
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
	public void render(GuiGraphics stack, int mouseX, int mouseY, float partialTick)
	{
		this.renderBackground(stack);
		
		super.render(stack, mouseX, mouseY, partialTick);
		
		stack.drawCenteredString(this.font, this.getTitle(), this.width / 2, PADDING, 0xFFFFFFFF);
		stack.drawString(this.font, this.openGLVersion, PADDING, PADDING, 0xFFFFFFFF);
		
		int top = PADDING + this.font.lineHeight;
		int y = top + (this.height - top - 40) / 2 - this.totalTextHeight / 2;
		
		for (FormattedCharSequence text : this.text)
		{
			stack.drawCenteredString(this.font, text, this.width / 2, y, 0xFFFFFFFF);
			y += this.font.lineHeight + 2;
		}
	}
	
	@Override
	public boolean shouldCloseOnEsc()
	{
		return false;
	}
	
	@Override
	public void onClose() {}
}