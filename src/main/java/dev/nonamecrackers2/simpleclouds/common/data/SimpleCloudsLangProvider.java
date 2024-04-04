package dev.nonamecrackers2.simpleclouds.common.data;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.config.SimpleCloudsConfig;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
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
	}
}
