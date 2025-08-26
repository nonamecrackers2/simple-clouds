package dev.nonamecrackers2.simpleclouds.common.command;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;


import dev.nonamecrackers2.simpleclouds.common.api.SimpleCloudsHooks;
import dev.nonamecrackers2.simpleclouds.api.SimpleCloudsAPI;
import dev.nonamecrackers2.simpleclouds.api.common.cloud.spawning.SpawnInfo;
import dev.nonamecrackers2.simpleclouds.api.common.cloud.spawning.StaticSpawnInfo;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeSource;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudGenerator;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudSpawningConfig;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import dev.nonamecrackers2.simpleclouds.common.world.ServerCloudManager;
import dev.nonamecrackers2.simpleclouds.common.world.SpawnRegion;
import dev.nonamecrackers2.simpleclouds.common.world.SyncType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

public interface CloudCommandSource<S extends Level, T extends CloudManager<S>>
{
	CloudCommandSource<ServerLevel, ServerCloudManager> SERVER = new CloudCommandSource<>()
	{
		@Override
		public Player getPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
		{
			return context.getSource().getPlayerOrException();
		}
		
		@Override
		public ServerCloudManager getCloudManager(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
		{
			return (ServerCloudManager)CloudManager.get(context.getSource().getLevel());
		}
		
		public void onValueUpdated(ServerCloudManager cloudManager, SyncType sync)
		{
			cloudManager.queueSync(sync);
		}
	};
	
	Predicate<CloudRegion> ALL = r -> true;
	
	Function<CloudSpawningConfig.Info, SpawnInfo> EXTREME_CLOUD_INFO = info -> 
	{
		return new StaticSpawnInfo(
				info.cloudType(),
				info.speed().getMaxValue(),
				info.radius().getMaxValue(),
				info.existTicks().getMaxValue(),
				info.growTicks().getMaxValue(),
				info.stretchFactor().getMinValue(), //Not a bug. Smaller values makes the stretch bigger
				info.movesToPlayer(),
				info.orderWeight()
		);
	};
	Function<CloudSpawningConfig.Info, SpawnInfo> TEMPERATE_CLOUD_INFO = info -> 
	{
		return new StaticSpawnInfo(
				info.cloudType(),
				info.speed().getMinValue(),
				info.radius().getMinValue(),
				info.existTicks().getMinValue(),
				info.growTicks().getMinValue(),
				info.stretchFactor().getMaxValue(), //Not a bug. Larger values makes the stretch smaller
				info.movesToPlayer(),
				info.orderWeight()
		);
	};
	
	static Predicate<CloudRegion> storms(CloudTypeSource source)
	{
		return r -> {
			CloudType type = source.getCloudTypeForId(r.getCloudTypeId());
			if (type != null)
				return type.weatherType().causesDarkening();
			return false;
		};
	}
	
	T getCloudManager(CommandContext<CommandSourceStack> context)  throws CommandSyntaxException;
	
	Player getPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException;
	
	void onValueUpdated(T cloudManager, SyncType sync);
	
	default int getScrollAmount(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		T manager = this.getCloudManager(context);
		source.sendSuccess(() -> Component.translatable("command.simpleclouds.scroll.get", manager.getScrollX(), manager.getScrollY(), manager.getScrollZ()), false);
		return 0;
	}
	
	default int getSpeed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		T manager = this.getCloudManager(context);
		source.sendSuccess(() -> Component.translatable("command.simpleclouds.speed.get", manager.getCloudSpeed()), false);
		return 0;
	}
	
	default int setSpeed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		T manager = this.getCloudManager(context);
		float speed = FloatArgumentType.getFloat(context, "amount");
		manager.setCloudSpeed(speed);
		this.onValueUpdated(manager, SyncType.MOVEMENT);
		return 0;
	}
	
	default int getSeed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		T manager = this.getCloudManager(context);
		source.sendSuccess(() -> Component.translatable("command.simpleclouds.seed.get", ComponentUtils.copyOnClickText(String.valueOf(manager.getSeed()))), true);
		return 0;
	} 
	
	default int getCloudHeight(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		int height = this.getCloudManager(context).getCloudHeight();
		source.sendSuccess(() -> Component.translatable("command.simpleclouds.height.get", height), false);
		return height;
	}
	
	default int setCloudHeight(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		int height = IntegerArgumentType.getInteger(context, "height");
		T manager = this.getCloudManager(context);
		manager.setCloudHeight(height);
		this.onValueUpdated(manager, SyncType.MOVEMENT);
		source.sendSuccess(() -> Component.translatable("command.simpleclouds.height.set", height), true);
		return height;
	}
	
	default int spawnCloud(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		T manager = this.getCloudManager(context);
		CloudGenerator generator = manager.getCloudGenerator();
		ResourceLocation id = ResourceLocationArgument.getId(context, "type");
		Vec2 pos = Vec2Argument.getVec2(context, "position");
		float radius = FloatArgumentType.getFloat(context, "radius") / SimpleCloudsConstants.CLOUD_SCALE;
		float stretchFactor = FloatArgumentType.getFloat(context, "stretchFactor");
		float rotation = (float)Math.PI / 180.0F * (FloatArgumentType.getFloat(context, "rotation") % 360.0F);
		int lifeTime = IntegerArgumentType.getInteger(context, "lifeTime");
		int growTime = IntegerArgumentType.getInteger(context, "growTime");
		Vec2 direction = Vec2Argument.getVec2(context, "direction");
		float maxSpeed = FloatArgumentType.getFloat(context, "maxSpeed");
		float accelerationFactor = FloatArgumentType.getFloat(context, "accelerationFactor");
		if (generator.addCloud(new CloudRegion(id, direction, maxSpeed, accelerationFactor, pos.x / SimpleCloudsConstants.CLOUD_SCALE, pos.y / SimpleCloudsConstants.CLOUD_SCALE, radius, rotation, stretchFactor, lifeTime, growTime, Integer.MAX_VALUE), CloudGenerator.Order.TOP))
		{
			source.sendSuccess(() -> Component.translatable("command.simpleclouds.clouds.spawn", id, pos.x, pos.y), true);
			return 1;
		}
		else
		{
			source.sendFailure(Component.translatable("command.simpleclouds.clouds.spawn.fail"));
			return 0;
		}
	}
	
	default int spawnRandomCloud(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		T manager = this.getCloudManager(context);
		CloudGenerator generator = manager.getCloudGenerator();
		CloudRegion region = generator.spawnCloud(generator.getSpawnConfig().get(), source.getUnsidedLevel()).orElse(null);
		if (region != null)
		{
			source.sendSuccess(() -> Component.translatable("command.simpleclouds.clouds.spawn", region.getCloudTypeId(), region.getWorldX(), region.getWorldZ()), true);
			return 1;
		}
		else
		{
			source.sendFailure(Component.translatable("command.simpleclouds.clouds.spawn.fail"));
			return 0;
		}
	}
	
	default int spawnModifiedCloud(CommandContext<CommandSourceStack> context, Function<CloudSpawningConfig.Info, SpawnInfo> func) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		T manager = this.getCloudManager(context);
		CloudGenerator generator = manager.getCloudGenerator();
		
		ResourceLocation id = ResourceLocationArgument.getId(context, "type");
		CloudSpawningConfig config = generator.getSpawnConfig().get();
		CloudSpawningConfig.Info info = config.getWeightInfo(id);
		
		if (info == null)
		{
			source.sendFailure(Component.translatable("commands.simpleclouds.cloudType.notFound", id.toString()));
			return 0;
		}
		
		RandomSource random = RandomSource.create();
		CloudRegion region = generator.spawnCloud(() -> func.apply(info), config.getSpawnInterval().sample(random), config.getMaxRegions(), source.getUnsidedLevel()).orElse(null);
		
		if (region != null)
		{
			source.sendSuccess(() -> Component.translatable("command.simpleclouds.clouds.spawn", region.getCloudTypeId(), region.getWorldX(), region.getWorldZ()), true);
			return 2;
		}
		else
		{
			source.sendFailure(Component.translatable("command.simpleclouds.clouds.spawn.fail"));
			return 1;
		}
	}
	
	//TODO: Explain results
	default int getCloudTypeAt(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		T manager = this.getCloudManager(context);
		CloudGenerator generator = manager.getCloudGenerator();
		Vec2 pos = Vec2Argument.getVec2(context, "position");
		
		CloudRegion region = generator.getCloudAtWorldPosition(pos.x, pos.y);
		if (region == null)
		{
			source.sendSuccess(() -> Component.translatable("command.simpleclouds.clouds.get.empty"), false);
			return 0;
		}
		
		CloudType type = manager.getCloudTypeForId(region.getCloudTypeId());
		if (type == null)
		{
			source.sendFailure(Component.translatable("commands.simpleclouds.cloudType.notFound", region.getCloudTypeId().toString()));
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("command.simpleclouds.clouds.get", type.id().toString(), region.getWorldX(), region.getWorldZ(), type.weatherType().getSerializedName()), false);
		
		return type.weatherType().ordinal() + 1;
	}
	
	default int getCloudTypeCount(CommandContext<CommandSourceStack> context, boolean inRegion, boolean withRadius) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		T manager = this.getCloudManager(context);
		CloudGenerator generator = manager.getCloudGenerator();
		
		List<CloudRegion> regions = Lists.newArrayList();
		
		if (inRegion)
		{
			Vec2 pos = Vec2Argument.getVec2(context, "position");
			int radius = SimpleCloudsConstants.SPAWN_RADIUS;
			if (withRadius)
				radius = IntegerArgumentType.getInteger(context, "radius");
			SpawnRegion region = new SpawnRegion(Mth.floor(pos.y) / SimpleCloudsConstants.CLOUD_SCALE, Mth.floor(pos.y) / SimpleCloudsConstants.CLOUD_SCALE, radius);
			regions.addAll(generator.getCloudsInRegion(region));
		}
		else
		{
			regions.addAll(generator.getClouds());
		}
		
		int size = regions.size();
		String types = regions.stream().map(t -> t.getCloudTypeId().toString()).distinct().collect(Collectors.joining(", "));
		source.sendSuccess(() -> Component.translatable("command.simpleclouds.clouds.count", size, types), false);
		return size;
	}
	
	default int clearClouds(CommandContext<CommandSourceStack> context, Predicate<CloudRegion> region) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		T manager = this.getCloudManager(context);
		CloudGenerator generator = manager.getCloudGenerator();
		int amount = generator.removeCloudsCount(region);
		if (amount > 0)
			source.sendSuccess(() -> Component.translatable("command.simpleclouds.clouds.clear", amount), true);
		else
			source.sendFailure(Component.translatable("command.simpleclouds.clouds.clear.fail"));
		return amount;
	}
	
	default int refreshClouds(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		if (SimpleCloudsAPI.getApi().getHooks().isExternalWeatherControlEnabled())
			return 0;
		CommandSourceStack source = context.getSource();
		T manager = this.getCloudManager(context);
		CloudGenerator generator = manager.getCloudGenerator();
		generator.removeAllClouds();
		for (SpawnRegion region : generator.getSpawnRegions())
			generator.doInitialGen(region.x(), region.z(), source.getUnsidedLevel(), true);
		source.sendSuccess(() -> Component.translatable("command.simpleclouds.clouds.refresh"), true);
		return 1;
	}
}
