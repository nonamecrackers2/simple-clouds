package dev.nonamecrackers2.simpleclouds.client.gui.widget;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

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

public class ParametersList extends ContainerObjectSelectionList<ParametersList.Entry>
{
	private static final int ROW_HEIGHT = 30;
	
	public ParametersList(ModifiableNoiseSettings settings, Minecraft mc, int x, int y, int width, int height)
	{
		super(mc, width, height, y, y + height, ROW_HEIGHT);
		this.setLeftPos(x);
		this.setRenderBackground(false);
		this.setRenderTopAndBottom(false);
		for (AbstractNoiseSettings.Param param : AbstractNoiseSettings.Param.values())
			this.addEntry(new ParametersList.Entry(param, settings));
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

	public class Entry extends ContainerObjectSelectionList.Entry<ParametersList.Entry>
	{
		private static final int TEXT_WIDTH = 40;
		private final List<AbstractWidget> children = Lists.newArrayList();
		private final AbstractNoiseSettings.Param parameter;
		private final Component fullName;
		private final Component squishedName;
		private final Font font;
		private final EditBox box;
		
		public Entry(AbstractNoiseSettings.Param parameter, ModifiableNoiseSettings settings)
		{
			this.parameter = parameter;
			this.fullName = Component.translatable("gui.simpleclouds.noise_settings.param." + parameter.toString().toLowerCase() + ".name");
			this.squishedName = ConfigListItem.shortenText(this.fullName, TEXT_WIDTH);
			this.font = ParametersList.this.minecraft.font;
			this.box = new EditBox(this.font, 0, 0, 60, 20, CommonComponents.EMPTY);
			float[] value = settings.getParam(this.parameter);
			String[] valuesAsString = new String[value.length];
			for (int i = 0; i < value.length; i++)
				valuesAsString[i] = String.valueOf(value[i]);
			this.box.setValue(StringUtils.join(valuesAsString, ", "));
			this.box.setResponder(v -> 
			{
				String[] split = v.split(", ");
				if (split.length == this.parameter.getSize())
				{
					float[] values = new float[split.length];
					for (int i = 0; i < split.length; i++)
					{
						String s = split[i];
						try 
						{
							float f = Float.parseFloat(s);
							values[i] = f;
							this.box.setTextColor(0xFFFFFFFF);
						} 
						catch (NumberFormatException e)
						{
							this.box.setTextColor(ChatFormatting.RED.getColor());
							return;
						}
					}
					settings.setParam(this.parameter, values);
				}
				else
				{
					this.box.setTextColor(ChatFormatting.RED.getColor());
				}
			});
			this.children.add(this.box);
		}
		
		@Override
		public List<? extends GuiEventListener> children()
		{
			return this.children;
		}

		@Override
		public List<? extends NarratableEntry> narratables()
		{
			return this.children;
		}

		@Override
		public void render(GuiGraphics stack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean selected, float partialTicks)
		{
			if (RenderUtil.isMouseInBounds(mouseX, mouseY, left, top, 20 + TEXT_WIDTH, height))
				stack.renderTooltip(this.font, this.fullName, mouseX, mouseY);
			stack.renderOutline(left, top, width - 5, height, 0xAAFFFFFF);
			stack.drawString(this.font, this.squishedName, left + 5, top + height / 2 - this.font.lineHeight / 2, 0xFFFFFFFF);
			this.box.setY(top + height / 2 - this.box.getHeight() / 2);
			this.box.setX(left + 20 + TEXT_WIDTH);
			this.box.setWidth(width - 30 - TEXT_WIDTH);
			this.box.render(stack, mouseX, mouseY, partialTicks);
		}
	}
}
