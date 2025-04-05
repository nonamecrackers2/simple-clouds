package dev.nonamecrackers2.simpleclouds.common.command.argument;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CloudTypeArgument extends ResourceLocationArgument
{
	public static final DynamicCommandExceptionType ERROR_UNKNOWN_TYPE = new DynamicCommandExceptionType(o -> {
		return Component.translatable("commands.simpleclouds.cloudType.notFound", o);
	});
	
	private @Nullable CloudTypeSource source;
	private @Nullable List<ResourceLocation> cloudTypes;
	
	private CloudTypeArgument(CloudTypeSource source)
	{
		this.source = source;
	}
	
	public static CloudTypeArgument type(CloudTypeSource source)
	{
		return new CloudTypeArgument(source);
	}
	
	@Override
	public ResourceLocation parse(StringReader reader) throws CommandSyntaxException
	{
		ResourceLocation loc = super.parse(reader);
		if (this.source != null ? !this.source.doesCloudTypeExist(loc) : !this.cloudTypes.contains(loc))
			throw ERROR_UNKNOWN_TYPE.create(loc);
		return loc;
	}
	
	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder)
	{
		List<String> vals;
		if (this.source == null)
			vals = this.cloudTypes.stream().map(ResourceLocation::toString).collect(Collectors.toList());
		else
			vals = Arrays.stream(this.source.getIndexedCloudTypes()).map(CloudType::id).map(ResourceLocation::toString).collect(Collectors.toList());
		return SharedSuggestionProvider.suggest(vals, builder);
	}
	
	public static class Info implements ArgumentTypeInfo<CloudTypeArgument, CloudTypeArgument.Info.Template>
	{
		@Override
		public void serializeToNetwork(CloudTypeArgument.Info.Template template, FriendlyByteBuf buffer)
		{
			buffer.writeCollection(template.types, FriendlyByteBuf::writeResourceLocation);
		}
		
		@Override
		public CloudTypeArgument.Info.Template deserializeFromNetwork(FriendlyByteBuf buffer)
		{
			return new CloudTypeArgument.Info.Template(buffer.readList(FriendlyByteBuf::readResourceLocation));
		}
		
		@Override
		public void serializeToJson(CloudTypeArgument.Info.Template template, JsonObject json)
		{
			JsonArray array = new JsonArray();
			template.types.forEach(t -> array.add(t.toString()));
			json.add("types", array);
		}
		
		@Override
		public CloudTypeArgument.Info.Template unpack(CloudTypeArgument argument)
		{
			return new CloudTypeArgument.Info.Template(Arrays.stream(argument.source.getIndexedCloudTypes()).map(CloudType::id).collect(Collectors.toList()));
		}
		
		public final class Template implements ArgumentTypeInfo.Template<CloudTypeArgument>
		{
			private final List<ResourceLocation> types;
			
			public Template(List<ResourceLocation> types)
			{
				this.types = types;
			}
			
			@Override
			public CloudTypeArgument instantiate(CommandBuildContext context)
			{
				var arg = new CloudTypeArgument(null);
				arg.cloudTypes = this.types;
				return arg;
			}
			
			@Override
			public ArgumentTypeInfo<CloudTypeArgument, ?> type()
			{
				return Info.this;
			}
		}
	}
}
