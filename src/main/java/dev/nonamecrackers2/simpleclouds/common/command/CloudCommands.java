package dev.nonamecrackers2.simpleclouds.common.command;

import java.util.function.Predicate;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import dev.nonamecrackers2.simpleclouds.SimpleCloudsMod;
import dev.nonamecrackers2.simpleclouds.common.world.CloudManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;

public class CloudCommands
{
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, String baseName, Predicate<CommandSourceStack> requirement, CloudCommandSource source)
	{
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(SimpleCloudsMod.MODID).requires(requirement);
		
		root.then(Commands.literal(baseName)
				.then(Commands.literal("scroll")
						.then(Commands.literal("get")
								.executes(source::getScrollAmount)
						)
						.then(Commands.literal("set")
								.then(Commands.argument("amount", Vec3Argument.vec3(false))
										.executes(source::setScrollAmount)
								)
						)
				)
		);
		
		root.then(Commands.literal(baseName)
				.then(Commands.literal("speed")
						.then(Commands.literal("get")
								.executes(source::getSpeed)
						)
						.then(Commands.literal("set")
								.then(Commands.argument("amount", FloatArgumentType.floatArg(0.0F))
										.executes(source::setSpeed)
								)
						)
				)
		);
		
		root.then(Commands.literal(baseName)
				.then(Commands.literal("seed")
						.then(Commands.literal("get")
								.executes(source::getSeed)
						)
				)
		);
		
		root.then(Commands.literal(baseName)
				.then(Commands.literal("reset")
						.then(Commands.literal("random")
								.executes(source::reinitializeWithRandomSeed)
						)
						.then(Commands.argument("seed", LongArgumentType.longArg(0L))
								.executes(source::reinitializeWithSpecifiedSeed)
						)
						.executes(source::reinitializeWithSameSeed)
				)
		);
		
		root.then(Commands.literal(baseName)
				.then(Commands.literal("direction")
						.then(Commands.literal("get")
								.executes(source::getDirection)
						)
						.then(Commands.literal("set")
								.then(Commands.literal("facingMyDirection")
										.executes(source::setDirectionWithPlayerFacing)
								)
								.then(Commands.argument("direction", Vec3Argument.vec3(false))
										.executes(source::setDirectionSpecified)
								)
						)
				)
		);
		
		root.then(Commands.literal(baseName)
				.then(Commands.literal("height")
						.then(Commands.literal("get")
								.executes(source::getCloudHeight)
						)
						.then(Commands.literal("set")
								.then(Commands.argument("height", IntegerArgumentType.integer(CloudManager.CLOUD_HEIGHT_MIN, CloudManager.CLOUD_HEIGHT_MAX))
										.executes(source::setCloudHeight)
								)
						)
				)
		);
		
		dispatcher.register(root);
	}
}
