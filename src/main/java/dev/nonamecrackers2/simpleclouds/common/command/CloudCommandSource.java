package dev.nonamecrackers2.simpleclouds.common.command;

import java.util.function.Function;
import java.util.function.Predicate;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.nonamecrackers2.simpleclouds.common.cloud.CloudType;
import dev.nonamecrackers2.simpleclouds.common.cloud.CloudTypeSource;
import dev.nonamecrackers2.simpleclouds.common.cloud.SimpleCloudsConstants;
import dev.nonamecrackers2.simpleclouds.common.cloud.region.CloudRegion;
import dev.nonamecrackers2.simpleclouds.common.cloud.spawning.CloudGenerator;
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
	
	default int reinitialize(CommandContext<CommandSourceStack> context, Function<T, Long> seedGetter) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		T manager = this.getCloudManager(context);
		manager.init(seedGetter.apply(manager));
		this.onValueUpdated(manager, SyncType.BASE_PROPERTIES);
		source.sendSuccess(() -> Component.translatable("command.simpleclouds.reinitialize"), true);
		return 0;
	}
	
	default int reinitializeWithSameSeed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		return this.reinitialize(context, CloudManager::getSeed);
	}
	
	default int reinitializeWithSpecifiedSeed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		return this.reinitialize(context, m -> LongArgumentType.getLong(context, "seed"));
	}
	
	default int reinitializeWithRandomSeed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		return this.reinitialize(context, m -> context.getSource().getUnsidedLevel().getRandom().nextLong());
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
	
	default int clearClouds(CommandContext<CommandSourceStack> context, Predicate<CloudRegion> region) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		T manager = this.getCloudManager(context);
		CloudGenerator generator = manager.getCloudGenerator();
		int amount = generator.getClouds().size();
		if (generator.removeClouds(region))
			source.sendSuccess(() -> Component.translatable("command.simpleclouds.clouds.clear", amount), true);
		else
			source.sendFailure(Component.translatable("command.simpleclouds.clouds.clear.fail"));
		return amount;
	}
	
	default int refreshClouds(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
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
