package dev.nonamecrackers2.simpleclouds.client.gui;

import java.util.List;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public class SimpleCloudsNoticeScreen extends SimpleCloudsInfoScreen
{
	private final Component text;
	
	public SimpleCloudsNoticeScreen(Component text)
	{
		super(Component.translatable("gui.simpleclouds.notice.title").withStyle(Style.EMPTY.withUnderlined(true).withBold(true)), 3);
		this.text = text;
	}
	
	@Override
	protected void generateButtons(GridLayout.RowHelper row)
	{
		super.generateButtons(row);
		
		row.addChild(Button.builder(Component.translatable("gui.simpleclouds.notice.close.title"), b -> {
			this.onClose();
		}).width(100).build());
	}

	@Override
	protected void generateText(List<FormattedCharSequence> text, int maxWidth)
	{
		text.addAll(this.font.split(this.text, maxWidth));
	}
}
