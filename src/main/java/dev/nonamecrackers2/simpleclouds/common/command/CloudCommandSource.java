package dev.nonamecrackers2.simpleclouds.common.command;

import java.util.function.Function;

import org.joml.Vector3f;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import dev.nonamecrackers2.simpleclouds.common.world.ServerCloudManager;
import dev.nonamecrackers2.simpleclouds.common.world.SyncType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface CloudCommandSource<S extends Level, T extends CloudManager<S>>
{
	public static final CloudCommandSource<ServerLevel, ServerCloudManager> SERVER = new CloudCommandSource<>()
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
			cloudManager.setRequiresSync(sync);
		}
	};
	
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
	
	default int setScrollAmount(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		T manager = this.getCloudManager(context);
		Vec3 scroll = Vec3Argument.getVec3(context, "amount");
		manager.setScrollX((float)scroll.x);
		manager.setScrollY((float)scroll.y);
		manager.setScrollZ((float)scroll.z);
		this.onValueUpdated(manager, SyncType.MOVEMENT);
		return 0;
	}
	
	default int getSpeed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		T manager = this.getCloudManager(context);
		source.sendSuccess(() -> Component.translatable("command.simpleclouds.speed.get", manager.getSpeed()), false);
		return 0;
	}
	
	default int setSpeed(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		T manager = this.getCloudManager(context);
		float speed = FloatArgumentType.getFloat(context, "amount");
		manager.setSpeed(speed);
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
	
	default int getDirection(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		T manager = this.getCloudManager(context);
		Vector3f dir = manager.getDirection();
		float dx = (float)Math.round(dir.x * 100.0F) / 100.0F;
		float dy = (float)Math.round(dir.y * 100.0F) / 100.0F;
		float dz = (float)Math.round(dir.z * 100.0F) / 100.0F;
		source.sendSuccess(() -> Component.translatable("command.simpleclouds.direction.get", dx, dy, dz, Direction.getNearest(dx, dy, dz)), false);
		return 0;
	}
	
	default int setDirection(CommandContext<CommandSourceStack> context, Vector3f dir) throws CommandSyntaxException
	{
		CommandSourceStack source = context.getSource();
		T manager = this.getCloudManager(context);
		manager.setDirection(dir);
		this.onValueUpdated(manager, SyncType.MOVEMENT);
		float dx = (float)Math.round(dir.x * 100.0F) / 100.0F;
		float dy = (float)Math.round(dir.y * 100.0F) / 100.0F;
		float dz = (float)Math.round(dir.z * 100.0F) / 100.0F;
		source.sendSuccess(() -> Component.translatable("command.simpleclouds.direction.set", dx, dy, dz, Direction.getNearest(dx, dy, dz)), true);
		return 0;
	}
	
	default int setDirectionWithPlayerFacing(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		return this.setDirection(context, this.getPlayer(context).getLookAngle().toVector3f());
	}
	
	default int setDirectionSpecified(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		return this.setDirection(context, Vec3Argument.getVec3(context, "direction").toVector3f());
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
}
