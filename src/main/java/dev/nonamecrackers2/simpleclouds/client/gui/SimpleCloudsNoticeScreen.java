package dev.nonamecrackers2.simpleclouds.client.gui;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public class SimpleCloudsNoticeScreen extends SimpleCloudsInfoScreen
{
	private final Component text;
	private @Nullable Runnable onClose;
	
	public SimpleCloudsNoticeScreen(Component text)
	{
		super(Component.translatable("gui.simpleclouds.notice.title").withStyle(Style.EMPTY.withUnderlined(true).withBold(true)), 3);
		this.text = text;
	}
	
	public void setOnClose(Runnable onClose)
	{
		this.onClose = onClose;
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
	
	@Override
	public void onClose()
	{
		if (this.onClose != null)
			this.onClose.run();
		else
			super.onClose();
	}
}
