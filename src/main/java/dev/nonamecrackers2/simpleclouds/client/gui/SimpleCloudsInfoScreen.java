package dev.nonamecrackers2.simpleclouds.client.gui;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.fml.loading.ImmediateWindowHandler;
import nonamecrackers2.crackerslib.client.util.GUIUtils;

public abstract class SimpleCloudsInfoScreen extends Screen
{
	protected static final int PADDING = 20;
	protected Component openGLVersion;
	protected List<FormattedCharSequence> text;
	protected int totalTextHeight;
	protected final int buttonCount;
	
	protected SimpleCloudsInfoScreen(Component title, int buttonCount)
	{
		super(title);
		this.buttonCount = buttonCount;
	}
	
	protected abstract void generateText(List<FormattedCharSequence> text, int maxWidth);
	
	protected void generateButtons(GridLayout.RowHelper row)
	{
		row.addChild(Button.builder(Component.translatable("gui.crackerslib.screen.config.github"), b -> {
			GUIUtils.openLink("https://github.com/nonamecrackers2/simple-clouds-new/issues");
		}).width(100).build());
		
		row.addChild(Button.builder(Component.translatable("gui.crackerslib.screen.config.discord"), b -> {
			GUIUtils.openLink("https://discord.com/invite/cracker-s-modded-community-987817685293355028");
		}).width(100).build());
	}
	
	@Override
	protected void init()
	{
		this.openGLVersion = Component.literal("OpenGL " + ImmediateWindowHandler.getGLVersion());
		
		this.text = Lists.newArrayList();
		int textMaxWidth = Mth.floor((float)this.width / 1.5F);
		this.generateText(this.text, textMaxWidth);
		this.totalTextHeight = this.text.size() * (this.font.lineHeight + 2);
		
		GridLayout layout = new GridLayout().spacing(10);
		GridLayout.RowHelper row = layout.createRowHelper(this.buttonCount);
		this.generateButtons(row);
		layout.arrangeElements();
		FrameLayout.centerInRectangle(layout, 0, this.height - 40, this.width, 40);
		layout.visitWidgets(this::addRenderableWidget);
	}
	
	@Override
	public void render(GuiGraphics stack, int mouseX, int mouseY, float partialTick)
	{
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
}
