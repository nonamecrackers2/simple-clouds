package dev.nonamecrackers2.simpleclouds.common.init;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.command.argument.CloudTypeArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SimpleCloudsCommandArguments
{
	private static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, SimpleCloudsMod.MODID);
	
	public static final DeferredHolder<ArgumentTypeInfo<?, ?>, CloudTypeArgument.Info> CLOUD_TYPE = ARGUMENT_TYPES.register("cloud_type", () -> ArgumentTypeInfos.registerByClass(CloudTypeArgument.class, new CloudTypeArgument.Info()));
	
	public static void register(IEventBus modBus)
	{
		ARGUMENT_TYPES.register(modBus);
	}
}
