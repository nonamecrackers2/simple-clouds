package dev.nonamecrackers2.simpleclouds.common.data;

import org.apache.commons.lang3.StringUtils;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import dev.nonamecrackers2.simpleclouds.common.noise.AbstractNoiseSettings;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.fml.ModList;
import nonamecrackers2.crackerslib.common.util.data.ConfigLangGeneratorHelper;

public class SimpleCloudsLangProvider extends LanguageProvider
{
	public SimpleCloudsLangProvider(PackOutput output)
	{
		super(output, SimpleCloudsMod.MODID, "en_us");
	}
	
	@Override
	protected void addTranslations()
	{
		ConfigLangGeneratorHelper.langForSpec(SimpleCloudsMod.MODID, SimpleCloudsConfig.CLIENT_SPEC, this, ConfigLangGeneratorHelper.Info.ONLY_RANGE);
		this.add("gui.simpleclouds.cloud_previewer.title", "Cloud Previewer");
		this.add("gui.simpleclouds.cloud_previewer.button.title", "Cloud Previewer");
		this.add("gui.simpleclouds.cloud_previewer.button.add_layer.title", "Add Layer");
		this.add("gui.simpleclouds.cloud_previewer.button.remove_layer.title", "Remove Layer");
		this.add("gui.simpleclouds.cloud_previewer.button.toggle_preview.title", "Toggle Preview");
		this.add("gui.simpleclouds.cloud_previewer.layers", "Layers");
		this.add("simpleclouds.key.openGenPreviewer", "Open Cloud Gen Previewer");
		this.add("simpleclouds.key.categories.main", ModList.get().getModContainerById(SimpleCloudsMod.MODID).get().getModInfo().getDisplayName());
		for (AbstractNoiseSettings.Param parameter : AbstractNoiseSettings.Param.values())
		{
			String key = "gui.simpleclouds.noise_settings.param." + parameter.toString().toLowerCase() + ".name";
			String[] splitted = parameter.toString().toLowerCase().split("_");
			for (int i = 0; i < splitted.length; i++)
				splitted[i] = StringUtils.capitalize(splitted[i]);
			this.add(key, StringUtils.join(splitted, " "));
		}
		this.add("gui.simpleclouds.noise_settings.param.range", "Range: %s - %s");
	}
}
