package dev.nonamecrackers2.simpleclouds.client.gui.widget;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dev.nonamecrackers2.simpleclouds.common.noise.AbstractNoiseSettings;
import dev.nonamecrackers2.simpleclouds.common.noise.ModifiableLayeredNoise;
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

public class ParametersList extends ContainerObjectSelectionList<ParametersList.Entry>
{
	private static final int ROW_HEIGHT = 30;
	private final ModifiableLayeredNoise settings;
	private final Runnable onEntrySelected;
	
	public ParametersList(ModifiableLayeredNoise settings, Minecraft mc, int x, int y, int width, int height, Runnable onEntrySelected)
	{
		super(mc, width, height, y, y + height, ROW_HEIGHT * AbstractNoiseSettings.Param.values().length + 4);
		this.setLeftPos(x);
		this.setRenderBackground(false);
		this.setRenderTopAndBottom(false);
		this.settings = settings;
		this.buildEntries();
		this.onEntrySelected = onEntrySelected;
	}
	
	public void buildEntries()
	{
		this.clearEntries();
		for (ModifiableNoiseSettings layer : this.settings.getNoiseLayers())
			this.addEntry(new ParametersList.Entry(layer));
	}
	
	@Override
	protected void renderBackground(GuiGraphics graphics)
	{
		graphics.fill(this.x0, this.y0, this.x1, this.y1, 0x99000000);
	}
	
	@Override
	public int getRowWidth()
	{
		return this.getWidth() - 5;
	}
	
	@Override
	protected int getScrollbarPosition()
	{
		return this.getLeft() + this.getWidth() - 5;
	}
	
	@Override
	protected boolean isSelectedItem(int index)
	{
		return Objects.equals(this.getSelected(), this.children().get(index));
	}
	
	public class Entry extends ContainerObjectSelectionList.Entry<ParametersList.Entry>
	{
		private static final int TEXT_WIDTH = 40;
		private final Map<AbstractNoiseSettings.Param, ParametersList.ParameterInfo> parameters = Maps.newLinkedHashMap();
		private final List<AbstractWidget> widgets = Lists.newArrayList();
		private final ModifiableNoiseSettings layer;
		private final Font font;
		private int prevLeft;
		private int prevTop;
		
		public Entry(ModifiableNoiseSettings settings)
		{
			this.font = ParametersList.this.minecraft.font;
			this.layer = settings;
			for (AbstractNoiseSettings.Param parameter : AbstractNoiseSettings.Param.values())
			{
				var fullName = Component.translatable("gui.simpleclouds.noise_settings.param." + parameter.toString().toLowerCase() + ".name"
						);
				var squishedName = ConfigListItem.shortenText(fullName, TEXT_WIDTH);
				
				var box = new EditBox(this.font, 0, 0, 60, 20, CommonComponents.EMPTY);
				box.setValue(String.valueOf(settings.getParam(parameter)));
				box.setResponder(v -> 
				{
					try 
					{
						float value = Float.parseFloat(v);
						if (value < parameter.getMinInclusive() || parameter.getMaxInclusive() < value)
							box.setTextColor(ChatFormatting.RED.getColor());
						else
							box.setTextColor(0xFFFFFFFF);
						settings.setParam(parameter, value);
					} 
					catch (NumberFormatException e)
					{
						box.setTextColor(ChatFormatting.RED.getColor());
						return;
					}
				});
				
				this.widgets.add(box);
				this.parameters.put(parameter, new ParametersList.ParameterInfo(squishedName, Lists.newArrayList(fullName, Component.translatable("gui.simpleclouds.noise_settings.param.range", parameter.getMinInclusive(), parameter.getMaxInclusive()).withStyle(ChatFormatting.GRAY)), box));
			}
		}
		
		public ModifiableNoiseSettings getLayer()
		{
			return this.layer;
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
			int paramIndex = 0;
			for (var entry : this.parameters.entrySet())
			{
				int yOffset = paramIndex * ROW_HEIGHT;
				ParametersList.ParameterInfo info = entry.getValue();
				if (RenderUtil.isMouseInBounds(mouseX, mouseY, left, top + yOffset, 20 + TEXT_WIDTH, ROW_HEIGHT))
					stack.renderComponentTooltip(this.font, info.tooltip, mouseX, mouseY);
				stack.drawString(this.font, info.name, left + 5, top + yOffset + ROW_HEIGHT / 2 - this.font.lineHeight / 2, 0xFFFFFFFF);
				info.box.setY(top + yOffset + ROW_HEIGHT / 2 - info.box.getHeight() / 2);
				info.box.setX(left + 20 + TEXT_WIDTH);
				info.box.setWidth(width - 30 - TEXT_WIDTH);
				info.box.render(stack, mouseX, mouseY, partialTicks);
				paramIndex++;
			}
		}
		
		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int clickType)
		{
			if (super.mouseClicked(mouseX, mouseY, clickType))
				return true;
			if (clickType == 0)
			{
				ParametersList.this.setSelected(this);
				ParametersList.this.onEntrySelected.run();
				return true;
			}
			else
			{
				return false;
			}
		}
		
		public @Nullable AbstractNoiseSettings.Param parameterMouseOver(int mouseX, int mouseY)
		{
			int paramIndex = 0;
			for (AbstractNoiseSettings.Param parameter : this.parameters.keySet())
			{
				int yOffset = paramIndex * ROW_HEIGHT;
				if (RenderUtil.isMouseInBounds(mouseX, mouseY, this.prevLeft, this.prevTop + yOffset, 20 + TEXT_WIDTH, ROW_HEIGHT))
					return parameter;
				paramIndex++;
			}
			return null;
		}
		
		@Override
		public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
		{
			if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY))
				return true;
			if (button != 0)
				return false;
			AbstractNoiseSettings.Param param = this.parameterMouseOver((int)mouseX, (int)mouseY);
			if (param != null)
			{
				float value = this.layer.getParam(param);
				value = (float)((double)value + dragX);
				this.layer.setParam(param, value);
				this.parameters.get(param).box.setValue(String.valueOf(this.layer.getParam(param)));
				return true;
			}
			return false;
		}
	}
	
	private static record ParameterInfo(Component name, List<Component> tooltip, EditBox box) {}
}
