package dev.nonamecrackers2.simpleclouds.common.init;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.command.argument.CloudTypeArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SimpleCloudsCommandArguments
{
	private static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES = DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, SimpleCloudsMod.MODID);
	
	public static final RegistryObject<CloudTypeArgument.Info> CLOUD_TYPE = ARGUMENT_TYPES.register("cloud_type", () -> ArgumentTypeInfos.registerByClass(CloudTypeArgument.class, new CloudTypeArgument.Info()));
	
	public static void register(IEventBus modBus)
	{
		ARGUMENT_TYPES.register(modBus);
	}
}
