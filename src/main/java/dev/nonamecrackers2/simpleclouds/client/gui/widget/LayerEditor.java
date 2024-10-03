package dev.nonamecrackers2.simpleclouds.client.gui.widget;

import java.util.List;
import java.util.Objects;

import com.google.common.collect.Lists;

import dev.nonamecrackers2.simpleclouds.common.noise.AbstractNoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.ModifiableNoiseSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import nonamecrackers2.crackerslib.client.gui.widget.config.ConfigListItem;
import nonamecrackers2.crackerslib.client.util.RenderUtil;

public class LayerEditor extends ContainerObjectSelectionList<LayerEditor.Entry>
{
	private static final int ROW_HEIGHT = 30;
	private final ModifiableNoiseSettings settings;
	private final Runnable onChanged;
	
	public LayerEditor(ModifiableNoiseSettings settings, Minecraft mc, int width, int height, int headerHeight, Runnable onChanged)
	{
		super(mc, width, height, headerHeight, ROW_HEIGHT);
		this.settings = settings;
		this.onChanged = onChanged;
		this.buildEntries();
	}
	
	public void buildEntries()
	{
		this.clearEntries();
		for (AbstractNoiseSettings.Param parameter : AbstractNoiseSettings.Param.values())
			this.addEntry(new LayerEditor.Entry(parameter));
	}
	
	@Override
	public int getRowWidth()
	{
		return this.getWidth() - 5;
	}
	
	@Override
	protected int getScrollbarPosition()
	{
		return this.getX() + this.getWidth() - 5;
	}
	
	@Override
	protected boolean isSelectedItem(int index)
	{
		return Objects.equals(this.getSelected(), this.children().get(index));
	}
	
	public class Entry extends ContainerObjectSelectionList.Entry<LayerEditor.Entry>
	{
		private static final int TEXT_WIDTH = 40;
		private final List<AbstractWidget> widgets = Lists.newArrayList();
		private final AbstractNoiseSettings.Param parameter;
		private final Font font;
		private final Component name;
		private final List<Component> tooltip; 
		private final EditBox box;
		private int prevLeft;
		private int prevTop;
		
		public Entry(AbstractNoiseSettings.Param parameter)
		{
			this.font = LayerEditor.this.minecraft.font;
			this.parameter = parameter;
			var fullName = Component.translatable("gui.simpleclouds.noise_settings.param." + parameter.toString().toLowerCase() + ".name");
			var squishedName = ConfigListItem.shortenText(fullName, TEXT_WIDTH);
			var box = new EditBox(this.font, 0, 0, 60, 20, CommonComponents.EMPTY);
			box.setValue(String.valueOf(LayerEditor.this.settings.getParam(parameter)));
			box.setResponder(v -> 
			{
				try 
				{
					float value = Float.parseFloat(v);
					if (value < parameter.getMinInclusive() || parameter.getMaxInclusive() < value)
						box.setTextColor(ChatFormatting.RED.getColor());
					else
						box.setTextColor(0xFFFFFFFF);
					LayerEditor.this.settings.setParam(parameter, value);
				} 
				catch (NumberFormatException e)
				{
					box.setTextColor(ChatFormatting.RED.getColor());
					return;
				}
				LayerEditor.this.onChanged.run();
			});
			this.name = squishedName;
			this.tooltip = Lists.newArrayList(fullName, Component.translatable("gui.simpleclouds.noise_settings.param.range", parameter.getMinInclusive(), parameter.getMaxInclusive()).withStyle(ChatFormatting.GRAY));
			this.box = box;
			this.widgets.add(box);
		}
		
		@Override
		public List<? extends GuiEventListener> children()
		{
			return this.widgets;
		}

		@Override
		public List<? extends NarratableEntry> narratables()
		{
			return this.widgets;
		}

		@Override
		public void render(GuiGraphics stack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean selected, float partialTicks)
		{
			this.prevLeft = left;
			this.prevTop = top;
			stack.renderOutline(left, top, width - 5, height, 0xAAFFFFFF);
			if (RenderUtil.isMouseInBounds(mouseX, mouseY, left, top, 20 + TEXT_WIDTH, height))
				stack.renderComponentTooltip(this.font, this.tooltip, mouseX, mouseY);
			stack.drawString(this.font, this.name, left + 5, top + height / 2 - this.font.lineHeight / 2, 0xFFFFFFFF);
			this.box.setY(top + height / 2 - this.box.getHeight() / 2);
			this.box.setX(left + 20 + TEXT_WIDTH);
			this.box.setWidth(width - 30 - TEXT_WIDTH);
			this.box.render(stack, mouseX, mouseY, partialTicks);
		}
		
		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int clickType)
		{
			if (super.mouseClicked(mouseX, mouseY, clickType))
				return true;
			if (clickType == 0)
			{
				LayerEditor.this.setSelected(this);
				return true;
			}
			else
			{
				return false;
			}
		}
		
		@Override
		public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
		{
			if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY))
				return true;
			if (button != 0)
				return false;
			if (RenderUtil.isMouseInBounds((int)mouseX, (int)mouseY, this.prevLeft, this.prevTop, 20 + TEXT_WIDTH, ROW_HEIGHT))
			{
				float value = LayerEditor.this.settings.getParam(this.parameter);
				value = (float)((double)value + dragX);
				LayerEditor.this.settings.setParam(this.parameter, value);
				this.box.setValue(String.valueOf(LayerEditor.this.settings.getParam(this.parameter)));
				LayerEditor.this.onChanged.run();
				return true;
			}
			return false;
		}
	}
}
