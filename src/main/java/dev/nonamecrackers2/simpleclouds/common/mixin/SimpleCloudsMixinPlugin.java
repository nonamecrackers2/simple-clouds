package dev.nonamecrackers2.simpleclouds.common.mixin;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.neoforged.fml.loading.LoadingModList;

public class SimpleCloudsMixinPlugin implements IMixinConfigPlugin
{
	private static final Pattern CATEGORY_MATCHER = Pattern.compile("(?<=mixin\\.).*(?=\\.)");
	
	@Override
	public void onLoad(String mixinPackage)
	{
	}

	@Override
	public String getRefMapperConfig()
	{
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
	{
		Matcher matcher = CATEGORY_MATCHER.matcher(mixinClassName);
		if (!matcher.find())
			return true;
		String category = matcher.group(0);
		switch (category)
		{
		case "vivecraft":
			return LoadingModList.get().getModFileById("vivecraft") != null;
		case "skyaesthetics":
			return LoadingModList.get().getModFileById("sky_aesthetics") != null;
		default:
			return true;
		}
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

	@Override
	public List<String> getMixins()
	{
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
